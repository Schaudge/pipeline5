package com.hartwig.pipeline.performance;

import com.hartwig.patient.Sample;
import com.hartwig.pipeline.io.sources.SampleData;

public class ClusterOptimizer {

    private final CpuFastQSizeRatio cpuToFastQSizeRatio;
    private final boolean usePreemtibleVms;

    public ClusterOptimizer(final CpuFastQSizeRatio cpuToFastQSizeRatio, final boolean usePreemtibleVms) {
        this.cpuToFastQSizeRatio = cpuToFastQSizeRatio;
        this.usePreemtibleVms = usePreemtibleVms;
    }

    public PerformanceProfile optimize(SampleData sampleData) {
        Sample sample = sampleData.sample();
        if (sampleData.sizeInBytes() <= 0) {
            throw new IllegalArgumentException(String.format("Sample [%s] lanes had no data. Cannot calculate data size or cpu requirements",
                    sample.name()));
        }
        long totalFileSizeGB = (long) (sampleData.sizeInBytes() / 1e9);
        double totalCpusRequired = totalFileSizeGB * cpuToFastQSizeRatio.cpusPerGB();
        MachineType defaultWorker = MachineType.defaultWorker();
        int numWorkers = new Double(totalCpusRequired / defaultWorker.cpus()).intValue();
        int numPreemptible = usePreemtibleVms ? numWorkers / 2 : 0;
        return PerformanceProfile.builder()
                .master(MachineType.defaultMaster())
                .primaryWorkers(defaultWorker)
                .preemtibleWorkers(MachineType.defaultPreemtibleWorker())
                .numPrimaryWorkers(Math.max(2, numWorkers - numPreemptible))
                .numPreemtibleWorkers(numPreemptible)
                .fastQSizeGB(totalFileSizeGB)
                .build();
    }
}
