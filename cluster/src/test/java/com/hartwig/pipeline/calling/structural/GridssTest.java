package com.hartwig.pipeline.calling.structural;

import static java.lang.String.format;

import static com.hartwig.pipeline.calling.structural.gridss.stage.SvCalling.GRIDSS_SCRIPT;
import static com.hartwig.pipeline.calling.structural.gridss.stage.SvCalling.SV_PREP_DEPTH_ANNOTATION;
import static com.hartwig.pipeline.testsupport.TestInputs.REFERENCE_BUCKET;
import static com.hartwig.pipeline.testsupport.TestInputs.TUMOR_BUCKET;
import static com.hartwig.pipeline.testsupport.TestInputs.toolCommand;
import static com.hartwig.pipeline.tools.HmfTool.SV_PREP;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hartwig.pipeline.Arguments;
import com.hartwig.pipeline.calling.structural.gridss.Gridss;
import com.hartwig.pipeline.calling.structural.gridss.GridssOutput;
import com.hartwig.pipeline.datatypes.DataType;
import com.hartwig.pipeline.input.SomaticRunMetadata;
import com.hartwig.pipeline.stages.Stage;
import com.hartwig.pipeline.stages.StageTest;
import com.hartwig.pipeline.storage.GoogleStorageLocation;
import com.hartwig.pipeline.testsupport.TestInputs;

import org.junit.Before;

public class GridssTest extends StageTest<GridssOutput, SomaticRunMetadata> {
    private static final String TUMOR_GRIDSS_UNFILTERED_VCF_GZ = "tumor.gridss.unfiltered.vcf.gz";
    private static final String GRIDSS = "gridss/";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void returnsExpectedOutput() {
        // not supported currently
    }

    @Override
    public void returnsExpectedFurtherOperations() {
        // not supported currently
    }

    @Override
    public void addsLogs() {
        // not supported currently
    }

    @Override
    protected void setupPersistedDataset() {
        persistedDataset.addPath(DataType.STRUCTURAL_VARIANTS_GRIDSS, GRIDSS + TUMOR_GRIDSS_UNFILTERED_VCF_GZ);
    }

    @Override
    protected void validatePersistedOutputFromPersistedDataset(final GridssOutput output) {
        assertThat(output.unfilteredVariants()).isEqualTo(GoogleStorageLocation.of(OUTPUT_BUCKET, GRIDSS + TUMOR_GRIDSS_UNFILTERED_VCF_GZ));
    }

    @Override
    protected void validatePersistedOutput(final GridssOutput output) {
        assertThat(output.unfilteredVariants()).isEqualTo(GoogleStorageLocation.of(OUTPUT_BUCKET,
                "set/gridss/" + TUMOR_GRIDSS_UNFILTERED_VCF_GZ));
    }

    @Override
    protected Arguments createDisabledArguments() {
        return Arguments.testDefaultsBuilder().runTertiary(false).build();
    }

    @Override
    protected Stage<GridssOutput, SomaticRunMetadata> createVictim() {
        return new Gridss(TestInputs.defaultPair(), TestInputs.REF_GENOME_37_RESOURCE_FILES, persistedDataset);
    }

    @Override
    protected SomaticRunMetadata input() {
        return TestInputs.defaultSomaticRunMetadata();
    }

    @Override
    protected List<String> expectedInputs() {
        return ImmutableList.of(inputDownload(REFERENCE_BUCKET, "reference.bam"),
                inputDownload(REFERENCE_BUCKET, "reference.bam.bai"),
                inputDownload(TUMOR_BUCKET, "tumor.bam"),
                inputDownload(TUMOR_BUCKET, "tumor.bam.bai"));
    }

    @Override
    protected String expectedRuntimeBucketName() {
        return TestInputs.SOMATIC_BUCKET;
    }

