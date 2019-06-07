package com.hartwig.pipeline.trace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StageTrace {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageTrace.class);

    public enum ExecutorType{
        DATAPROC, COMPUTE_ENGINE
    }

    private final String stageName;
    private final ExecutorType executorType;

    public StageTrace(final String stageName, final ExecutorType executorType) {
        this.stageName = stageName.toUpperCase();
        this.executorType = executorType;
    }

    public StageTrace start(){
        LOGGER.info("Stage [{}] starting and will be run on [{}]", stageName, executorType);
        return this;
    }

    public void stop(){
        LOGGER.info("Stage [{}] complete", stageName);
    }
}
