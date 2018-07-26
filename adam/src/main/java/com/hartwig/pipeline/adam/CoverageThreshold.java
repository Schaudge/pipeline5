package com.hartwig.pipeline.adam;

import java.io.Serializable;

import org.immutables.value.Value;

@Value.Immutable
public interface CoverageThreshold extends Serializable {

    @Value.Parameter
    long coverage();

    @Value.Parameter
    double minimumPercentage();

    static CoverageThreshold of(long coverage, double minimumPercentage) {
        return com.hartwig.pipeline.adam.ImmutableCoverageThreshold.of(coverage, minimumPercentage);
    }
}
