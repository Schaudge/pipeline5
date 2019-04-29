package com.hartwig.pipeline.calling.somatic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CosmicAnnotationTest extends SubStageTest {

    @Override
    SubStage createVictim() {
        return new CosmicAnnotation("cosmic.vcf.gz");
    }

    @Override
    String expectedPath() {
        return "/data/output/tumor.cosmic.annotated.vcf.gz";
    }

    @Test
    public void runsBcfToolsDbSnpAnnotation() {
        assertThat(output.currentBash().asUnixString()).contains(
                "/data/tools/bcftools/1.3.1/bcftools annotate -a cosmic.vcf.gz -c ID,INFO "
                        + "-o /data/output/tumor.cosmic.annotated.vcf.gz -O z /data/output/tumor.strelka.vcf");
    }

    @Test
    public void runsTabix() {
        assertThat(output.currentBash().asUnixString()).contains(
                "/data/tools/tabix/0.2.6/tabix /data/output/tumor.cosmic.annotated.vcf.gz -p vcf ");
    }

}