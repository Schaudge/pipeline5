package com.hartwig.bam.runtime;

import com.hartwig.bam.runtime.configuration.BwaParameters;
import com.hartwig.bam.runtime.configuration.Configuration;
import com.hartwig.bam.runtime.configuration.PatientParameters;
import com.hartwig.bam.runtime.configuration.PipelineParameters;
import com.hartwig.bam.runtime.configuration.ReferenceGenomeParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleCloudBamCreationRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudBamCreationRuntime.class);

    public static void main(String[] args) {
        if (args.length == 5) {
            String version = args[0];
            String runId = args[1];
            String project = args[2];
            String namespace = args[3];
            String jobName = args[4];
            LOGGER.info("Starting pipeline with version [{}] run id [{}] for project [{}] in namespace [{}] on Google Dataproc",
                    version,
                    runId,
                    project,
                    namespace);

            Configuration configuration = Configuration.builder()
                    .pipeline(PipelineParameters.builder()
                            .hdfs("gs:///")
                            .bwa(BwaParameters.builder().threads(1).build())
                            .resultsDirectory(namespace + "/results")
                            .build())
                    .referenceGenome(ReferenceGenomeParameters.builder()
                            .file("reference.fasta")
                            .directory(namespace + "/reference_genome")
                            .build())
                    .patient(PatientParameters.builder().directory(namespace + "/samples").name("").build())
                    .build();

            new PipelineRuntime(configuration, jobName).start();
        }

    }
}
