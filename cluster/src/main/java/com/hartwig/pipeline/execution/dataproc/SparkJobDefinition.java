package com.hartwig.pipeline.execution.dataproc;

import com.hartwig.patient.Sample;
import com.hartwig.pipeline.Arguments;
import com.hartwig.pipeline.BamCreationPipeline;
import com.hartwig.pipeline.execution.JobDefinition;
import com.hartwig.pipeline.io.ResultsDirectory;
import com.hartwig.pipeline.io.RuntimeBucket;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Value.Immutable
public interface SparkJobDefinition extends JobDefinition<DataprocPerformanceProfile> {

    String GUNZIP_MAIN = "com.hartwig.pipeline.runtime.GoogleCloudGunzip";
    String BAM_CREATION_MAIN = "com.hartwig.pipeline.runtime.GoogleCloudPipelineRuntime";
    String SORT_INDEX_MAIN = "com.hartwig.pipeline.runtime.GoogleCloudSortAndIndex";

    String mainClass();

    String jarLocation();

    List<String> arguments();

    Map<String, String> sparkProperties();

    static SparkJobDefinition bamCreation(JarLocation jarLocation, Arguments arguments, RuntimeBucket runtimeBucket,
            DataprocPerformanceProfile profile) {
        return ImmutableSparkJobDefinition.builder()
                .name("BamCreation")
                .mainClass(BAM_CREATION_MAIN)
                .jarLocation(jarLocation.uri())
                .addArguments(arguments.version(), runtimeBucket.name(), arguments.project(), runtimeBucket.getNamespace())
                .sparkProperties(SparkProperties.asMap(profile))
                .performanceProfile(profile)
                .build();
    }

    static SparkJobDefinition sortAndIndex(JarLocation jarLocation, Arguments arguments, RuntimeBucket runtimeBucket, Sample sample,
            ResultsDirectory resultsDirectory) {
        DataprocPerformanceProfile performanceProfile = DataprocPerformanceProfile.singleNode();
        return ImmutableSparkJobDefinition.builder()
                .name("SortAndIndex")
                .mainClass(SORT_INDEX_MAIN)
                .jarLocation(jarLocation.uri())
                .addArguments(arguments.version(),
                        runtimeBucket.name(),
                        arguments.project(),
                        sample.name(),
                        resultsPath(runtimeBucket, resultsDirectory))
                .sparkProperties(SparkProperties.asMap(performanceProfile))
                .performanceProfile(performanceProfile)
                .build();
    }

    static SparkJobDefinition sortAndIndexRecalibrated(JarLocation jarLocation, Arguments arguments, RuntimeBucket runtimeBucket,
            Sample sample, ResultsDirectory resultsDirectory) {
        DataprocPerformanceProfile performanceProfile = DataprocPerformanceProfile.singleNode();
        return ImmutableSparkJobDefinition.builder()
                .name("RecalibratedSortAndIndex")
                .mainClass(SORT_INDEX_MAIN)
                .jarLocation(jarLocation.uri())
                .addArguments(arguments.version(),
                        runtimeBucket.name(),
                        arguments.project(),
                        sample.name() + "." + BamCreationPipeline.RECALIBRATED_SUFFIX,
                        resultsPath(runtimeBucket, resultsDirectory))
                .sparkProperties(SparkProperties.asMap(performanceProfile))
                .performanceProfile(performanceProfile)
                .build();
    }

    @NotNull
    static String resultsPath(final RuntimeBucket runtimeBucket, final ResultsDirectory resultsDirectory) {
        return runtimeBucket.getNamespace() + "/" + resultsDirectory.path();
    }

    static SparkJobDefinition gunzip(JarLocation jarLocation, RuntimeBucket runtimeBucket) {
        DataprocPerformanceProfile performanceProfile = DataprocPerformanceProfile.mini();
        return ImmutableSparkJobDefinition.builder()
                .name("Gunzip")
                .mainClass(GUNZIP_MAIN)
                .jarLocation(jarLocation.uri())
                .sparkProperties(SparkProperties.asMap(performanceProfile))
                .performanceProfile(performanceProfile)
                .addArguments(runtimeBucket.getNamespace())
                .build();
    }
}
