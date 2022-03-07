package com.hartwig.pipeline.calling.sage;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hartwig.pipeline.ResultsDirectory;
import com.hartwig.pipeline.alignment.AlignmentPair;
import com.hartwig.pipeline.execution.PipelineStatus;
import com.hartwig.pipeline.execution.vm.BashCommand;
import com.hartwig.pipeline.execution.vm.BashStartupScript;
import com.hartwig.pipeline.execution.vm.VirtualMachineJobDefinition;
import com.hartwig.pipeline.metadata.AddDatatype;
import com.hartwig.pipeline.metadata.ArchivePath;
import com.hartwig.pipeline.metadata.SingleSampleRunMetadata;
import com.hartwig.pipeline.metadata.SomaticRunMetadata;
import com.hartwig.pipeline.report.Folder;
import com.hartwig.pipeline.report.ReportComponent;
import com.hartwig.pipeline.report.RunLogComponent;
import com.hartwig.pipeline.report.SingleFileComponent;
import com.hartwig.pipeline.report.StartupScriptComponent;
import com.hartwig.pipeline.report.ZippedVcfAndIndexComponent;
import com.hartwig.pipeline.reruns.PersistedDataset;
import com.hartwig.pipeline.reruns.PersistedLocations;
import com.hartwig.pipeline.stages.SubStageInputOutput;
import com.hartwig.pipeline.storage.GoogleStorageLocation;
import com.hartwig.pipeline.storage.RuntimeBucket;
import com.hartwig.pipeline.tertiary.TertiaryStage;

public class SageCaller extends TertiaryStage<SageOutput> {

    public static final String SAGE_GENE_COVERAGE_TSV = "sage.gene.coverage.tsv";
    public static final String SAGE_BQR_PNG = "sage.bqr.png";

    private final PersistedDataset persistedDataset;
    private final SageConfiguration sageConfiguration;

    public SageCaller(final AlignmentPair alignmentPair, final PersistedDataset persistedDataset,
            final SageConfiguration sageConfiguration) {
        super(alignmentPair);
        this.persistedDataset = persistedDataset;
        this.sageConfiguration = sageConfiguration;
    }

    @Override
    public List<BashCommand> normalOnlyCommands(final SomaticRunMetadata metadata) {
        return metadata.maybeReference()
                .map(r -> new SageApplication(sageConfiguration.commandBuilder()
                        .addReference(r.sampleName(),
                                getReferenceBamDownload().getLocalTargetPath())).apply(SubStageInputOutput.empty(r.sampleName())).bash())
                .orElse(Collections.emptyList());
    }

    @Override
    public List<BashCommand> tumorNormalCommands(final SomaticRunMetadata metadata) {
        return new SageApplication(sageConfiguration.commandBuilder()
                .addTumor(metadata.tumor().sampleName(), getTumorBamDownload().getLocalTargetPath())
                .addReference(metadata.reference().sampleName(),
                        getReferenceBamDownload().getLocalTargetPath())).andThen(sageConfiguration.postProcess().apply(metadata))
                .apply(SubStageInputOutput.empty(metadata.tumor().sampleName()))
                .bash();
    }

    @Override
    public String namespace() {
        return sageConfiguration.namespace();
    }

    @Override
    public VirtualMachineJobDefinition vmDefinition(final BashStartupScript bash, final ResultsDirectory resultsDirectory) {
        return VirtualMachineJobDefinition.sageCalling(bash, resultsDirectory, namespace());
    }

    @Override
    public SageOutput output(final SomaticRunMetadata metadata, final PipelineStatus jobStatus, final RuntimeBucket bucket,
            final ResultsDirectory resultsDirectory) {
        return outputBuilder(metadata, jobStatus, bucket, resultsDirectory).build();
    }

