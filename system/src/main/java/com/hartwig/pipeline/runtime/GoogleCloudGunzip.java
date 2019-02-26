package com.hartwig.pipeline.runtime;

import com.hartwig.patient.Patient;
import com.hartwig.patient.input.PatientReader;
import com.hartwig.pipeline.GunZip;
import com.hartwig.pipeline.runtime.configuration.Configuration;
import com.hartwig.pipeline.runtime.configuration.KnownIndelParameters;
import com.hartwig.pipeline.runtime.configuration.KnownSnpParameters;
import com.hartwig.pipeline.runtime.configuration.PatientParameters;
import com.hartwig.pipeline.runtime.configuration.PipelineParameters;
import com.hartwig.pipeline.runtime.configuration.ReferenceGenomeParameters;
import com.hartwig.pipeline.runtime.spark.SparkContexts;
import com.hartwig.support.hadoop.Hadoop;

import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleCloudGunzip {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudGunzip.class);

    private final Configuration configuration;

    private GoogleCloudGunzip(final Configuration configuration) {
        this.configuration = configuration;
    }

    private void execute() {
        JavaSparkContext javaSparkContext = SparkContexts.create("ADAM", configuration);
        SparkContext sparkContext = javaSparkContext.sc();
        try {
            FileSystem fileSystem = Hadoop.fileSystem(configuration.pipeline().hdfs());
            Patient patient = PatientReader.fromHDFS(fileSystem, configuration.patient().directory(), configuration.patient().name());
            GunZip.execute(fileSystem, javaSparkContext, patient.reference(), false);
        } catch (Exception e) {
            LOGGER.error("Fatal error while running ADAM pipeline. See stack trace for more details", e);
            throw new RuntimeException(e);
        } finally {
            LOGGER.info("Pipeline complete, stopping spark context");
            sparkContext.stop();
            LOGGER.info("Spark context stopped");
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        Configuration configuration = Configuration.builder()
                .pipeline(PipelineParameters.builder().hdfs("gs:///").build())
                .referenceGenome(ReferenceGenomeParameters.builder().file("N/A").build())
                .knownIndel(KnownIndelParameters.builder().addFiles("N/A").build())
                .knownSnp(KnownSnpParameters.builder().addFiles("N/A").build())
                .patient(PatientParameters.builder().directory("/samples").name("").build())
                .build();
        new GoogleCloudGunzip(configuration).execute();
    }
}
