package com.hartwig.pipeline.io;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import com.hartwig.pipeline.Arguments;
import com.hartwig.pipeline.alignment.Run;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

public class RuntimeBucket {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeBucket.class);

    private final Storage storage;
    private final Bucket bucket;
    private final String namespace;
    private final Run run;

    public static RuntimeBucket from(final Storage storage, final String namespace, final String sampleName, final Arguments arguments) {
        return createBucketIfNeeded(storage, namespace, arguments, Run.from(sampleName, arguments));
    }

    public static RuntimeBucket from(final Storage storage, final String namespace, final String referenceSampleName,
            final String tumorSampleName, final Arguments arguments) {
        return createBucketIfNeeded(storage, namespace, arguments, Run.from(referenceSampleName, tumorSampleName, arguments));
    }

    @NotNull
    private synchronized static RuntimeBucket createBucketIfNeeded(final Storage storage, final String namespace, final Arguments arguments,
            final Run run) {
        Bucket bucket = storage.get(run.id());
        if (bucket == null) {
            LOGGER.info("Creating runtime bucket [{}] in Google Storage", run.id());
            bucket = storage.create(BucketInfo.newBuilder(run.id())
                    .setStorageClass(StorageClass.REGIONAL)
                    .setLocation(arguments.region())
                    .build());
            LOGGER.info("Creating runtime bucket complete");
        }
        return new RuntimeBucket(storage, bucket, namespace, run);
    }

    public String getNamespace() {
        return namespace;
    }

    public Blob get(String blobName) {
        return bucket.get(namespace(blobName));
    }

    @NotNull
    private String namespace(final String blobName) {
        return namespace + (blobName.startsWith("/") ? blobName : ("/" + blobName));
    }

    public void create(String blobName, byte[] content) {
        bucket.create(namespace(blobName), content);
    }

    public void create(String blobName, InputStream content) {
        bucket.create(namespace(blobName), content);
    }

    public Page<Blob> list() {
        return bucket.list(Storage.BlobListOption.prefix(namespace));
    }

    public Page<Blob> list(String prefix) {
        return bucket.list(Storage.BlobListOption.prefix(namespace(prefix)));
    }

    public void copyInto(String sourceBucket, String sourceBlobName, String targetBlobName) {
        BlobInfo targetBlobInfo = BlobInfo.newBuilder(bucket.getName(), namespace(targetBlobName)).build();
        storage.copy(Storage.CopyRequest.of(sourceBucket, sourceBlobName, targetBlobInfo));
    }

    void compose(List<String> sources, String target) {
        storage.compose(Storage.ComposeRequest.of(bucket.getName(), sources, namespace(target)));
    }

    private RuntimeBucket(final Storage storage, final Bucket bucket, final String namespace, final Run run) {
        this.storage = storage;
        this.bucket = bucket;
        this.namespace = namespace;
        this.run = run;
    }

    public String name() {
        return bucket.getName() + "/" + namespace;
    }

    public String runId() {
        return run.id();
    }

    @Override
    public String toString() {
        return String.format("runtime bucket [%s]", name());
    }
}
