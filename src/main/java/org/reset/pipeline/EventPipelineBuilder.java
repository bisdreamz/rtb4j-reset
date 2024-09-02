package org.reset.pipeline;

import com.smrtb.rtb4j.library.Rtb4j;
import com.smrtb.rtb4j.library.pipeline.AuctionPipeline;
import com.smrtb.rtb4j.library.pipeline.TaskPipeline;
import com.smrtb.rtb4j.library.rtb.common.cache.LocalTrackerCache;
import com.smrtb.rtb4j.library.rtb.pipeline.event.NotificationEvent;
import com.smrtb.rtb4j.library.rtb.stages.event.EventTelemetryStage;
import com.smrtb.rtb4j.library.rtb.stages.event.ImpressionLoggingStage;
import org.reset.pipeline.stages.event.EventImpDedupeStage;

public class EventPipelineBuilder {

    public static PipelineContext<NotificationEvent> build(Rtb4j rtb4j, TaskPipeline startupPipeline,
                                                    TaskPipeline shutdownPipeline) {
        AuctionPipeline<NotificationEvent> notificationPipeline = new AuctionPipeline<>(rtb4j);

        // Filters for billing events and filters out local duplicates
        notificationPipeline.then("impDedupeAndFilter", new EventImpDedupeStage());
        // ImpLoggingStage caches burls and logs them but not yet fires them
        notificationPipeline.then("notificationLogger", new ImpressionLoggingStage(new LocalTrackerCache()));
        notificationPipeline.always("notificationTelemetry", new EventTelemetryStage());
        // pipeline.then("TrafficShapingImpRecordStage", new ImpressionLoggingStage(trackerCache));

        return new PipelineContext<>(notificationPipeline, startupPipeline, shutdownPipeline);
    }
}
