package com.hartwig.pipeline.runtime.configuration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.immutables.value.Value;

@JsonDeserialize(as = ImmutablePatientParameters.class)
@Value.Immutable
public interface PatientParameters {

    @Value.Default
    default String directory() {
        return "/patients";
    }

    @Value.Default
    default String name() {
        return "";
    }
}
