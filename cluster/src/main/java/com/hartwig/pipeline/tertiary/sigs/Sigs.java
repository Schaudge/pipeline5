package com.hartwig.pipeline.tertiary.sigs;

import static com.hartwig.pipeline.execution.vm.InputDownload.initialiseOptionalLocation;
import static com.hartwig.pipeline.tools.HmfTool.SIGS;

import java.util.List;

import com.hartwig.pipeline.Arguments;
import com.hartwig.pipeline.ResultsDirectory;
import com.hartwig.pipeline.datatypes.DataType;
import com.hartwig.pipeline.execution.PipelineStatus;
import com.hartwig.pipeline.execution.vm.BashCommand;
import com.hartwig.pipeline.execution.vm.BashStartupScript;
import com.hartwig.pipeline.execution.vm.InputDownload;
import com.hartwig.pipeline.execution.vm.VirtualMachineJobDefinition;
import com.hartwig.pipeline.execution.vm.VirtualMachinePerformanceProfile;
import com.hartwig.pipeline.execution.vm.VmDirectories;
import com.hartwig.pipeline.execution.vm.java.JavaJarCommand;
import com.hartwig.pipeline.input.SomaticRunMetadata;
import com.hartwig.pipeline.output.AddDatatype;
import com.hartwig.pipeline.output.ArchivePath;
import com.hartwig.pipeline.output.EntireOutputComponent;
import com.hartwig.pipeline.output.Folder;
import com.hartwig.pipeline.output.RunLogComponent;
import com.hartwig.pipeline.reruns.PersistedDataset;
import com.hartwig.pipeline.reruns.PersistedLocations;
import com.hartwig.pipeline.resource.ResourceFiles;
import com.hartwig.pipeline.stages.Namespace;
import com.hartwig.pipeline.stages.Stage;
import com.hartwig.pipeline.storage.GoogleStorageLocation;
import com.hartwig.pipeline.storage.RuntimeBucket;
import com.hartwig.pipeline.tertiary.purple.PurpleOutput;

@Namespace(Sigs.NAMESPACE)
public class Sigs implements Stage<SigsOutput, SomaticRunMetadata> {
    public static final String ALLOCATION_TSV = ".sig.allocation.tsv";
    public static final String NAMESPACE = "sigs";

    private final InputDownload purpleSomaticVariantsDownload;

    private final ResourceFiles resourceFiles;
    private final PersistedDataset persistedDataset;

    public Sigs(final PurpleOutput purpleOutput, final ResourceFiles resourceFiles, final PersistedDataset persistedDataset) {
        purpleSomaticVariantsDownload = initialiseOptionalLocation(purpleOutput.outputLocations().somaticVariants());
        this.resourceFiles = resourceFiles;
        this.persistedDataset = persistedDataset;
    }

    @Override
    public List<BashCommand> inputs() {
        return List.of(purpleSomaticVariantsDownload);
    }

    @Override
    public String namespace() {
        return NAMESPACE;
    }

    @Override
    public List<BashCommand> tumorReferenceCommands(final SomaticRunMetadata metadata) {
        return buildCommands(metadata);
    }

    @Override
    public List<BashCommand> tumorOnlyCommands(final SomaticRunMetadata metadata) {
        return buildCommands(metadata);
    }

    private List<BashCommand> buildCommands(final SomaticRunMetadata metadata) {
        return List.of(new JavaJarCommand(SIGS, buildArguments(metadata)));
    }

    private List<String> buildArguments(final SomaticRunMetadata metadata) {
        return List.of("-sample",
                metadata.tumor().sampleName(),
                "-signatures_file",
                resourceFiles.snvSignatures(),
                "-somatic_vcf_file",
                purpleSomaticVariantsDownload.getLocalTargetPath(),
                "-output_dir",
                VmDirectories.OUTPUT);
    }

    @Override
    public VirtualMachineJobDefinition vmDefinition(final BashStartupScript bash, final ResultsDirectory resultsDirectory) {
        return VirtualMachineJobDefinition.builder()
                .name(NAMESPACE)
                .startupCommand(bash)
                .namespacedResults(resultsDirectory)
                .performanceProfile(VirtualMachinePerformanceProfile.custom(4, 16))
                .workingDiskSpaceGb(375)
                .build();
    }

    @Override
    public SigsOutput output(final SomaticRunMetadata metadata, final PipelineStatus jobStatus, final RuntimeBucket bucket,
            final ResultsDirectory resultsDirectory) {
        return SigsOutput.builder()
                .status(jobStatus)
                .maybeAllocationTsv(GoogleStorageLocation.of(bucket.name(), resultsDirectory.path(allocationTsv(metadata))))
                .addFailedLogLocations(GoogleStorageLocation.of(bucket.name(), RunLogComponent.LOG_FILE))
                .addReportComponents(new EntireOutputComponent(bucket, Folder.root(), namespace(), resultsDirectory))
                .addAllDatatypes(addDatatypes(metadata))
                .build();
    }

    @Override
    public List<AddDatatype> addDatatypes(final SomaticRunMetadata metadata) {
        return List.of(new AddDatatype(DataType.SIGNATURE_ALLOCATION,
                metadata.barcode(),
                new ArchivePath(Folder.root(), namespace(), allocationTsv(metadata))));
    }

    @Override
    public SigsOutput skippedOutput(final SomaticRunMetadata metadata) {
        return SigsOutput.builder().status(PipelineStatus.SKIPPED).build();
    }

    @Override
    public SigsOutput persistedOutput(final SomaticRunMetadata metadata) {
        return SigsOutput.builder()
                .status(PipelineStatus.PERSISTED)
                .maybeAllocationTsv(persistedDataset.path(metadata.tumor().sampleName(), DataType.SIGNATURE_ALLOCATION)
                        .orElse(GoogleStorageLocation.of(metadata.bucket(),
                                PersistedLocations.blobForSet(metadata.set(), namespace(), allocationTsv(metadata)))))
                .addAllDatatypes(addDatatypes(metadata))
                .build();
    }

    @Override
    public boolean shouldRun(final Arguments arguments) {
        return !arguments.shallow() && arguments.runTertiary() && !arguments.useTargetRegions();
    }

    private String allocationTsv(final SomaticRunMetadata metadata) {
        return metadata.tumor().sampleName() + ALLOCATION_TSV;
    }
}