    protected ImmutableSageOutput.Builder outputBuilder(final SomaticRunMetadata metadata, final PipelineStatus jobStatus,
            final RuntimeBucket bucket, final ResultsDirectory resultsDirectory) {

        final String filteredOutputFile = sageConfiguration.filteredTemplate().apply(metadata);
        final String unfilteredOutputFile = sageConfiguration.unfilteredTemplate().apply(metadata);
        final String geneCoverageFile = sageConfiguration.geneCoverageTemplate().apply(metadata);
        final Optional<String> somaticRefSampleBqrPlot = somaticRefSampleBqrPlot(metadata);
        final Optional<String> somaticTumorSampleBqrPlot = somaticTumorSampleBqrPlot(metadata);
        final ImmutableSageOutput.Builder builder = SageOutput.builder(namespace()).status(jobStatus);
        somaticRefSampleBqrPlot.ifPresent(s -> builder.maybeSomaticRefSampleBqrPlot(GoogleStorageLocation.of(bucket.name(),
                        resultsDirectory.path(s)))
                .addReportComponents(bqrComponent(metadata.reference(), "png", bucket, resultsDirectory))
                .addReportComponents(bqrComponent(metadata.reference(), "tsv", bucket, resultsDirectory)));
        somaticTumorSampleBqrPlot.ifPresent(s -> builder.maybeSomaticTumorSampleBqrPlot(GoogleStorageLocation.of(bucket.name(),
                        resultsDirectory.path(s)))
                .addReportComponents(bqrComponent(metadata.tumor(), "png", bucket, resultsDirectory))
                .addReportComponents(bqrComponent(metadata.tumor(), "tsv", bucket, resultsDirectory)));
        return builder.addFailedLogLocations(GoogleStorageLocation.of(bucket.name(), RunLogComponent.LOG_FILE))
                .maybeGermlineGeneCoverage(GoogleStorageLocation.of(bucket.name(), resultsDirectory.path(geneCoverageFile)))
                .maybeSomaticTumorSampleBqrPlot(somaticTumorSampleBqrPlot.map(t -> GoogleStorageLocation.of(bucket.name(),
                        resultsDirectory.path(t))))
                .maybeVariants(GoogleStorageLocation.of(bucket.name(), resultsDirectory.path(filteredOutputFile)))
                .addReportComponents(bqrComponent(metadata.tumor(), "png", bucket, resultsDirectory))
                .addReportComponents(bqrComponent(metadata.tumor(), "tsv", bucket, resultsDirectory))
                .addReportComponents(vcfComponent(unfilteredOutputFile, bucket, resultsDirectory))
                .addReportComponents(vcfComponent(filteredOutputFile, bucket, resultsDirectory))
                .addReportComponents(singleFileComponent(geneCoverageFile, bucket, resultsDirectory))
                .addReportComponents(new RunLogComponent(bucket, namespace(), Folder.root(), resultsDirectory))
                .addReportComponents(new StartupScriptComponent(bucket, namespace(), Folder.root()))
                .addDatatypes(new AddDatatype(sageConfiguration.vcfDatatype(),
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), filteredOutputFile)))
                .addDatatypes(new AddDatatype(sageConfiguration.geneCoverageDatatype(),
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), geneCoverageFile)))
                .addAllDatatypes(somaticRefSampleBqrPlot.stream()
                        .map(r -> new AddDatatype(sageConfiguration.tumorSampleBqrPlot(),
                                metadata.barcode(),
                                new ArchivePath(Folder.root(), namespace(), r)))
                        .collect(Collectors.toList()))
                .addAllDatatypes(somaticTumorSampleBqrPlot.stream()
                        .map(t -> new AddDatatype(sageConfiguration.refSampleBqrPlot(),
                                metadata.barcode(),
                                new ArchivePath(Folder.root(), namespace(), t)))
                        .collect(Collectors.toList()));
    }

    private Optional<String> somaticTumorSampleBqrPlot(final SomaticRunMetadata metadata) {
        return metadata.maybeTumor().map(t -> String.format("%s.%s", t.sampleName(), SAGE_BQR_PNG));
    }

    private Optional<String> somaticRefSampleBqrPlot(final SomaticRunMetadata metadata) {
        return metadata.maybeReference().map(r -> String.format("%s.%s", r.sampleName(), SAGE_BQR_PNG));
    }

    private String geneCoverageFile(final SomaticRunMetadata metadata) {
        return String.format("%s.sage.gene.coverage.tsv", metadata.reference().sampleName());
    }

    @Override
    public SageOutput skippedOutput(final SomaticRunMetadata metadata) {
        return SageOutput.builder(namespace()).status(PipelineStatus.SKIPPED).build();
    }

    @Override
    public final SageOutput persistedOutput(final SomaticRunMetadata metadata) {
        final String filteredOutputFile = sageConfiguration.filteredTemplate().apply(metadata);
        final String geneCoverageFile = geneCoverageFile(metadata);
        final Optional<String> somaticRefSampleBqrPlot = somaticRefSampleBqrPlot(metadata);
        final Optional<String> somaticTumorSampleBqrPlot = somaticTumorSampleBqrPlot(metadata);
        final ImmutableSageOutput.Builder builder = SageOutput.builder(namespace())
                .status(PipelineStatus.PERSISTED)
                .maybeVariants(persistedDataset.path(metadata.tumor().sampleName(), sageConfiguration.vcfDatatype())
                        .orElse(GoogleStorageLocation.of(metadata.bucket(),
                                PersistedLocations.blobForSet(metadata.set(), namespace(), filteredOutputFile))))
                .maybeGermlineGeneCoverage(persistedDataset.path(metadata.tumor().sampleName(), sageConfiguration.geneCoverageDatatype())
                        .orElse(GoogleStorageLocation.of(metadata.bucket(),
                                PersistedLocations.blobForSet(metadata.set(), namespace(), geneCoverageFile))));
        somaticRefSampleBqrPlot.ifPresent(r -> builder.maybeSomaticRefSampleBqrPlot(persistedDataset.path(metadata.tumor().sampleName(),
                        sageConfiguration.refSampleBqrPlot())
                .orElse(GoogleStorageLocation.of(metadata.bucket(), PersistedLocations.blobForSet(metadata.set(), namespace(), r)))));
        somaticTumorSampleBqrPlot.ifPresent(r -> builder.maybeSomaticTumorSampleBqrPlot(persistedDataset.path(metadata.tumor().sampleName(),
                        sageConfiguration.tumorSampleBqrPlot())
                .orElse(GoogleStorageLocation.of(metadata.bucket(), PersistedLocations.blobForSet(metadata.set(), namespace(), r)))));
        return builder.build();
    }

    protected ReportComponent singleFileComponent(String filename, final RuntimeBucket bucket, final ResultsDirectory resultsDirectory) {
        return new SingleFileComponent(bucket, namespace(), Folder.root(), filename, filename, resultsDirectory);
    }

    private ReportComponent bqrComponent(final SingleSampleRunMetadata metadata, final String extension, final RuntimeBucket bucket,
            final ResultsDirectory resultsDirectory) {
        String filename = format("%s.sage.bqr.%s", metadata.sampleName(), extension);
        return singleFileComponent(filename, bucket, resultsDirectory);
    }

    private ReportComponent vcfComponent(final String filename, final RuntimeBucket bucket, final ResultsDirectory resultsDirectory) {
        return new ZippedVcfAndIndexComponent(bucket, namespace(), Folder.root(), filename, resultsDirectory);
    }
}