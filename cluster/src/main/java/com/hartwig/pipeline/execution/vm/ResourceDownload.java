package com.hartwig.pipeline.execution.vm;

import static java.util.stream.Stream.of;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.storage.Storage;
import com.hartwig.pipeline.io.RuntimeBucket;
import com.hartwig.pipeline.resource.Resource;
import com.hartwig.pipeline.resource.ResourceLocation;

public class ResourceDownload implements BashCommand {

    private static final String RESOURCES_PATH = "/data/resources";
    private final ResourceLocation resourceLocation;
    private final RuntimeBucket runtimeBucket;

    public ResourceDownload(final ResourceLocation resourceLocation, final RuntimeBucket runtimeBucket) {
        this.resourceLocation = resourceLocation;
        this.runtimeBucket = runtimeBucket;
    }

    @Override
    public String asBash() {
        return String.format("gsutil -m cp gs://%s/%s/* %s", runtimeBucket.name(), resourceLocation.bucket(), RESOURCES_PATH);
    }

    List<String> getLocalPaths() {
        return resourceLocation.files().stream().map(this::fileName).map(file -> RESOURCES_PATH + "/" + file).collect(Collectors.toList());
    }

    public String find(String... extensions) {
        return getLocalPaths().stream()
                .filter(file -> of(extensions).anyMatch(file::endsWith))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "No file with extension(s) %s was found in resource location [%s]",
                        Arrays.toString(extensions),
                        resourceLocation)));
    }

    private String fileName(final String path) {
        String[] pathSplit = path.split("/");
        return pathSplit[pathSplit.length - 1];
    }

    public static ResourceDownload from(final Storage storage, final String resource, final RuntimeBucket runtimeBucket) {
        return from(runtimeBucket, new Resource(storage, resource, resource));
    }

    public static ResourceDownload from(final RuntimeBucket runtimeBucket, final Resource resource) {
        return new ResourceDownload(resource.copyInto(runtimeBucket), runtimeBucket);
    }
}
