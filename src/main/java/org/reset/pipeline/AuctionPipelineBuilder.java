package org.reset.pipeline;

import com.smrtb.rtb4j.library.Rtb4j;
import com.smrtb.rtb4j.library.pipeline.AuctionPipeline;
import com.smrtb.rtb4j.library.pipeline.TaskPipeline;
import com.smrtb.rtb4j.library.rtb.NotificationUrlProducer;
import com.smrtb.rtb4j.library.rtb.common.DbIpLocationClient;
import com.smrtb.rtb4j.library.rtb.common.IpLookupClient;
import com.smrtb.rtb4j.library.rtb.common.cache.TrackerValueCache;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.AuctionContext;
import com.smrtb.rtb4j.library.rtb.stages.auction.*;
import com.smrtb.rtb4j.library.store.bq.BigQueryStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reset.pipeline.stages.auction.BiddersMatchingStage;
import org.reset.pipeline.stages.auction.BigqueryDbStage;

import java.util.Objects;

public class AuctionPipelineBuilder {

    private static final Logger log = LogManager.getLogger(AuctionPipelineBuilder.class);

    public static PipelineContext<AuctionContext> build(Rtb4j rtb4j, TaskPipeline startupPipeline,
                                                        TaskPipeline shutdownPipeline, TrackerValueCache trackerValueCache) {
        AuctionPipeline pipeline = new AuctionPipeline(rtb4j);

        // These may be shared, but have required startup or shutdown lifecycle hooks
        IpLookupClient ipLookupClient = new DbIpLocationClient();
        DeviceLookupYauaaStage deviceLookupYauaaStage = new DeviceLookupYauaaStage();
        BiddersRtbRequestStage rtbRequestStage = new BiddersRtbRequestStage(rtb4j);
        BigqueryDbStage dbStage = new BigqueryDbStage(rtb4j,
                new BigQueryStore(null, null).logOnly());

        // Register stages in order which an auction request will flow through
        pipeline.then("BidReqDefaults", new BidRequestBasicDefaultsStage());
        pipeline.then("BidReqValidator", new BidRequestMinimalValidatorStage());
        pipeline.then("IPLookup", new BidRequestIPInfoStage(ipLookupClient));
        pipeline.then("DeviceLookupYauaa", deviceLookupYauaaStage);
        //pipeline.then("BidderAdapters", new BidderRtbAdapterStage()
        //                .adapter(bidders.getFirst(), new SampleBidderAdapter()));
        pipeline.then("BiddersMatching", new BiddersMatchingStage());
        pipeline.then("MultiImpBreakout", new BidderMultiImpBreakoutStage());
        if (Objects.requireNonNullElse(System.getenv("DEV_SKIP_BIDDER_AUCTIONS"), "false")
                .equals("true")) {
            log.warn("DEV_SKIP_BIDDER_AUCTIONS enabled! Skipping any real bidder auctions");
        } else {
            pipeline.then("BidderAuctions", rtbRequestStage);
        }

        if (!Objects.requireNonNullElse(System.getenv("DEV_SKIP_FORCE_BIDDER"), "false")
                .equals("true")) {
            // for testing, lets us pass through reqs to another test bidder instance
            pipeline.then("ForceTestBidderStage", new ForceBidStage());
        }

        pipeline.then("BurlEventInjector", new NotificationInjectionStage(new NotificationUrlProducer(
                rtb4j.conf().getNotifications()), trackerValueCache, (bidderContext -> NotificationInjectionStage.BurlLocation.BOTH)));
        pipeline.then("CompileResponse", new BidResponseAggregationStage());
        pipeline.always("DbBigqueryLogging", dbStage);
        pipeline.always("PublisherRequestTelemetryStage", new PublisherRequestTelemetryStage());
        pipeline.always("BidderTelemetryStage", new BidderTelemetryStage());

        startupPipeline.register(ipLookupClient::startup);
        startupPipeline.register(rtbRequestStage::startup);
        startupPipeline.register(dbStage::startup);

        shutdownPipeline.register(rtbRequestStage::shutdown);
        shutdownPipeline.register(ipLookupClient::shutdown);
        shutdownPipeline.register(deviceLookupYauaaStage::shutdown);
        shutdownPipeline.register(dbStage::shutdown);

        return new PipelineContext<>(pipeline, startupPipeline, shutdownPipeline);
    }

}
