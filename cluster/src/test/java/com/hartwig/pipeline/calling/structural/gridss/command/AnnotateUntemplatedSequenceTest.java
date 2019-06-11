package com.hartwig.pipeline.calling.structural.gridss.command;

import com.hartwig.pipeline.calling.structural.gridss.CommonEntities;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotateUntemplatedSequenceTest implements CommonEntities {
    private String className;
    private static final String OUTFILE = String.format("%s/annotated.vcf", OUT_DIR);
    private AnnotateUntemplatedSequence command;
    private String inputVcf;

    @Before
    public void setup() {
        className = "gridss.AnnotateUntemplatedSequence";
        inputVcf = "/intermediate.vcf";
        command = new AnnotateUntemplatedSequence(inputVcf, REFERENCE_GENOME);
    }

    @Test
    public void shouldReturnClassName() {
        assertThat(command.className()).isEqualTo(className);
    }

    @Test
    public void shouldUseStandardGridssHeapSize() {
        GridssCommonArgumentsAssert.assertThat(command).usesStandardAmountOfMemory();
    }

    @Test
    public void shouldConstructGridssOptions() {
        GridssCommonArgumentsAssert.assertThat(command)
                .hasGridssArguments(ARGS_REFERENCE_SEQUENCE)
                .and(ARG_KEY_INPUT, inputVcf)
                .and(ARG_KEY_OUTPUT, command.resultantVcf())
                .and("aligner_command_line", "null")
                .and("aligner_command_line", PATH_TO_BWA)
                .and("aligner_command_line", "mem")
                .and("'aligner_command_line", "-K 40000000'")
                .and("aligner_command_line", "-t")
                .and("'aligner_command_line", "%3$d'")
                .and("'aligner_command_line", "%2$s'")
                .and("'aligner_command_line", "%1$s'")
                .andNoMore();
    }

    @Test
    public void shouldReturnResultantVcf() {
        assertThat(command.resultantVcf()).isNotNull();
        assertThat(command.resultantVcf()).isEqualTo(OUTFILE);
    }
}
