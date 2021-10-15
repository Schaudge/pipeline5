package com.hartwig.pipeline.tertiary.cuppa;

import static java.lang.String.format;

import java.util.Arrays;
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
import com.hartwig.pipeline.execution.vm.python.Python3Command;
import com.hartwig.pipeline.execution.vm.r.RscriptCommand;
import com.hartwig.pipeline.metadata.AddDatatype;
import com.hartwig.pipeline.metadata.ArchivePath;
import com.hartwig.pipeline.metadata.SomaticRunMetadata;
import com.hartwig.pipeline.report.EntireOutputComponent;
import com.hartwig.pipeline.report.Folder;
import com.hartwig.pipeline.report.RunLogComponent;
import com.hartwig.pipeline.reruns.PersistedDataset;
import com.hartwig.pipeline.reruns.PersistedLocations;
import com.hartwig.pipeline.resource.ResourceFiles;
import com.hartwig.pipeline.stages.Stage;
import com.hartwig.pipeline.storage.GoogleStorageLocation;
import com.hartwig.pipeline.storage.RuntimeBucket;
import com.hartwig.pipeline.tertiary.linx.LinxOutput;
import com.hartwig.pipeline.tertiary.purple.PurpleOutput;
import com.hartwig.pipeline.tools.Versions;

public class Cuppa implements Stage<CuppaOutput, SomaticRunMetadata> {
    public static final String CUP_REPORT_SUMMARY_PNG = ".cup.report.summary.png";
    public static final String CUP_DATA_CSV = ".cup.data.csv";
    public static final String CUPPA_CHART_PNG = ".cuppa.chart.png";
    public static final String CUPPA_CONCLUSION_TXT = ".cuppa.conclusion.txt";
    public static String NAMESPACE = "cuppa";
    private final InputDownload purpleSomaticVcfDownload;
    private final InputDownload purpleStructuralVcfDownload;
    private final InputDownload purpleOutputDirectory;
    private final LinxOutput linxOutput;

    private final ResourceFiles resourceFiles;

    private final PersistedDataset persistedDataset;

    public Cuppa(final PurpleOutput purpleOutput, final LinxOutput linxOutput, final ResourceFiles resourceFiles,
            final PersistedDataset persistedDataset) {
        purpleSomaticVcfDownload = new InputDownload(purpleOutput.outputLocations().somaticVcf());
        purpleStructuralVcfDownload = new InputDownload(purpleOutput.outputLocations().structuralVcf());
        purpleOutputDirectory = new InputDownload(purpleOutput.outputLocations().outputDirectory());
        this.linxOutput = linxOutput;
        this.resourceFiles = resourceFiles;
        this.persistedDataset = persistedDataset;
    }

    @Override
    public List<BashCommand> inputs() {
        return List.of(purpleSomaticVcfDownload, purpleStructuralVcfDownload, purpleOutputDirectory, linxOutputDownload());
    }

    @Override
    public String namespace() {
        return NAMESPACE;
    }

    @Override
    public List<BashCommand> commands(final SomaticRunMetadata metadata) {
        final List<String> r_script_arguments = Arrays.asList(metadata.tumor().sampleName(), VmDirectories.OUTPUT + "/");
        return List.of(new JavaJarCommand("cuppa",
                        Versions.CUPPA,
                        "cuppa.jar",
                        "4G",
                        List.of("-categories",
                                "DNA",
                                "-ref_data_dir",
                                resourceFiles.cuppaRefData(),
                                "-sample_data",
                                metadata.tumor().sampleName(),
                                "-sample_data_dir",
                                linxOutputDownload().getLocalTargetPath(),
                                "-sample_sv_file",
                                purpleStructuralVcfDownload.getLocalTargetPath(),
                                "-sample_somatic_vcf",
                                purpleSomaticVcfDownload.getLocalTargetPath(),
                                "-output_dir",
                                VmDirectories.OUTPUT)),
                new Python3Command("cuppa-chart",
                        Versions.CUPPA,
                        "cuppa-chart.py",
                        List.of("-sample",
                                metadata.tumor().sampleName(),
                                "-sample_data",
                                VmDirectories.outputFile(format("%s.cup.data.csv", metadata.tumor().sampleName())),
                                "-output_dir",
                                VmDirectories.OUTPUT)),
                new RscriptCommand("cuppa", Versions.CUPPA, "CupGenerateReport_pipeline.R", r_script_arguments));
    }

