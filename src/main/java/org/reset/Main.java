package org.reset;

import com.smrtb.rtb4j.library.Rtb4j;
import com.smrtb.rtb4j.library.RtbConfig;
import com.smrtb.rtb4j.library.pipeline.TaskPipeline;
import com.smrtb.rtb4j.library.rtb.WebServer;
import com.smrtb.rtb4j.library.rtb.common.NotificationFields;
import com.smrtb.rtb4j.library.rtb.common.cache.LocalTrackerCache;
import com.smrtb.rtb4j.library.rtb.common.cache.RedisTrackerCache;
import com.smrtb.rtb4j.library.rtb.common.cache.TrackerValueCache;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.AuctionContext;
import com.smrtb.rtb4j.library.rtb.pipeline.event.NotificationEvent;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vertx.core.VertxOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reset.pipeline.EventPipelineBuilder;
import org.reset.pipeline.PipelineContext;
import org.reset.pipeline.RedisConnectionFactory;
import org.reset.server.EventRoutes;
import org.reset.server.ServerRoutes;
import org.reset.pipeline.AuctionPipelineBuilder;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        log.info("rtb4j-reset starting");

        TelemetryProvider.registerGlobal();

        Rtb4j rtb4j = new Rtb4j(new RtbConfig()
                .setNotifications(new RtbConfig.NotificationConfig()
                        .setDomain(Objects.requireNonNullElse(System.getenv("PUBLIC_HOST"), "localhost"))
                        .setBasePath("/events")
                        .setFields(new NotificationFields())
                        .setSslOnly(false))
                .setSchainDomain("resetdigital.co"),
                new VertxOptions().setPreferNativeTransport(true)
        ).setHost(System.getenv("HOST"))
         .setPort(Integer.parseInt(Objects.requireNonNullElse(System.getenv("PORT"), "8080")));

        log.debug("Will listen on {}:{}", rtb4j.getHost(), rtb4j.getPort());

        TaskPipeline startupPipeline = new TaskPipeline(true);
        TaskPipeline shutdownPipeline = new TaskPipeline(false);

        // Create web server and register shutdown as first thing
        // So then db and data may be flushed after all reqs done
        WebServer server = new WebServer(rtb4j);
        shutdownPipeline.register(server::shutdown);

        // Determine which cache to use for partner BURLs
        // Redis will handle production where any node can handle a burl
        // even if the bid originated from a different server
        TrackerValueCache trackerCache;
        final String burlRedis = System.getenv("REDIS_BID_CACHE");
        if (burlRedis != null && !burlRedis.isEmpty()) {
            StatefulRedisConnection<String, String> redisBidCache
                    = RedisConnectionFactory.get(burlRedis, null);
            trackerCache = new RedisTrackerCache(redisBidCache);
            log.info("Found REDIS_BID_CACHE, using redis for burl cache");
        } else {
            log.info("Using local tracker cache for burls");
            trackerCache = new LocalTrackerCache();
        }

        // This helper method will register pipeline stages and
        // related startup/shutdown tasks
        PipelineContext<AuctionContext> rqPipelineCtx // for auction requests
                = AuctionPipelineBuilder.build(rtb4j, startupPipeline, shutdownPipeline, trackerCache);
        PipelineContext<NotificationEvent> eventPipelineCtx // for handling imp/burl events
                = EventPipelineBuilder.build(rtb4j, startupPipeline, shutdownPipeline, trackerCache);

        // Start web server after other dependencies are ready
        startupPipeline.register(server::startup);

        log.info("starting pipeline");

        rqPipelineCtx.startupPipeline().register(() -> {
            ServerRoutes.registerRtbHandler(server, rqPipelineCtx.stagePipeline(), "/auction");
            ServerRoutes.registerRtbNobidHandler(server, "/nobid");
            EventRoutes.registerNotificationsHandler(server, eventPipelineCtx.stagePipeline(),
                    rtb4j.conf().getNotifications());

            // Sample hello world
            server.registerGetHandler("/hi", ctx -> {
                log.info("hi");
                return CompletableFuture.completedFuture("hi");
            });

            return CompletableFuture.completedFuture(null);
        });

        startupPipeline.execute().whenComplete((ignored, ex) -> {
            if (ex != null)
                throw new RuntimeException(ex);

            log.info("Startup complete, now accepting auction requests");
        });

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownPipeline.execute().whenComplete((ignored, ex) -> {
                log.info("Shutdown tasks completed");
                latch.countDown();
            });
        }));

        latch.await();
        System.exit(0);
    }
}