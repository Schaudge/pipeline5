package com.hartwig.pipeline.io.sources;

import java.util.stream.StreamSupport;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.collect.Iterables;
import com.hartwig.patient.Sample;
import com.hartwig.pipeline.bootstrap.Arguments;
import com.hartwig.pipeline.io.RuntimeBucket;

public class GoogleStorageSampleSource implements SampleSource {

    private final Storage storage;

    public GoogleStorageSampleSource(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public SampleData sample(final Arguments arguments) {

        if (arguments.patientId() == null || arguments.patientId().isEmpty()) {
            throw new IllegalArgumentException("Unable to run in -no_upload mode without an explicit patient/sample name (use -p)");
        }

        RuntimeBucket runtimeBucket = RuntimeBucket.from(storage, arguments.patientId(), arguments);

        Iterable<Blob> blobs = runtimeBucket.bucket().list(Storage.BlobListOption.prefix("samples/")).iterateAll();
        if (Iterables.isEmpty(blobs)) {
            throw new IllegalArgumentException(String.format("No sample data found in bucket [%s] so there is no input to process. "
                    + "You cannot use the no_upload flag if no sample has already been uploaded", runtimeBucket.getName()));
        }
        long factor = blobs.iterator().next().getName().endsWith("gz") ? 1 : 4;
        long fileSize = StreamSupport.stream(blobs.spliterator(), false).mapToLong(BlobInfo::getSize).sum() / factor;
        return SampleData.of(Sample.builder("", arguments.patientId()).build(), fileSize);
    }
}
