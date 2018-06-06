package hmf.pipeline.gatk;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import hmf.io.OutputFile;
import hmf.io.PipelineOutput;
import hmf.pipeline.Stage;
import hmf.sample.FlowCell;
import picard.sam.markduplicates.MarkDuplicates;

class MergeAndMarkDuplicates implements Stage<FlowCell> {

    @Override
    public PipelineOutput output() {
        return PipelineOutput.DUPLICATE_MARKED;
    }

    @Override
    public void execute(FlowCell flowCell) {
        List<String> inputArgs = flowCell.lanes()
                .stream()
                .map(lane -> String.format("I=%s", OutputFile.of(PipelineOutput.SORTED, lane).path()))
                .collect(Collectors.toList());
        inputArgs.add(String.format("O=%s", OutputFile.of(output(), flowCell).path()));
        inputArgs.add(String.format("M=%s", "results/dupes.txt"));

        PicardExecutor.of(new MarkDuplicates(), inputArgs.toArray(new String[inputArgs.size()])).execute();
    }
}
