package com.hartwig.pipeline.calling.structural.gridss.process;

import com.hartwig.pipeline.execution.vm.BashCommand;
import com.hartwig.pipeline.execution.vm.VmDirectories;

public class AnnotateVariants implements BashCommand {
    private final String sampleBam;
    private final String tumorBam;
    private final String assemblyBam;
    private final String inputVcf;
    private final String referenceGenome;

    public AnnotateVariants(String sampleBam, String tumorBam, String assemblyBam, String inputVcf, String referenceGenome) {
        this.sampleBam = sampleBam;
        this.tumorBam = tumorBam;
        this.assemblyBam = assemblyBam;
        this.inputVcf = inputVcf;
        this.referenceGenome = referenceGenome;
    }

    public String resultantVcf() {
        return VmDirectories.outputFile("annotate_variants.vcf");
    }

    @Override
    public String asBash() {
        return GridssCommon.gridssCommand("gridss.AnnotateVariants", "8G", new GridssArguments()
                .add("tmp_dir", "/tmp")
                .add("working_dir", VmDirectories.OUTPUT)
                .add("reference_sequence", referenceGenome)
                .add("input", sampleBam)
                .add("input", tumorBam)
                .add("input_vcf", inputVcf)
                .add("output_vcf", resultantVcf())
                .add("assembly", assemblyBam)
                .add("worker_threads", "2")
                .add("blacklist", GridssCommon.blacklist())
                .add("configuration_file", GridssCommon.configFile())
                .asBash()
        ).asBash();
    }
}
