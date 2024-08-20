package org.reset.server;

import com.smrtb.rtb4j.library.RtbConfig;
import com.smrtb.rtb4j.library.pipeline.AuctionPipeline;
import com.smrtb.rtb4j.library.rtb.WebServer;
import com.smrtb.rtb4j.library.rtb.common.cache.TrackerValueCache;
import com.smrtb.rtb4j.library.rtb.pipeline.event.NotificationEvent;
import com.smrtb.rtb4j.library.rtb.stages.event.ImpressionLoggingStage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reset.pipeline.stages.auction.event.EventImpDedupeStage;

public class EventRoutes {

    private static final Logger log = LogManager.getLogger(EventRoutes.class);

    public static void registerNotificationsHandler(WebServer server, AuctionPipeline<NotificationEvent> pipeline,
                                                    RtbConfig.NotificationConfig cfg) {
        server.registerNotificationHandler(cfg, ((event, routingContext) -> {
            log.debug("Received event notification call");

            return pipeline.process(event).whenComplete((ignored, ex) -> {
                if (ex == null) {
                    log.debug("Event processed -> {}", event);

                    if (event.getBlockReason() != null)
                        routingContext.response().setStatusMessage(event.getBlockReason().toString());
                } else {
                    log.warn("Event processing failed -> {} : {}", event, ex);

                    routingContext.response().setStatusCode(500).setStatusMessage(ex.getMessage());
                }
            });
        }));

    }
}
