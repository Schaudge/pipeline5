package com.hartwig.pipeline.tertiary.amber;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.hartwig.pipeline.datatypes.DataType;
import com.hartwig.pipeline.metadata.AddDatatypeToFile;
import com.hartwig.pipeline.metadata.ApiFileOperation;
import com.hartwig.pipeline.metadata.SomaticRunMetadata;
import com.hartwig.pipeline.report.Folder;
import com.hartwig.pipeline.stages.Stage;
import com.hartwig.pipeline.storage.GoogleStorageLocation;
import com.hartwig.pipeline.tertiary.TertiaryStageTest;
import com.hartwig.pipeline.testsupport.TestInputs;

import org.junit.Before;

public class AmberTest extends TertiaryStageTest<AmberOutput> {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected Stage<AmberOutput, SomaticRunMetadata> createVictim() {
        return new Amber(TestInputs.defaultPair(), TestInputs.REG_GENOME_37_RESOURCE_FILES, persistedDataset);
    }

    @Override
    protected List<ApiFileOperation> expectedFurtherOperations() {
        String basenameLoci = TestInputs.REG_GENOME_37_RESOURCE_FILES.amberHeterozygousLoci()
                .substring(TestInputs.REG_GENOME_37_RESOURCE_FILES.amberHeterozygousLoci().lastIndexOf("/") + 1);
        return List.of(new AddDatatypeToFile(DataType.B_ALLELE_FREQUENCY,
                        Folder.root(),
                        Amber.NAMESPACE,
                        "tumor.amber.baf.tsv",
                        TestInputs.defaultSomaticRunMetadata().barcode()),
                new AddDatatypeToFile(DataType.GERMLINE_HETERO_PON,
                        Folder.root(),
                        Amber.NAMESPACE,
                        basenameLoci,
                        TestInputs.defaultSomaticRunMetadata().barcode()));
    }

    @Override
    protected void validateOutput(final AmberOutput output) {
        assertThat(output.outputDirectory().bucket()).isEqualTo("run-reference-tumor-test/amber");
        assertThat(output.outputDirectory().path()).isEqualTo("results");
        assertThat(output.outputDirectory().isDirectory()).isTrue();
    }

    @Override
    protected void validatePersistedOutput(final AmberOutput output) {
        assertThat(output.outputDirectory()).isEqualTo(GoogleStorageLocation.of(OUTPUT_BUCKET, "set/amber", true));
    }

    @Override
    protected void setupPersistedDataset() {
        persistedDataset.addPath(DataType.B_ALLELE_FREQUENCY, "amber");
    }

    @Override
    protected void validatePersistedOutputFromPersistedDataset(final AmberOutput output) {
        assertThat(output.outputDirectory()).isEqualTo(GoogleStorageLocation.of(OUTPUT_BUCKET, "amber", true));
    }

    @Override
    protected List<String> expectedCommands() {
        return List.of("java -Xmx32G -cp /opt/tools/amber/3.4/amber.jar com.hartwig.hmftools.amber.AmberApplication "
                        + "-reference reference -reference_bam /data/input/reference.bam -tumor tumor -tumor_bam /data/input/tumor.bam -output_dir "
                        + "/data/output -threads $(grep -c '^processor' /proc/cpuinfo) -ref_genome "
                        + "/opt/resources/reference_genome/37/Homo_sapiens.GRCh37.GATK.illumina.fasta "
                        + "-loci /opt/resources/amber/37/GermlineHetPon.hg19.vcf.gz",
                "cp /opt/resources/amber/37/GermlineHetPon.hg19.vcf.gz /data/output");
    }
}