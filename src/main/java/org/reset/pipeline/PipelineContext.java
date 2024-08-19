package org.reset.pipeline;

import com.smrtb.rtb4j.library.pipeline.AuctionPipeline;
import com.smrtb.rtb4j.library.pipeline.BlockingContext;
import com.smrtb.rtb4j.library.pipeline.TaskPipeline;

public final class PipelineContext<T extends BlockingContext> {

    private final AuctionPipeline<T> stagePipeline;
    private final TaskPipeline startupPipeline;
    private final TaskPipeline shutdownPipeline;

    public PipelineContext(AuctionPipeline stagePipeline,
                             TaskPipeline startupPipeline, TaskPipeline shutdownPipeline) {
        this.stagePipeline = stagePipeline;
        this.startupPipeline = startupPipeline;
        this.shutdownPipeline = shutdownPipeline;
    }

    public AuctionPipeline stagePipeline() {
        return stagePipeline;
    }

    public TaskPipeline startupPipeline() {
        return startupPipeline;
    }

    public TaskPipeline shutdownPipeline() {
        return shutdownPipeline;
    }

}
