package com.hartwig.pipeline.alignment;

import java.util.Optional;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.hartwig.patient.Sample;
import com.hartwig.pipeline.Arguments;
import com.hartwig.pipeline.io.GoogleStorageLocation;
import com.hartwig.pipeline.io.NamespacedResults;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlignmentOutputStorage {

    private final Logger LOGGER = LoggerFactory.getLogger(AlignmentOutputStorage.class);

    private final Storage storage;
    private final Arguments arguments;
    private final NamespacedResults namespacedResults;

    public AlignmentOutputStorage(final Storage storage, final Arguments arguments, final NamespacedResults namespacedResults) {
        this.storage = storage;
        this.arguments = arguments;
        this.namespacedResults = namespacedResults;
    }

    public Optional<AlignmentOutput> get(final Sample sample) {
        Bucket bucket = storage.get(Run.from(sample.name(), arguments).id());
        if (bucket != null) {
            String sorted = AlignmentOutputPaths.sorted(sample);
            Blob bamBlob = bucket.get(namespacedResults.path(sorted));
            Blob baiBlob = bucket.get(namespacedResults.path(AlignmentOutputPaths.bai(sorted)));
            String recalibrated = AlignmentOutputPaths.recalibrated(sample);
            Blob recalibratedBamBlob = bucket.get(namespacedResults.path(recalibrated));
            Blob recalibratedBaiBlob = bucket.get(namespacedResults.path(AlignmentOutputPaths.bai(recalibrated)));
            if (recalibratedBamBlob != null) {

                if (bamBlob == null) {
                    throw new IllegalStateException(String.format("Recalibrated bam in bucket [%s] but the sorted bam was missing. "
                            + "This output is corrupted and cannot be used for downstream processing", bucket));
                }

                return Optional.of(AlignmentOutput.of(location(bucket, bamBlob),
                        location(bucket, baiBlob),
                        location(bucket, recalibratedBamBlob),
                        location(bucket, recalibratedBaiBlob),
                        sample));
            } else {
                LOGGER.info("No recalibrated BAM found in bucket [{}]. Alignment stage is likely not yet complete.", bucket);
            }
        }
        return Optional.empty();
    }

    @NotNull
    private GoogleStorageLocation location(final Bucket bucket, final Blob blob) {
        return GoogleStorageLocation.of(bucket.getName(), blob.getName());
    }
}
