package com.hartwig.pipeline.tertiary.cobalt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import com.hartwig.pipeline.metadata.SomaticRunMetadata;
import com.hartwig.pipeline.reruns.NoopPersistedDataset;
import com.hartwig.pipeline.stages.Stage;
import com.hartwig.pipeline.storage.GoogleStorageLocation;
import com.hartwig.pipeline.tertiary.TertiaryStageTest;
import com.hartwig.pipeline.testsupport.TestInputs;

import org.junit.Before;

public class CobaltTest extends TertiaryStageTest<CobaltOutput> {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected Stage<CobaltOutput, SomaticRunMetadata> createVictim() {
        return new Cobalt(TestInputs.defaultPair(), TestInputs.HG19_RESOURCE_FILES, new NoopPersistedDataset());
    }

    @Override
    protected List<String> expectedCommands() {
        return Collections.singletonList(
                "java -Xmx8G -cp /opt/tools/cobalt/1.7/cobalt.jar com.hartwig.hmftools.cobalt.CountBamLinesApplication -reference "
                        + "reference -reference_bam /data/input/reference.bam -tumor tumor -tumor_bam /data/input/tumor.bam -output_dir "
                        + "/data/output -threads $(grep -c '^processor' /proc/cpuinfo) -gc_profile /opt/resources/gc/hg19/GC_profile.1000bp.cnp");
    }

    @Override
    protected void validateOutput(final CobaltOutput output) {
        assertThat(output.outputDirectory().bucket()).isEqualTo(expectedRuntimeBucketName() + "/" + Cobalt.NAMESPACE);
        assertThat(output.outputDirectory().path()).isEqualTo("results");
        assertThat(output.outputDirectory().isDirectory()).isTrue();
    }

    @Override
    protected void validatePersistedOutput(final CobaltOutput output) {
        assertThat(output.outputDirectory()).isEqualTo(GoogleStorageLocation.of(OUTPUT_BUCKET, "set/cobalt", true));
    }
}