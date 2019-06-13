package com.hartwig.pipeline.tertiary.amber;

import com.hartwig.pipeline.execution.vm.Bash;
import com.hartwig.pipeline.tertiary.HmfToolCommand;
import com.hartwig.pipeline.tools.Versions;

class AmberApplicationCommand extends HmfToolCommand {
    AmberApplicationCommand(String referenceSampleName, String referenceBamPath, String tumorSampleName, String tumorBamPath,
            String referenceGenomePath, String bedPath, String snpBedPath) {
        super("amber",
                Versions.AMBER,
                "amber.jar",
                "com.hartwig.hmftools.amber.AmberApplication",
                "32G",
                referenceSampleName,
                referenceBamPath,
                tumorSampleName,
                tumorBamPath,
                "-ref_genome",
                referenceGenomePath,
                "-bed",
                bedPath,
                "-snp_bed",
                snpBedPath,
                "-threads",
                Bash.allCpus());
    }
}
