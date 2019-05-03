package com.hartwig.pipeline.execution.dataproc;

import com.hartwig.patient.Sample;
import com.hartwig.pipeline.Arguments;
import com.hartwig.pipeline.BamCreationPipeline;
import com.hartwig.pipeline.execution.JobDefinition;
import com.hartwig.pipeline.io.NamespacedResults;
import com.hartwig.pipeline.io.RuntimeBucket;

import org.immutables.value.Value;

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
            DataprocPerformanceProfile profile, NamespacedResults namespacedResults) {
        return ImmutableSparkJobDefinition.builder()
                .name("BamCreation")
                .mainClass(BAM_CREATION_MAIN)
                .jarLocation(jarLocation.uri())
                .addArguments(arguments.version(), runtimeBucket.name(), arguments.project(), namespacedResults.path())
                .sparkProperties(SparkProperties.asMap(profile))
                .performanceProfile(profile)
                .build();
    }

    static SparkJobDefinition sortAndIndex(JarLocation jarLocation, Arguments arguments, RuntimeBucket runtimeBucket, Sample sample,
            NamespacedResults namespacedResults) {
        DataprocPerformanceProfile performanceProfile = DataprocPerformanceProfile.mini();
        return ImmutableSparkJobDefinition.builder()
                .name("SortAndIndex")
                .mainClass(SORT_INDEX_MAIN)
                .jarLocation(jarLocation.uri())
                .addArguments(arguments.version(), runtimeBucket.name(), arguments.project(), sample.name(), namespacedResults.path())
                .sparkProperties(SparkProperties.asMap(performanceProfile))
                .performanceProfile(performanceProfile)
                .build();
    }

    static SparkJobDefinition sortAndIndexRecalibrated(JarLocation jarLocation, Arguments arguments, RuntimeBucket runtimeBucket,
            Sample sample, NamespacedResults namespacedResults) {
        DataprocPerformanceProfile performanceProfile = DataprocPerformanceProfile.mini();
        return ImmutableSparkJobDefinition.builder()
                .name("RecalibratedSortAndIndex")
                .mainClass(SORT_INDEX_MAIN)
                .jarLocation(jarLocation.uri())
                .addArguments(arguments.version(),
                        runtimeBucket.name(),
                        arguments.project(),
                        sample.name() + "." + BamCreationPipeline.RECALIBRATED_SUFFIX,
                        namespacedResults.path(""))
                .sparkProperties(SparkProperties.asMap(performanceProfile))
                .performanceProfile(performanceProfile)
                .build();
    }

    static SparkJobDefinition gunzip(JarLocation jarLocation) {
        DataprocPerformanceProfile performanceProfile = DataprocPerformanceProfile.mini();
        return ImmutableSparkJobDefinition.builder()
                .name("Gunzip")
                .mainClass(GUNZIP_MAIN)
                .jarLocation(jarLocation.uri())
                .sparkProperties(SparkProperties.asMap(performanceProfile))
                .performanceProfile(performanceProfile)
                .build();
    }

    static SparkJobDefinition tool(JarLocation jarLocation, String mainClass) {
        DataprocPerformanceProfile performanceProfile = DataprocPerformanceProfile.mini();
        return ImmutableSparkJobDefinition.builder()
                .name("Tool")
                .mainClass(mainClass)
                .jarLocation(jarLocation.uri())
                .sparkProperties(SparkProperties.asMap(performanceProfile))
                .performanceProfile(performanceProfile)
                .build();
    }
}
