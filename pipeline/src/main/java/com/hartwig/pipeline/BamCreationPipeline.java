package com.hartwig.pipeline;

import static java.lang.String.format;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.hartwig.io.DataSource;
import com.hartwig.io.InputOutput;
import com.hartwig.io.OutputFile;
import com.hartwig.io.OutputStore;
import com.hartwig.io.OutputType;
import com.hartwig.patient.Patient;
import com.hartwig.patient.Sample;

import org.bdgenomics.adam.rdd.read.AlignmentRecordRDD;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Value.Immutable
public abstract class BamCreationPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(BamCreationPipeline.class);

    public void execute(Patient patient) throws Exception {
        LOGGER.info("Storing results in {}", OutputFile.RESULTS_DIRECTORY);
        ExecutorService executorService = executorService();
        Future<?> awaitReference = executorService.submit(() -> createBAM(patient.reference()));
        if (patient.maybeTumour().isPresent()) {
            Future<?> awaitTumour = executorService.submit(() -> createBAM(patient.tumour()));
            awaitTumour.get();
        }
        awaitReference.get();
    }

    private void createBAM(final Sample sample) {
        LOGGER.info("Preprocessing started for {} sample", sample.name());
        try {
            long startTime = startTimer();
            InputOutput<AlignmentRecordRDD> aligned;
            if (!bamStore().exists(sample, OutputType.ALIGNED)) {
                aligned = runStage(sample, alignment(), bamStore(), InputOutput.seed(sample));
            } else {
                skipping(alignment(), sample);
                aligned = alignmentDatasource().extract(sample);
            }

            QualityControl<AlignmentRecordRDD> readCount = qcFactory().readCount(aligned.payload());

            InputOutput<AlignmentRecordRDD> output = null;
            for (Stage<AlignmentRecordRDD, AlignmentRecordRDD> bamEnricher : bamEnrichment()) {
                if (!bamStore().exists(sample, bamEnricher.outputType())) {
                    InputOutput<AlignmentRecordRDD> input = bamEnricher.datasource().extract(sample);
                    qc(readCount, input);
                    output = runStage(sample, bamEnricher, bamStore(), input);
                } else {
                    skipping(bamEnricher, sample);
                }
            }
            if (output != null) {
                qc(qcFactory().referenceBAMQC(), output);
            }
            LOGGER.info("Preprocessing complete for {} sample, Took {} ms", sample.name(), (endTimer() - startTime));
        } catch (IOException e) {
            LOGGER.error(format("Unable to create BAM for %s. Check exception for details", sample.name()), e);
        }
    }

    private void skipping(final Stage<AlignmentRecordRDD, AlignmentRecordRDD> bamEnricher, final Sample sample) {
        LOGGER.info("Skipping [{}] stage for [{}] as the output already exists in [{}]",
                bamEnricher.outputType(),
                sample.name(),
                OutputFile.RESULTS_DIRECTORY);
    }

    private void qc(final QualityControl<AlignmentRecordRDD> qcCheck, final InputOutput<AlignmentRecordRDD> toQC) throws IOException {
        QCResult check = qcCheck.check(toQC);
        if (!check.isOk()) {
            throw new IllegalStateException(check.message());
        }
    }

    private InputOutput<AlignmentRecordRDD> runStage(final Sample sample, final Stage<AlignmentRecordRDD, AlignmentRecordRDD> stage,
            final OutputStore<AlignmentRecordRDD> store, final InputOutput<AlignmentRecordRDD> input) throws IOException {
        Trace trace =
                Trace.of(BamCreationPipeline.class, format("Executing [%s] stage for [%s]", stage.outputType(), sample.name())).start();
        InputOutput<AlignmentRecordRDD> output = stage.execute(input == null ? InputOutput.seed(sample) : input);
        store.store(output);
        trace.finish();
        return output;
    }

    private static long startTimer() {
        return System.currentTimeMillis();
    }

    private static long endTimer() {
        return System.currentTimeMillis();
    }

    protected abstract DataSource<AlignmentRecordRDD> alignmentDatasource();

    protected abstract AlignmentStage alignment();

    protected abstract QualityControlFactory qcFactory();

    protected abstract List<Stage<AlignmentRecordRDD, AlignmentRecordRDD>> bamEnrichment();

    protected abstract OutputStore<AlignmentRecordRDD> bamStore();

    private ExecutorService executorService() {
        return Executors.newFixedThreadPool(2);
    }

    public static ImmutableBamCreationPipeline.Builder builder() {
        return ImmutableBamCreationPipeline.builder();
    }
}
