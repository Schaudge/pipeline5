package com.hartwig.pipeline;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.hartwig.patient.ImmutableSample;
import com.hartwig.patient.Sample;
import com.hartwig.pipeline.alignment.Aligner;
import com.hartwig.pipeline.alignment.AlignerProvider;
import com.hartwig.pipeline.alignment.AlignmentOutput;
import com.hartwig.pipeline.alignment.AlignmentOutputStorage;
import com.hartwig.pipeline.alignment.AlignmentPair;
import com.hartwig.pipeline.alignment.after.metrics.BamMetrics;
import com.hartwig.pipeline.alignment.after.metrics.BamMetricsOutput;
import com.hartwig.pipeline.alignment.after.metrics.BamMetricsOutputStorage;
import com.hartwig.pipeline.alignment.after.metrics.BamMetricsProvider;
import com.hartwig.pipeline.calling.germline.GermlineCaller;
import com.hartwig.pipeline.calling.germline.GermlineCallerOutput;
import com.hartwig.pipeline.calling.germline.GermlineCallerProvider;
import com.hartwig.pipeline.calling.somatic.SomaticCaller;
import com.hartwig.pipeline.calling.somatic.SomaticCallerOutput;
import com.hartwig.pipeline.calling.somatic.SomaticCallerProvider;
import com.hartwig.pipeline.calling.structural.StructuralCaller;
import com.hartwig.pipeline.calling.structural.StructuralCallerOutput;
import com.hartwig.pipeline.calling.structural.StructuralCallerProvider;
import com.hartwig.pipeline.credentials.CredentialProvider;
import com.hartwig.pipeline.io.ResultsDirectory;
import com.hartwig.pipeline.metadata.PatientMetadataApi;
import com.hartwig.pipeline.metadata.PatientMetadataApiProvider;
import com.hartwig.pipeline.report.PatientReport;
import com.hartwig.pipeline.report.PatientReportProvider;
import com.hartwig.pipeline.storage.StorageProvider;
import com.hartwig.pipeline.tertiary.amber.Amber;
import com.hartwig.pipeline.tertiary.amber.AmberOutput;
import com.hartwig.pipeline.tertiary.amber.AmberProvider;
import com.hartwig.pipeline.tertiary.cobalt.Cobalt;
import com.hartwig.pipeline.tertiary.cobalt.CobaltOutput;
import com.hartwig.pipeline.tertiary.cobalt.CobaltProvider;
import com.hartwig.pipeline.tertiary.healthcheck.HealthChecker;
import com.hartwig.pipeline.tertiary.healthcheck.HealthCheckerProvider;
import com.hartwig.pipeline.tertiary.purple.Purple;
import com.hartwig.pipeline.tertiary.purple.PurpleOutput;
import com.hartwig.pipeline.tertiary.purple.PurpleProvider;
import com.hartwig.pipeline.tools.Versions;

