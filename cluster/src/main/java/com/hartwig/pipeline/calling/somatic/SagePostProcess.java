package com.hartwig.pipeline.calling.somatic;

import java.util.List;

import com.google.api.client.util.Lists;
import com.hartwig.pipeline.calling.SubStage;
import com.hartwig.pipeline.calling.SubStageInputOutput;
import com.hartwig.pipeline.calling.substages.SnpEff;
import com.hartwig.pipeline.execution.vm.BashCommand;
import com.hartwig.pipeline.execution.vm.OutputFile;
import com.hartwig.pipeline.resource.RefGenomeVersion;
import com.hartwig.pipeline.resource.ResourceFiles;

public class SagePostProcess extends SubStage {

    private final ResourceFiles resourceFiles;
    private final String assembly;
    private final SubStageInputOutput tumorSampleName;

    public SagePostProcess(final String tumorSampleName, final ResourceFiles resourceFiles) {
        super("sage.somatic.filtered", OutputFile.GZIPPED_VCF);
        this.tumorSampleName = SubStageInputOutput.empty(tumorSampleName);
        this.resourceFiles = resourceFiles;
        this.assembly = resourceFiles.version() == RefGenomeVersion.HG19 ? "hg19" : "hg38";
    }

    @Override
    public List<BashCommand> bash(final OutputFile input, final OutputFile output) {
        final List<BashCommand> result = Lists.newArrayList();

        SubStage passFilter = new PassFilter();
        SubStage mappabilityAnnotation =
                new MappabilityAnnotation(resourceFiles.out150Mappability(), resourceFiles.mappabilityHDR());
        SubStage ponAnnotation = new PonAnnotation("sage.pon", resourceFiles.sageGermlinePon(), "PON_COUNT", "PON_MAX");
        SubStage ponFilter = new PonFilter(resourceFiles.version());
        SubStage snpEff = new SnpEff(resourceFiles);

        OutputFile passFilterFile = passFilter.apply(tumorSampleName).outputFile();
        OutputFile mappabilityAnnotationFile = mappabilityAnnotation.apply(tumorSampleName).outputFile();
        OutputFile ponAnnotationFile = ponAnnotation.apply(tumorSampleName).outputFile();
        OutputFile ponFilterFile = ponFilter.apply(tumorSampleName).outputFile();
        OutputFile snpEffFile = snpEff.apply(tumorSampleName).outputFile();

        result.addAll(passFilter.bash(input, passFilterFile));
        result.addAll(mappabilityAnnotation.bash(passFilterFile, mappabilityAnnotationFile));
        result.addAll(ponAnnotation.bash(mappabilityAnnotationFile, ponAnnotationFile));
        result.addAll(ponFilter.bash(ponAnnotationFile, ponFilterFile));
        result.addAll(snpEff.bash(ponFilterFile, snpEffFile));
        result.add(new SagePostProcessCommand(assembly, snpEffFile.path(), output.path()));
        return result;
    }
}
