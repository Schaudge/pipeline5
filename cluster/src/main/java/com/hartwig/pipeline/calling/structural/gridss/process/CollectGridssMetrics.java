package com.hartwig.pipeline.calling.structural.gridss.process;

import com.hartwig.pipeline.execution.vm.BashCommand;
import com.hartwig.pipeline.execution.vm.VmDirectories;

public class CollectGridssMetrics implements BashCommand {
    private String inputFile;

    public CollectGridssMetrics(String inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public String asBash() {
        return GridssCommon.gridssCommand("gridss.analysis.CollectGridssMetrics", "256M",
                new GridssArguments()
                        .add("tmp_dir", "/tmp")
                        .add("assume_sorted", "true")
                        .add("i", inputFile)
                        .add("o", metrics())
                        .add("threshold_coverage", "50000")
                        .add("file_extension", "null")
                        .add("gridss_program", "null")
                        .add("gridss_program", "CollectCigarMetrics")
                        .add("gridss_program", "CollectMapqMetrics")
                        .add("gridss_program", "CollectTagMetrics")
                        .add("gridss_program", "CollectIdsvMetrics")
                        .add("gridss_program", "ReportThresholdCoverage")
                        .add("program", "null")
                        .add("program", "CollectInsertSizeMetrics")
                        .asBash())
                .asBash();
    }

    public String metrics() {
        return VmDirectories.outputFile("collect_gridss.metrics");
    }
}
