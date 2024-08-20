package org.reset.pipeline.stages.auction.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.smrtb.rtb4j.library.pipeline.AuctionStage;
import com.smrtb.rtb4j.library.pipeline.StageState;
import com.smrtb.rtb4j.library.rtb.common.EventBlockReasons;
import com.smrtb.rtb4j.library.rtb.common.models.SkipReason;
import com.smrtb.rtb4j.library.rtb.pipeline.event.NotificationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Filters to handle billable impression events (adm beacon or burl)
 * and keeps a local cache to avoid known duplicate calls
 */
public class EventImpDedupeStage implements AuctionStage<NotificationEvent> {

    private static final Logger log = LogManager.getLogger(EventImpDedupeStage.class);

    private final Cache<String, String> cache;

    public EventImpDedupeStage() {
        this.cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(30)).build();
    }

    @Override
    public CompletableFuture<NotificationEvent> process(StageState<NotificationEvent> stageState) {
        NotificationEvent ev = stageState.context();

        if (ev.getType() == null || ev.getType() != NotificationEvent.Type.IMPRESSION) {
            log.warn("Received event of type {} but currently only handling impression events", ev.getType());

            ev.setBlockReason(EventBlockReasons.UNSUPPORTED.ofDescription("Event type unsupported"));

            return CompletableFuture.completedFuture(ev);
        }

        if (ev.getGlobalBidId() == null || ev.getGlobalBidId().isEmpty()) {
            log.warn("Received billing event with empty global bid id, cannot record!");

            ev.setBlockReason(EventBlockReasons.INVALID.ofDescription("Missing global bid identifier"));
            return CompletableFuture.completedFuture(ev);
        }

        if (cache.getIfPresent(ev.getGlobalBidId()) != null) {
            log.debug("Skipping processing duplicate imp event");

            ev.setBlockReason(EventBlockReasons.DUPE.ofDescription("Imp event recorded already"));
            return CompletableFuture.completedFuture(ev);
        }

        log.debug("Received valid impression event -> {}", ev);
        cache.put(ev.getGlobalBidId(), ev.getGlobalBidId());

        return CompletableFuture.completedFuture(ev);
    }

}
