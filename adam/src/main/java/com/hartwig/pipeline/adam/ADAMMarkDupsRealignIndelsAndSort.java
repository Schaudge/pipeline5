package com.hartwig.pipeline.adam;

import java.io.IOException;
import java.util.stream.Collector;

import com.hartwig.io.DataSource;
import com.hartwig.io.InputOutput;
import com.hartwig.io.IntermediateDataLocation;
import com.hartwig.io.OutputType;
import com.hartwig.patient.KnownIndels;
import com.hartwig.patient.ReferenceGenome;
import com.hartwig.pipeline.Stage;

import org.apache.spark.rdd.RDD;
import org.bdgenomics.adam.algorithms.consensus.ConsensusGeneratorFromKnowns;
import org.bdgenomics.adam.api.java.JavaADAMContext;
import org.bdgenomics.adam.rdd.read.AlignmentRecordRDD;
import org.bdgenomics.adam.rdd.variant.VariantRDD;
import org.bdgenomics.adam.util.ReferenceFile;
import org.bdgenomics.formats.avro.Variant;

import scala.Option;

public class ADAMMarkDupsRealignIndelsAndSort implements Stage<AlignmentRecordRDD, AlignmentRecordRDD> {

    private final KnownIndels knownIndels;
    private final ReferenceGenome referenceGenome;
    private final JavaADAMContext javaADAMContext;
    private final IntermediateDataLocation dataLocation;

    ADAMMarkDupsRealignIndelsAndSort(final KnownIndels knownIndels, final ReferenceGenome referenceGenome,
            final JavaADAMContext javaADAMContext, final IntermediateDataLocation dataLocation) {
        this.knownIndels = knownIndels;
        this.referenceGenome = referenceGenome;
        this.javaADAMContext = javaADAMContext;
        this.dataLocation = dataLocation;
    }

    @Override
    public DataSource<AlignmentRecordRDD> datasource() {
        return new HDFSAlignmentRDDSource(OutputType.DUPLICATE_MARKED, javaADAMContext, dataLocation);
    }

    @Override
    public OutputType outputType() {
        return OutputType.INDEL_REALIGNED;
    }

    @Override
    public InputOutput<AlignmentRecordRDD> execute(final InputOutput<AlignmentRecordRDD> input) throws IOException {
        RDD<Variant> allKnownVariants = knownIndels.paths()
                .stream()
                .map(javaADAMContext::loadVariants)
                .map(VariantRDD::rdd)
                .collect(Collector.of(() -> javaADAMContext.getSparkContext().<Variant>emptyRDD().rdd(), RDD::union, RDD::union));
        ReferenceFile fasta = javaADAMContext.loadReferenceFile(referenceGenome.path());
        UnmappedReads unmapped = UnmappedReads.from(input.payload());
        return InputOutput.of(OutputType.INDEL_REALIGNED,
                input.sample(),
                RDDs.persistDisk(unmapped.toAlignment(input.payload()
                        .markDuplicates()
                        .realignIndels(new ConsensusGeneratorFromKnowns(allKnownVariants, 0),
                                false,
                                500,
                                30,
                                5.0,
                                3000,
                                20000,
                                Option.apply(fasta), false)).sortReadsByReferencePositionAndIndex()));
    }
}
