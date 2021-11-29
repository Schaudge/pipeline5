package com.hartwig.batch.operations.rna;

import static java.lang.String.format;

import static com.hartwig.batch.operations.rna.RnaCommon.MAX_EXPECTED_BAM_SIZE_GB;
import static com.hartwig.batch.operations.rna.RnaCommon.RNA_RESOURCES;
import static com.hartwig.batch.operations.rna.RnaCommon.getRnaCohortDirectory;
import static com.hartwig.batch.operations.rna.RnaIsofox.FUNC_TRANSCRIPT_COUNTS;
import static com.hartwig.batch.operations.rna.RnaIsofox.ISOFOX_JAR;
import static com.hartwig.batch.operations.rna.RnaIsofox.ISOFOX_LOCATION;
import static com.hartwig.batch.operations.rna.RnaIsofox.RNA_BAM_FILE_ID;
import static com.hartwig.batch.operations.rna.RnaIsofox.RNA_BAM_INDEX_FILE_ID;
import static com.hartwig.pipeline.resource.RefGenomeVersion.V37;
import static com.hartwig.pipeline.resource.ResourceFilesFactory.buildResourceFiles;

import java.util.StringJoiner;

import com.hartwig.batch.BatchOperation;
import com.hartwig.batch.OperationDescriptor;
import com.hartwig.batch.input.InputBundle;
import com.hartwig.batch.input.InputFileDescriptor;
import com.hartwig.pipeline.ResultsDirectory;
import com.hartwig.pipeline.execution.vm.Bash;
import com.hartwig.pipeline.execution.vm.BashStartupScript;
import com.hartwig.pipeline.execution.vm.ImmutableVirtualMachineJobDefinition;
import com.hartwig.pipeline.execution.vm.OutputUpload;
import com.hartwig.pipeline.execution.vm.RuntimeFiles;
import com.hartwig.pipeline.execution.vm.VirtualMachineJobDefinition;
import com.hartwig.pipeline.execution.vm.VmDirectories;
import com.hartwig.pipeline.resource.RefGenomeVersion;
import com.hartwig.pipeline.resource.ResourceFiles;
import com.hartwig.pipeline.storage.GoogleStorageLocation;
import com.hartwig.pipeline.storage.RuntimeBucket;

public class RnaIsofoxUnmapped implements BatchOperation
{
    private static final int COL_SAMPLE_ID = 0;

    @Override
    public VirtualMachineJobDefinition execute(
            InputBundle inputs, RuntimeBucket bucket, BashStartupScript startupScript, RuntimeFiles executionFlags) {

        InputFileDescriptor descriptor = inputs.get();

        final String batchInputs = descriptor.inputValue();
        final String[] batchItems = batchInputs.split(",");

        if(batchItems.length < 2)
        {
            System.out.print(String.format("invalid input arguments(%s) - expected SampleId,ReadLength", batchInputs));
            return null;
        }

        final String sampleId = batchItems[COL_SAMPLE_ID];
        final RefGenomeVersion refGenomeVersion = V37;

        final ResourceFiles resourceFiles = buildResourceFiles(refGenomeVersion);

        final String samplesDir = String.format("%s/%s", getRnaCohortDirectory(refGenomeVersion), "samples");

        // copy down BAM and index file for this sample
        final String bamFile = String.format("%s%s", sampleId, RNA_BAM_FILE_ID);
        startupScript.addCommand(() -> format("gsutil -u hmf-crunch cp %s/%s/%s %s",
                samplesDir, sampleId, bamFile, VmDirectories.INPUT));

        final String bamIndexFile = String.format("%s%s", sampleId, RNA_BAM_INDEX_FILE_ID);
        startupScript.addCommand(() -> format("gsutil -u hmf-crunch cp %s/%s/%s %s",
                samplesDir, sampleId, bamIndexFile, VmDirectories.INPUT));

        // copy down the executable
        startupScript.addCommand(() -> format("gsutil -u hmf-crunch cp %s/%s %s",
                ISOFOX_LOCATION, ISOFOX_JAR, VmDirectories.TOOLS));

        startupScript.addCommand(() -> format("cd %s", VmDirectories.OUTPUT));

        // run Isofox
        StringJoiner isofoxArgs = new StringJoiner(" ");
        isofoxArgs.add(String.format("-sample %s", sampleId));
        isofoxArgs.add(String.format("-functions UNMAPPED_READS"));

        isofoxArgs.add(String.format("-output_dir %s/", VmDirectories.OUTPUT));
        isofoxArgs.add(String.format("-bam_file %s/%s", VmDirectories.INPUT, bamFile));

        isofoxArgs.add(String.format("-ref_genome %s", resourceFiles.refGenomeFile()));
        isofoxArgs.add(String.format("-ensembl_data_dir %s", resourceFiles.ensemblDataCache()));

        final String threadCount = Bash.allCpus();
        isofoxArgs.add(String.format("-threads %s", threadCount));

        startupScript.addCommand(() -> format("java -jar %s/%s %s", VmDirectories.TOOLS, ISOFOX_JAR, isofoxArgs.toString()));

        // upload the results
        startupScript.addCommand(new OutputUpload(GoogleStorageLocation.of(bucket.name(), "isofox"), executionFlags));

        return ImmutableVirtualMachineJobDefinition.builder().name("rna-isofox").startupCommand(startupScript)
                .namespacedResults(ResultsDirectory.defaultDirectory())
                .workingDiskSpaceGb(MAX_EXPECTED_BAM_SIZE_GB)
                .build();
    }

    @Override
    public OperationDescriptor descriptor() {
        return OperationDescriptor.of("RnaIsofoxUnmapped", "Isofox unmapped read analysis",
                OperationDescriptor.InputType.FLAT);
    }

}
