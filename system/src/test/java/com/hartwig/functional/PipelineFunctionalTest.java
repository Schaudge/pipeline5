package com.hartwig.functional;

import static com.hartwig.testsupport.Assertions.assertThatOutput;
import static com.hartwig.testsupport.TestConfigurations.HUNDREDK_READS_HISEQ;
import static com.hartwig.testsupport.TestConfigurations.HUNDREDK_READS_HISEQ_PATIENT_NAME;

import java.io.File;

import com.hartwig.io.OutputType;
import com.hartwig.patient.Sample;
import com.hartwig.patient.io.PatientReader;
import com.hartwig.pipeline.adam.ADAMPipelines;
import com.hartwig.support.hadoop.Hadoop;
import com.hartwig.testsupport.SparkContextSingleton;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.spark.api.java.JavaSparkContext;
import org.bdgenomics.adam.rdd.ADAMContext;
import org.junit.BeforeClass;
import org.junit.Test;

public class PipelineFunctionalTest {

    private static final Sample REFERENCE_SAMPLE =
            Sample.builder(HUNDREDK_READS_HISEQ.patient().directory(), HUNDREDK_READS_HISEQ_PATIENT_NAME + "R").build();
    private static final Sample TUMOUR_SAMPLE =
            Sample.builder(HUNDREDK_READS_HISEQ.patient().directory(), HUNDREDK_READS_HISEQ_PATIENT_NAME + "T").build();
    private static JavaSparkContext context;

    private static String RESULT_DIR = System.getProperty("user.dir") + "/results/";

    @BeforeClass
    public static void beforeClass() throws Exception {
        FileUtils.deleteDirectory(new File(RESULT_DIR));
        context = SparkContextSingleton.instance();
    }

    @Test
    public void adamBamCreationMatchesCurrentPipelineOuput() throws Exception {
        FileSystem fileSystem = Hadoop.localFilesystem();
        ADAMPipelines.bamCreation(new ADAMContext(context.sc()),
                fileSystem,
                RESULT_DIR,
                HUNDREDK_READS_HISEQ.referenceGenome().path(),
                HUNDREDK_READS_HISEQ.knownIndel().paths(),
                1, false, false, true).execute(PatientReader.fromHDFS(fileSystem, HUNDREDK_READS_HISEQ.patient().directory(), HUNDREDK_READS_HISEQ_PATIENT_NAME));
        assertThatOutput(RESULT_DIR, OutputType.DUPLICATE_MARKED, REFERENCE_SAMPLE).sorted().aligned().duplicatesMarked().isEqualToExpected();
        assertThatOutput(RESULT_DIR, OutputType.DUPLICATE_MARKED, TUMOUR_SAMPLE).sorted().aligned().duplicatesMarked().isEqualToExpected();
    }
}