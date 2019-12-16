package com.hartwig.batch.operations;

import com.hartwig.batch.BatchOperation;
import com.hartwig.batch.input.InputBundle;
import com.hartwig.batch.input.InputFileDescriptor;
import com.hartwig.pipeline.ResultsDirectory;
import com.hartwig.pipeline.calling.command.VersionedToolCommand;
import com.hartwig.pipeline.execution.vm.Bash;
import com.hartwig.pipeline.execution.vm.BashStartupScript;
import com.hartwig.pipeline.execution.vm.OutputUpload;
import com.hartwig.pipeline.execution.vm.RuntimeFiles;
import com.hartwig.pipeline.execution.vm.VirtualMachineJobDefinition;
import com.hartwig.pipeline.execution.vm.VirtualMachinePerformanceProfile;
import com.hartwig.pipeline.execution.vm.VmDirectories;
import com.hartwig.pipeline.resource.Resource;
import com.hartwig.pipeline.storage.GoogleStorageLocation;
import com.hartwig.pipeline.storage.RuntimeBucket;
import com.hartwig.pipeline.tools.Versions;

import java.io.File;

public class SambambaCramaBam implements BatchOperation {
    public VirtualMachineJobDefinition execute(final InputBundle inputs, final RuntimeBucket bucket,
                                               final BashStartupScript startupScript, final RuntimeFiles executionFlags) {
        InputFileDescriptor input = inputs.get();
        String outputFile = VmDirectories.outputFile(new File(input.remoteFilename()).getName().replaceAll("\\.bam$", ".cram"));
        String localInput = String.format("%s/%s", VmDirectories.INPUT, new File(input.remoteFilename()).getName());
        startupScript.addCommand(() -> input.toCommandForm(localInput));
        startupScript.addCommand(new VersionedToolCommand("sambamba",
                "sambamba",
                Versions.SAMBAMBA,
                "view",
                localInput,
                "-o",
                outputFile,
                "-t",
                Bash.allCpus(),
                "--format=cram",
                "-T",
                Resource.REFERENCE_GENOME_FASTA));
        startupScript.addCommand(new OutputUpload(GoogleStorageLocation.of(bucket.name(), "cram"), executionFlags));

        return VirtualMachineJobDefinition.builder().name("cram").startupCommand(startupScript)
                .namespacedResults(ResultsDirectory.defaultDirectory())
                .performanceProfile(VirtualMachinePerformanceProfile.custom(4, 6))
                .build();
    }

    @Override
    public OperationDescriptor descriptor() {
        return OperationDescriptor.of("SambambaCramaBam", "Produce a CRAM file from each inputs BAM",
                OperationDescriptor.InputType.FLAT);
    }
}
