package com.hartwig.pipeline.io;

import com.hartwig.patient.ImmutableSample;
import com.hartwig.patient.Sample;
import com.hartwig.pipeline.execution.JobStatus;
import com.hartwig.pipeline.io.sbp.SBPS3FileTarget;
import com.hartwig.pipeline.testsupport.MockRuntimeBucket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class CloudBamDownloadTest {

    private static final String SAMPLE_NAME = "TEST123";
    private static final ImmutableSample SAMPLE = Sample.builder("", SAMPLE_NAME).barcode("FR123").build();
    private CloudCopy cloudCopy;
    private CloudBamDownload victim;
    private MockRuntimeBucket runtimeBucket;

    @Before
    public void setUp() {
        cloudCopy = mock(CloudCopy.class);
        victim = new CloudBamDownload(SBPS3FileTarget::from, ResultsDirectory.defaultDirectory(), cloudCopy);
        runtimeBucket = MockRuntimeBucket.of("run");
    }

    @Test(expected = RuntimeException.class)
    public void rethrowsExceptionsAsRuntime() {
        doThrow(new IOException()).when(cloudCopy).copy(anyString(), anyString());
        victim.run(SAMPLE, runtimeBucket.getRuntimeBucket(), JobStatus.SUCCESS);
    }

    @Test
    public void copiesBamAndBaiToTargetLocation() {
        ArgumentCaptor<String> sourceCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> targetCaptor = ArgumentCaptor.forClass(String.class);
        victim.run(SAMPLE, runtimeBucket.getRuntimeBucket(), JobStatus.SUCCESS);
        verify(cloudCopy, times(2)).copy(sourceCaptor.capture(), targetCaptor.capture());
        assertThat(sourceCaptor.getAllValues().get(0)).isEqualTo("gs://run/results/TEST123.sorted.bam");
        assertThat(sourceCaptor.getAllValues().get(1)).isEqualTo("gs://run/results/TEST123.sorted.bam.bai");
        assertThat(targetCaptor.getAllValues().get(0)).isEqualTo("s3://hmf-bam-storage/FR123/TEST123.bam");
        assertThat(targetCaptor.getAllValues().get(1)).isEqualTo("s3://hmf-bam-storage/FR123/TEST123.bam.bai");
    }
}