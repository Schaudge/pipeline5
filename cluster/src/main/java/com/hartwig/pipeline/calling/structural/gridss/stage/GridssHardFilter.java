package com.hartwig.pipeline.calling.structural.gridss.stage;

import java.util.Collections;
import java.util.List;

import com.hartwig.pipeline.calling.SubStage;
import com.hartwig.pipeline.calling.structural.gridss.command.GripssHardFilterCommand;
import com.hartwig.pipeline.execution.vm.BashCommand;
import com.hartwig.pipeline.execution.vm.OutputFile;

public class GridssHardFilter extends SubStage {

    public static final String GRIDSS_SOMATIC_FILTERED = "gripss.somatic.filtered";

    public GridssHardFilter() {
        super(GRIDSS_SOMATIC_FILTERED, OutputFile.GZIPPED_VCF);
    }

    @Override
    public List<BashCommand> bash(final OutputFile input, final OutputFile output) {
        return Collections.singletonList(new GripssHardFilterCommand(input.path(), output.path()));
    }
}
