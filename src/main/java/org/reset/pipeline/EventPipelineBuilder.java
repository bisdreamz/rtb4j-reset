package org.reset.pipeline;

import com.smrtb.rtb4j.library.Rtb4j;
import com.smrtb.rtb4j.library.pipeline.AuctionPipeline;
import com.smrtb.rtb4j.library.pipeline.TaskPipeline;
import com.smrtb.rtb4j.library.rtb.common.cache.TrackerValueCache;
import com.smrtb.rtb4j.library.rtb.pipeline.event.NotificationEvent;
import com.smrtb.rtb4j.library.rtb.stages.event.EventTelemetryStage;
import com.smrtb.rtb4j.library.rtb.stages.event.ImpressionFiringStage;

public class EventPipelineBuilder {

    public static PipelineContext<NotificationEvent> build(Rtb4j rtb4j, TaskPipeline startupPipeline,
                                                           TaskPipeline shutdownPipeline, TrackerValueCache trackerValueCache) {
        AuctionPipeline<NotificationEvent> notificationPipeline = new AuctionPipeline<>(rtb4j);

        // Filters for billing events and filters out local duplicates, only needed if using local cache
        // notificationPipeline.then("impDedupeAndFilter", new LocalEventImpDedupeStage());
        // ImpLoggingStage caches burls logs, fires them
        notificationPipeline.then("notificationLogger", new ImpressionFiringStage(rtb4j, trackerValueCache));
        notificationPipeline.always("notificationTelemetry", new EventTelemetryStage());
        // pipeline.then("TrafficShapingImpRecordStage", new ImpressionLoggingStage(trackerCache));

        return new PipelineContext<>(notificationPipeline, startupPipeline, shutdownPipeline);
    }
}
