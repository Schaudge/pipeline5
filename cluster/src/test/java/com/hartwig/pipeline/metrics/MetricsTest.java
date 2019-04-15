package com.hartwig.pipeline.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hartwig.pipeline.cost.CostCalculator;
import com.hartwig.pipeline.execution.dataproc.DataprocPerformanceProfile;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class MetricsTest {

    private static final double COST = 100.0;
    private static final int ONE_HOUR = 3600000;
    private static final String METRIC_NAME = "metric";
    private ArgumentCaptor<Metric> metricArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        metricArgumentCaptor = ArgumentCaptor.forClass(Metric.class);
        final Monitor monitor = mock(Monitor.class);
        final CostCalculator costCalculator = mock(CostCalculator.class);
        final DataprocPerformanceProfile profile = DataprocPerformanceProfile.builder().fastQSizeGB(10.0).build();
        when(costCalculator.calculate(eq(profile), eq(1.0))).thenReturn(COST);
        Metrics victim = new Metrics(monitor, costCalculator);
        victim.record(METRIC_NAME, profile, ONE_HOUR);
        verify(monitor, times(4)).update(metricArgumentCaptor.capture());
    }

    @Test
    public void recordsTotalRuntimeInMillis() {
        Metric runtime = metricArgumentCaptor.getAllValues().get(0);
        assertThat(runtime.name()).isEqualTo(Metrics.BOOTSTRAP + "_" + METRIC_NAME + "_SPENT_TIME");
        assertThat(runtime.value()).isEqualTo(ONE_HOUR);
    }

    @Test
    public void recordsCost() {
        Metric runtime = metricArgumentCaptor.getAllValues().get(1);
        assertThat(runtime.name()).isEqualTo(Metrics.COST);
        assertThat(runtime.value()).isEqualTo(COST);
    }

    @Test
    public void recordsFastQSizeGB() {
        Metric runtime = metricArgumentCaptor.getAllValues().get(2);
        assertThat(runtime.name()).isEqualTo(Metrics.FASTQ_SIZE_GB);
        assertThat(runtime.value()).isEqualTo(10.0);
    }

    @Test
    public void recordsCostPerGB() {
        Metric runtime = metricArgumentCaptor.getAllValues().get(3);
        assertThat(runtime.name()).isEqualTo(Metrics.COST_PER_GB);
        assertThat(runtime.value()).isEqualTo(10.0);
    }
}