package com.hartwig.pipeline.calling.command;

import com.hartwig.pipeline.tools.Versions;

public class BgzipCommand extends VersionedToolCommand {

    public BgzipCommand(String vcf) {
        super("tabix", "bgzip", Versions.TABIX, "-f", vcf);
    }
}