    private InputDownload linxOutputDownload() {
        return new InputDownload(linxOutput.linxOutputLocations().outputDirectory());
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
    public CuppaOutput output(final SomaticRunMetadata metadata, final PipelineStatus jobStatus, final RuntimeBucket bucket,
            final ResultsDirectory resultsDirectory) {
        final String conclusionTxt = cuppaConclusionTxt(metadata);
        final String cuppaChart = cuppaChartPng(metadata);
        final String resultsCsv = cupDataCsv(metadata);
        final String featurePlot = cupReportSummaryPng(metadata);
        return CuppaOutput.builder()
                .status(jobStatus)
                .maybeCuppaOutputLocations(CuppaOutputLocations.builder()
                        .conclusionTxt(GoogleStorageLocation.of(bucket.name(), resultsDirectory.path(conclusionTxt)))
                        .chartPng(GoogleStorageLocation.of(bucket.name(), resultsDirectory.path(cuppaChart)))
                        .featurePlot(GoogleStorageLocation.of(bucket.name(), resultsDirectory.path(featurePlot)))
                        .resultCsv(GoogleStorageLocation.of(bucket.name(), resultsDirectory.path(resultsCsv)))
                        .build())
                .addFailedLogLocations(GoogleStorageLocation.of(bucket.name(), RunLogComponent.LOG_FILE))
                .addReportComponents(new EntireOutputComponent(bucket, Folder.root(), namespace(), resultsDirectory))
                .addDatatypes(new AddDatatype(DataType.CUPPA_CHART,
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), cuppaChart)))
                .addDatatypes(new AddDatatype(DataType.CUPPA_CONCLUSION,
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), conclusionTxt)))
                .addDatatypes(new AddDatatype(DataType.CUPPA_RESULTS,
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), resultsCsv)))
                .addDatatypes(new AddDatatype(DataType.CUPPA_FEATURE_PLOT,
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), featurePlot)))
                .build();
    }

    private String cupReportSummaryPng(final SomaticRunMetadata metadata) {
        return metadata.tumor().sampleName() + CUP_REPORT_SUMMARY_PNG;
    }

    private String cupDataCsv(final SomaticRunMetadata metadata) {
        return metadata.tumor().sampleName() + CUP_DATA_CSV;
    }

    private String cuppaChartPng(final SomaticRunMetadata metadata) {
        return metadata.tumor().sampleName() + CUPPA_CHART_PNG;
    }

    private String cuppaConclusionTxt(final SomaticRunMetadata metadata) {
        return metadata.tumor().sampleName() + CUPPA_CONCLUSION_TXT;
    }

    @Override
    public CuppaOutput skippedOutput(final SomaticRunMetadata metadata) {
        return CuppaOutput.builder().status(PipelineStatus.SKIPPED).build();
    }

    @Override
    public CuppaOutput persistedOutput(final SomaticRunMetadata metadata) {
        final String conclusionTxt = cuppaConclusionTxt(metadata);
        final String cuppaChart = cuppaChartPng(metadata);
        final String resultsCsv = cupDataCsv(metadata);
        final String featurePlot = cupReportSummaryPng(metadata);
        return CuppaOutput.builder()
                .status(PipelineStatus.PERSISTED)
                .maybeCuppaOutputLocations(CuppaOutputLocations.builder()
                        .conclusionTxt(persistedDataset.path(metadata.tumor().sampleName(), DataType.CUPPA_CONCLUSION)
                                .orElse(GoogleStorageLocation.of(metadata.bucket(),
                                        PersistedLocations.blobForSet(metadata.set(), namespace(), conclusionTxt))))
                        .chartPng(persistedDataset.path(metadata.tumor().sampleName(), DataType.CUPPA_CHART)
                                .orElse(GoogleStorageLocation.of(metadata.bucket(),
                                        PersistedLocations.blobForSet(metadata.set(), namespace(), cuppaChart))))
                        .resultCsv(persistedDataset.path(metadata.tumor().sampleName(), DataType.CUPPA_RESULTS)
                                .orElse(GoogleStorageLocation.of(metadata.bucket(),
                                        PersistedLocations.blobForSet(metadata.set(), namespace(), resultsCsv))))
                        .featurePlot(persistedDataset.path(metadata.tumor().sampleName(), DataType.CUPPA_FEATURE_PLOT)
                                .orElse(GoogleStorageLocation.of(metadata.bucket(),
                                        PersistedLocations.blobForSet(metadata.set(), namespace(), featurePlot))))
                        .build())
                .addDatatypes(new AddDatatype(DataType.CUPPA_CONCLUSION,
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), conclusionTxt)))
                .addDatatypes(new AddDatatype(DataType.CUPPA_CHART,
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), cuppaChart)))
                .addDatatypes(new AddDatatype(DataType.CUPPA_RESULTS,
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), resultsCsv)))
                .addDatatypes(new AddDatatype(DataType.CUPPA_FEATURE_PLOT,
                        metadata.barcode(),
                        new ArchivePath(Folder.root(), namespace(), featurePlot)))
                .build();
    }

    @Override
    public boolean shouldRun(final Arguments arguments) {
        return !arguments.shallow() && arguments.runTertiary();
    }
}