import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatientReportPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientReportPipeline.class);

    private final PatientMetadataApi patientMetadataApi;
    private final Aligner aligner;
    private final BamMetrics metrics;
    private final GermlineCaller germlineCaller;
    private final SomaticCaller somaticCaller;
    private final StructuralCaller structuralCaller;
    private final Amber amber;
    private final Cobalt cobalt;
    private final Purple purple;
    private final HealthChecker healthChecker;
    private final AlignmentOutputStorage alignmentOutputStorage;
    private final BamMetricsOutputStorage bamMetricsOutputStorage;
    private final PatientReport report;
    private final ExecutorService executorService;

    PatientReportPipeline(final PatientMetadataApi patientMetadataApi, final Aligner aligner, final BamMetrics metrics,
            final GermlineCaller germlineCaller, final SomaticCaller somaticCaller, final StructuralCaller structuralCaller,
            final Amber amber, final Cobalt cobalt, final Purple purple, final HealthChecker healthChecker,
            final AlignmentOutputStorage alignmentOutputStorage, final BamMetricsOutputStorage bamMetricsOutputStorage,
            final PatientReport report, final ExecutorService executorService) {
        this.patientMetadataApi = patientMetadataApi;
        this.aligner = aligner;
        this.metrics = metrics;
        this.germlineCaller = germlineCaller;
        this.somaticCaller = somaticCaller;
        this.structuralCaller = structuralCaller;
        this.amber = amber;
        this.cobalt = cobalt;
        this.purple = purple;
        this.healthChecker = healthChecker;
        this.alignmentOutputStorage = alignmentOutputStorage;
        this.bamMetricsOutputStorage = bamMetricsOutputStorage;
        this.report = report;
        this.executorService = executorService;
    }

    public PipelineState run() throws Exception {
        Versions.printAll();
        String setName = patientMetadataApi.getMetadata().setName();
        PipelineState state = new PipelineState();
        AlignmentOutput alignmentOutput = report.add(state.add(aligner.run()));
        if (state.shouldProceed()) {
            Future<BamMetricsOutput> bamMetricsFuture = executorService.submit(() -> metrics.run(alignmentOutput));
            Future<GermlineCallerOutput> germlineCallerFuture = executorService.submit(() -> germlineCaller.run(alignmentOutput));

            Optional<AlignmentOutput> maybeMate = alignmentOutputStorage.get(mate(alignmentOutput.sample()));
            if (maybeMate.isPresent()) {

                AlignmentPair pair = AlignmentPair.of(alignmentOutput, maybeMate.get());

                Future<SomaticCallerOutput> somaticCallerFuture = executorService.submit(() -> somaticCaller.run(pair));
                Future<AmberOutput> amberOutputFuture = executorService.submit(() -> amber.run(pair));
                Future<CobaltOutput> cobaltOutputFuture = executorService.submit(() -> cobalt.run(pair));

                BamMetricsOutput metricsOutput = report.add(state.add(futurePayload(bamMetricsFuture)));

                if (state.shouldProceed()) {
                    BamMetricsOutput mateMetricsOutput = bamMetricsOutputStorage.get(mate(alignmentOutput.sample()));
                    Future<StructuralCallerOutput> structuralCallerFuture =
                            executorService.submit(() -> structuralCaller.run(pair, metricsOutput, mateMetricsOutput));

                    SomaticCallerOutput somaticCallerOutput = report.add(state.add(futurePayload(somaticCallerFuture)));
                    StructuralCallerOutput structuralCallerOutput = report.add(state.add(futurePayload(structuralCallerFuture)));
                    AmberOutput amberOutput = report.add(state.add(futurePayload(amberOutputFuture)));
                    CobaltOutput cobaltOutput = report.add(state.add(futurePayload(cobaltOutputFuture)));

                    if (state.shouldProceed()) {
                        PurpleOutput purpleOutput = report.add(state.add(purple.run(pair,
                                somaticCallerOutput,
                                structuralCallerOutput,
                                cobaltOutput,
                                amberOutput)));
                        if (state.shouldProceed()) {
                            report.add(state.add(healthChecker.run(pair,
                                    metricsOutput,
                                    mateMetricsOutput,
                                    somaticCallerOutput,
                                    purpleOutput,
                                    amberOutput,
                                    setName)));
                        }
                    }
                }
            } else {
                report.add(state.add(futurePayload(bamMetricsFuture)));
            }
            report.add(state.add(futurePayload(germlineCallerFuture)));
        }

        report.compose(setName);
        return state;
    }

    private static <T> T futurePayload(final Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try {
            Arguments arguments = CommandLineOptions.from(args);
            LOGGER.info("Arguments [{}]", arguments);
            try {
                GoogleCredentials credentials = CredentialProvider.from(arguments).get();
                Storage storage = StorageProvider.from(arguments, credentials).get();
                PipelineState state = new PatientReportPipeline(PatientMetadataApiProvider.from(arguments).get(),
                        AlignerProvider.from(credentials, storage, arguments).get(),
                        BamMetricsProvider.from(arguments, credentials, storage).get(),
                        GermlineCallerProvider.from(credentials, storage, arguments).get(),
                        SomaticCallerProvider.from(arguments, credentials, storage).get(),
                        StructuralCallerProvider.from(arguments, credentials, storage).get(),
                        AmberProvider.from(arguments, credentials, storage).get(),
                        CobaltProvider.from(arguments, credentials, storage).get(),
                        PurpleProvider.from(arguments, credentials, storage).get(),
                        HealthCheckerProvider.from(arguments, credentials, storage).get(),
                        new AlignmentOutputStorage(storage, arguments, ResultsDirectory.defaultDirectory()),
                        new BamMetricsOutputStorage(storage, arguments, ResultsDirectory.defaultDirectory()),
                        PatientReportProvider.from(storage, arguments).get(),
                        Executors.newCachedThreadPool()).run();
                LOGGER.info("Patient report pipeline is complete with status [{}]. Stages run were [{}]", state.status(), state);
            } catch (Exception e) {
                LOGGER.error("An unexpected issue arose while running the pipeline. See the attached exception for more details.", e);
                System.exit(1);
            }
            System.exit(0);
        } catch (ParseException e) {
            LOGGER.info("Exiting due to incorrect arguments");
        }
    }

    private static Sample mate(Sample sample) {
        if (sample.type().equals(Sample.Type.REFERENCE)) {
            return replaceSuffix(sample, "T").type(Sample.Type.TUMOR).build();
        } else {
            return replaceSuffix(sample, "R").type(Sample.Type.REFERENCE).build();
        }
    }

    @NotNull
    private static ImmutableSample.Builder replaceSuffix(final Sample sample, final String newSuffix) {
        return Sample.builder(sample.directory(), sample.name().substring(0, sample.name().length() - 1).concat(newSuffix));
    }
}
