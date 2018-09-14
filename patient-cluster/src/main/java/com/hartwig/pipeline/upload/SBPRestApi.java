package com.hartwig.pipeline.upload;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.hartwig.pipeline.bootstrap.Arguments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SBPRestApi {

    private final static Logger LOGGER = LoggerFactory.getLogger(SBPRestApi.class);
    private final WebTarget target;

    private SBPRestApi(final WebTarget target) {
        this.target = target;
    }

    String getFastQ(int sampleId) {
        LOGGER.info("Connecting to SBP at [{}] for sample id [{}]", target.getUri(), sampleId);
        Response response = target.path("hmf").path("v1").path("fastq").queryParam("sample_id", sampleId).request().buildGet().invoke();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        }
        throw new RuntimeException(String.format("Received an error status of [%s] from SBP Api at [%s]",
                response.getStatus(),
                target.getUri()));
    }

    public static SBPRestApi newInstance(Arguments arguments) {
        return new SBPRestApi(ClientBuilder.newClient().target(arguments.sbpApiUrl()));
    }
}