    @Override
    protected List<String> expectedCommands() {
        List<String> expectedCommands = Lists.newArrayList();

        expectedCommands.add("export PATH=\"${PATH}:/opt/tools/bwa/0.7.17\"");
        expectedCommands.add("export PATH=\"${PATH}:/opt/tools/samtools/1.14\"");

        // @formatter:off
        expectedCommands.add(
                toolCommand(SV_PREP)
                + " -sample tumor "
                + "-bam_file /data/input/tumor.bam "
                + "-ref_genome /opt/resources/reference_genome/37/Homo_sapiens.GRCh37.GATK.illumina.fasta "
                + "-ref_genome_version V37 "
                + "-blacklist_bed /opt/resources/gridss/37/sv_prep_blacklist.37.bed "
                + "-known_fusion_bed /opt/resources/fusions/37/known_fusions.37.bedpe "
                + "-write_types \"JUNCTIONS;BAM;FRAGMENT_LENGTH_DIST\" "
                + "-output_dir /data/output "
                + "-threads $(grep -c '^processor' /proc/cpuinfo)");

        expectedCommands.add("/opt/tools/samtools/1.14/samtools sort "
                + "-@ $(grep -c '^processor' /proc/cpuinfo) -m 2G -T tmp "
                + "-O bam /data/output/tumor.sv_prep.bam "
                + "-o /data/output/tumor.sv_prep.sorted.bam");

        expectedCommands.add("/opt/tools/samtools/1.14/samtools index "
                + "-@ $(grep -c '^processor' /proc/cpuinfo) /data/output/tumor.sv_prep.sorted.bam");

        expectedCommands.add(
                toolCommand(SV_PREP)
                + " -sample reference "
                + "-bam_file /data/input/reference.bam "
                + "-ref_genome /opt/resources/reference_genome/37/Homo_sapiens.GRCh37.GATK.illumina.fasta "
                + "-ref_genome_version V37 "
                + "-blacklist_bed /opt/resources/gridss/37/sv_prep_blacklist.37.bed "
                + "-known_fusion_bed /opt/resources/fusions/37/known_fusions.37.bedpe "
                + "-existing_junction_file /data/output/tumor.sv_prep.junctions.tsv "
                + "-write_types \"JUNCTIONS;BAM;FRAGMENT_LENGTH_DIST\" "
                + "-output_dir /data/output "
                + "-threads $(grep -c '^processor' /proc/cpuinfo)");

        expectedCommands.add("/opt/tools/samtools/1.14/samtools sort "
                + "-@ $(grep -c '^processor' /proc/cpuinfo) -m 2G -T tmp "
                + "-O bam /data/output/reference.sv_prep.bam "
                + "-o /data/output/reference.sv_prep.sorted.bam");

        expectedCommands.add("/opt/tools/samtools/1.14/samtools index "
                + "-@ $(grep -c '^processor' /proc/cpuinfo) /data/output/reference.sv_prep.sorted.bam");

        expectedCommands.add(
                format("/opt/tools/%s/%s/%s --steps all", SV_PREP.directory(), SV_PREP.runVersion(), GRIDSS_SCRIPT)
                + " --output /data/output/tumor.gridss.vcf.gz "
                + "--workingdir /data/output "
                + "--reference /opt/resources/reference_genome/37/Homo_sapiens.GRCh37.GATK.illumina.fasta "
                + "--jar /opt/tools/gridss/2.13.2/gridss.jar "
                + "--blacklist /opt/resources/gridss/37/gridss_blacklist.37.bed.gz "
                + "--configuration /opt/resources/gridss/gridss.properties "
                + "--labels reference,tumor "
                + "--bams /data/input/reference.bam,/data/input/tumor.bam "
                + "--filtered_bams /data/output/reference.sv_prep.sorted.bam,/data/output/tumor.sv_prep.sorted.bam "
                + "--jvmheap 48G --threads 10");

        expectedCommands.add(
                toolCommand(SV_PREP, SV_PREP_DEPTH_ANNOTATION)
                + " -input_vcf /data/output/tumor.gridss.vcf.gz "
                + "-output_vcf /data/output/tumor.gridss.driver.vcf.gz "
                + "-samples reference,tumor "
                + "-bam_files /data/input/reference.bam,/data/input/tumor.bam "
                + "-ref_genome /opt/resources/reference_genome/37/Homo_sapiens.GRCh37.GATK.illumina.fasta "
                + "-ref_genome_version V37 "
                + "-threads $(grep -c '^processor' /proc/cpuinfo)");

        expectedCommands.add("java -Xmx8G -Dsamjdk.create_index=true "
                + "-Dsamjdk.use_async_io_read_samtools=true "
                + "-Dsamjdk"
                + ".use_async_io_write_samtools=true "
                + "-Dsamjdk.use_async_io_write_tribble=true "
                + "-Dsamjdk.buffer_size=4194304 "
                + "-cp /opt/tools/gridss/2.13.2/gridss.jar "
                + "gridss.AnnotateInsertedSequence "
                + "REFERENCE_SEQUENCE=/opt/resources/virus_reference_genome/human_virus.fa "
                + "INPUT=/data/output/tumor.gridss.driver.vcf.gz " + "OUTPUT=/data/output/tumor.gridss.unfiltered.vcf.gz "
                + "ALIGNMENT=APPEND WORKER_THREADS=$(grep -c '^processor' /proc/cpuinfo)");
        // @formatter:on

        return expectedCommands;
    }

    @Override
    protected void validateOutput(final GridssOutput output) {
        // no further validation yet
    }

    private String inputDownload(final String bucket, final String basename) {
        return input(format("%s/aligner/results/%s", bucket, basename), basename);
    }
}
