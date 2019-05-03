package com.hartwig.pipeline.tertiary.amber;

import com.google.cloud.storage.Storage;
import com.hartwig.pipeline.Arguments;
import com.hartwig.pipeline.alignment.AlignmentPair;
import com.hartwig.pipeline.execution.vm.OutputFile;
import com.hartwig.pipeline.execution.JobStatus;
import com.hartwig.pipeline.execution.vm.BashStartupScript;
import com.hartwig.pipeline.execution.vm.ComputeEngine;
import com.hartwig.pipeline.execution.vm.InputDownload;
import com.hartwig.pipeline.execution.vm.OutputUpload;
import com.hartwig.pipeline.execution.vm.ResourceDownload;
import com.hartwig.pipeline.execution.vm.VirtualMachineJobDefinition;
import com.hartwig.pipeline.io.GoogleStorageLocation;
import com.hartwig.pipeline.io.NamespacedResults;
import com.hartwig.pipeline.io.RuntimeBucket;

public class Amber {

    public static final String RESULTS_NAMESPACE = "amber";
    private final Arguments arguments;
    private final ComputeEngine computeEngine;
    private final Storage storage;
    private final NamespacedResults namespacedResults;

    Amber(final Arguments arguments, final ComputeEngine computeEngine, final Storage storage, final NamespacedResults namespacedResults) {
        this.arguments = arguments;
        this.computeEngine = computeEngine;
        this.storage = storage;
        this.namespacedResults = namespacedResults;
    }

    public AmberOutput run(AlignmentPair pair) {
        String tumorSampleName = pair.tumor().sample().name();
        String referenceSampleName = pair.reference().sample().name();
        RuntimeBucket runtimeBucket = RuntimeBucket.from(storage, referenceSampleName, tumorSampleName, arguments);
        BashStartupScript bash = BashStartupScript.of(runtimeBucket.name());

        ResourceDownload referenceGenomeDownload = ResourceDownload.from(storage, "reference_genome", runtimeBucket);
        ResourceDownload amberResourceDownload = ResourceDownload.from(storage, "amber-pon", runtimeBucket);
        bash.addCommand(referenceGenomeDownload).addCommand(amberResourceDownload);

        InputDownload tumorBam = new InputDownload(pair.tumor().finalBamLocation());
        InputDownload tumorBai = new InputDownload(pair.tumor().finalBaiLocation());
        InputDownload referenceBam = new InputDownload(pair.reference().finalBamLocation());
        InputDownload referenceBai = new InputDownload(pair.reference().finalBaiLocation());
        bash.addCommand(tumorBam).addCommand(referenceBam).addCommand(tumorBai).addCommand(referenceBai);

        OutputFile amberBaf = OutputFile.of(tumorSampleName, "amber", "baf");
        bash.addCommand(new AmberApplicationCommand(referenceSampleName,
                referenceBam.getLocalTargetPath(),
                tumorSampleName,
                tumorBam.getLocalTargetPath(),
                referenceGenomeDownload.find("fasta", "fa"),
                amberResourceDownload.find("bed")));
        bash.addCommand(new OutputUpload(GoogleStorageLocation.of(runtimeBucket.name(), namespacedResults.path())));
        JobStatus status = computeEngine.submit(runtimeBucket, VirtualMachineJobDefinition.amber(bash, namespacedResults));
        return AmberOutput.builder()
                .status(status)
                .baf(GoogleStorageLocation.of(runtimeBucket.name(), namespacedResults.path(amberBaf.fileName())))
                .build();
    }
}
