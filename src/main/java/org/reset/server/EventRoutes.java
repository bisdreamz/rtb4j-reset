package org.reset.server;

import com.smrtb.rtb4j.library.RtbConfig;
import com.smrtb.rtb4j.library.pipeline.AuctionPipeline;
import com.smrtb.rtb4j.library.rtb.WebServer;
import com.smrtb.rtb4j.library.rtb.common.cache.TrackerValueCache;
import com.smrtb.rtb4j.library.rtb.common.models.NotificationEvent;
import com.smrtb.rtb4j.library.rtb.stages.impression.ImpressionLoggingStage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EventRoutes {

    private static final Logger log = LogManager.getLogger(EventRoutes.class);

    public static void registerNotificationsHandler(WebServer server, AuctionPipeline<NotificationEvent> pipeline,
                                                    TrackerValueCache trackerCache, RtbConfig.NotificationConfig cfg) {
        server.registerNotificationHandler(cfg, ((event, routingContext) -> {
            pipeline.then("notificationLogger", new ImpressionLoggingStage(trackerCache));
            // TODO Implement a stage to de-duplicate imps, fire BURLs
            // ImpLoggingStage caches burls and logs them but not yet fires them
            // pipeline.then("TrafficShapingImpRecordStage", new ImpressionLoggingStage(trackerCache));

            return pipeline.process(event).whenComplete((ignored, ex) -> {
                if (ex == null) {
                    log.debug("Event processed -> {}", event);
                } else {
                    log.warn("Event processing failed -> {} : {}", event, ex);

                    routingContext.response().setStatusCode(500).setStatusMessage(ex.getMessage());
                }
            });
        }));

    }
}
