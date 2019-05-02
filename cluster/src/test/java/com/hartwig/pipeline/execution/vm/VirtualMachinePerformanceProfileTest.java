package com.hartwig.pipeline.execution.vm;

import org.junit.Test;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class VirtualMachinePerformanceProfileTest {
    @Test
    public void shouldSetUriForCustomProfile() {
        int cores = 16;
        int memoryGb = 32;
        int memoryMb = memoryGb * 1024;
        assertThat(VirtualMachinePerformanceProfile.custom(cores, memoryGb).uri())
                .isEqualTo(format("custom-%d-%d", cores, memoryMb));
    }

    @Test
    public void shouldDefaultDiskSizeInGb() {
        assertThat(VirtualMachinePerformanceProfile.defaultVm().diskGb()).isEqualTo(1000);
    }
}