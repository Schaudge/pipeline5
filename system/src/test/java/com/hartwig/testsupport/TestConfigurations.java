package com.hartwig.testsupport;

import static com.hartwig.support.test.Resources.testResource;

import com.google.common.collect.ImmutableMap;
import com.hartwig.pipeline.runtime.configuration.Configuration;
import com.hartwig.pipeline.runtime.configuration.ImmutableConfiguration;
import com.hartwig.pipeline.runtime.configuration.ImmutableKnownIndelParameters;
import com.hartwig.pipeline.runtime.configuration.ImmutableKnownSnpParameters;
import com.hartwig.pipeline.runtime.configuration.ImmutablePatientParameters;
import com.hartwig.pipeline.runtime.configuration.ImmutablePipelineParameters;
import com.hartwig.pipeline.runtime.configuration.ImmutableReferenceGenomeParameters;
import com.hartwig.pipeline.runtime.configuration.KnownIndelParameters;
import com.hartwig.pipeline.runtime.configuration.KnownSnpParameters;
import com.hartwig.pipeline.runtime.configuration.ReferenceGenomeParameters;

public class TestConfigurations {
    private static final String PATIENT_DIR = "patients";

    public static final String HUNDREDK_READS_HISEQ_PATIENT_NAME = "TESTX";

    public static final ReferenceGenomeParameters REFERENCE_GENOME_PARAMETERS =
            ImmutableReferenceGenomeParameters.builder().directory(testResource("reference_genome/")).file("reference.fasta").build();

    private static final KnownIndelParameters KNOWN_INDEL_PARAMETERS = ImmutableKnownIndelParameters.builder()
            .directory(testResource("known_indels/"))
            .addFiles("1000G_phase1.indels.b37.vcf.gz")
            .build();
    private static final KnownSnpParameters KNOWN_SNP_PARAMETERS =
            ImmutableKnownSnpParameters.builder().directory(testResource("known_snps/")).addFiles("dbsnp_137.b37.onevariant.vcf").build();

    private static final ImmutablePatientParameters.Builder DEFAULT_PATIENT_BUILDER = ImmutablePatientParameters.builder();

    private static final ImmutableConfiguration.Builder DEFAULT_CONFIG_BUILDER = ImmutableConfiguration.builder()
            .spark(ImmutableMap.of("master", "local[2]"))
            .pipeline(ImmutablePipelineParameters.builder().hdfs("file:///").build())
            .referenceGenome(REFERENCE_GENOME_PARAMETERS)
            .knownIndel(KNOWN_INDEL_PARAMETERS)
            .knownSnp(KNOWN_SNP_PARAMETERS);

    public static final Configuration HUNDREDK_READS_HISEQ =
            DEFAULT_CONFIG_BUILDER.patient(DEFAULT_PATIENT_BUILDER.directory(testResource(PATIENT_DIR + "/100k_reads_hiseq"))
                    .name(HUNDREDK_READS_HISEQ_PATIENT_NAME)
                    .build()).build();
}
