package com.hartwig.pipeline.reruns;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hartwig.pipeline.datatypes.DataType;
import com.hartwig.pipeline.sbpapi.SbpRestApi;
import com.hartwig.pipeline.storage.GoogleStorageLocation;

public class ApiPersistedDataset implements PersistedDataset {

    private final Map<String, Map<String, Map<String, String>>> datasetMap;
    private final String billingProject;

    public ApiPersistedDataset(final SbpRestApi restApi, final ObjectMapper objectMapper, final String biopsyName,
            final String billingProject) {
        this.billingProject = billingProject;
        try {
            String dataset = restApi.getDataset(biopsyName);
            this.datasetMap = deserialize(objectMapper, dataset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Map<String, Map<String, String>>> deserialize(final ObjectMapper objectMapper, final String dataset)
            throws com.fasterxml.jackson.core.JsonProcessingException {
        ObjectNode objectNode = (ObjectNode) objectMapper.readTree(dataset);
        objectNode.remove("rna_raw_reads");
        return objectMapper.convertValue(objectNode, new TypeReference<>() {
        });
    }

    @Override
    public Optional<GoogleStorageLocation> path(final String sample, final DataType dataType) {
        return ofNullable(datasetMap.get(dataType.toString().toLowerCase())).map(d -> d.get(sample))
                .map(d -> d.get("path"))
                .map(p -> GoogleStorageLocation.from(p, billingProject));
    }
}
