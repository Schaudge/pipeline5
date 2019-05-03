package com.hartwig.pipeline.io;

import java.util.function.Function;

import com.hartwig.patient.Sample;
import com.hartwig.pipeline.execution.JobStatus;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudBamDownload implements BamDownload {

    private static final Logger LOGGER = LoggerFactory.getLogger(BamComposer.class);

    private final Function<Sample, String> targetResolver;
    private final NamespacedResults namespacedResults;
    private final CloudCopy cloudCopy;

    public CloudBamDownload(final Function<Sample, String> targetResolver, final NamespacedResults namespacedResults,
            final CloudCopy cloudCopy) {
        this.targetResolver = targetResolver;
        this.namespacedResults = namespacedResults;
        this.cloudCopy = cloudCopy;
    }

    @Override
    public void run(final Sample sample, final RuntimeBucket runtimeBucket, final JobStatus result) {
        try {
            String bamPath = String.format("gs://%s/%s%s.sorted.bam", runtimeBucket.name(), namespacedResults.path(""), sample.name());
            String targetBam = targetResolver.apply(sample);
            cloudCopy.copy(bamPath, targetBam);
            cloudCopy.copy(bai(bamPath), bai(targetBam));
            LOGGER.info("Downloaded BAM (and BAI) from {} to {}", bamPath, targetBam);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private String bai(final String path) {
        return path + ".bai";
    }
}
