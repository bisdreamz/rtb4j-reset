package org.reset.pipeline;

import com.smrtb.rtb4j.library.Rtb4j;
import com.smrtb.rtb4j.library.pipeline.AuctionPipeline;
import com.smrtb.rtb4j.library.pipeline.TaskPipeline;
import com.smrtb.rtb4j.library.rtb.NotificationUrlProducer;
import com.smrtb.rtb4j.library.rtb.common.DbIpLocationClient;
import com.smrtb.rtb4j.library.rtb.common.Ip2LocationClient;
import com.smrtb.rtb4j.library.rtb.common.IpLookupClient;
import com.smrtb.rtb4j.library.rtb.common.cache.LocalTrackerCache;
import com.smrtb.rtb4j.library.rtb.common.cache.TrackerValueCache;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.AuctionContext;
import com.smrtb.rtb4j.library.rtb.stages.auction.*;
import com.smrtb.rtb4j.library.store.bq.BigQueryStore;
import org.reset.pipeline.stages.auction.BiddersMatchingStage;
import org.reset.pipeline.stages.auction.BigqueryDbStage;

import java.util.List;

public class AuctionPipelineBuilder {

    public static PipelineContext<AuctionContext> build(Rtb4j rtb4j,
                                                        TaskPipeline startupPipeline, TaskPipeline shutdownPipeline) {
        AuctionPipeline pipeline = new AuctionPipeline(rtb4j);

        // These are classes which we must inject and are shared dependencies
        TrackerValueCache trackerCache = new LocalTrackerCache();

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
        pipeline.then("BidderAuctions",  rtbRequestStage);
        pipeline.then("SimpleTracker", new NotificationInjectionStage(new NotificationUrlProducer(
                rtb4j.conf().getNotifications()), trackerCache));
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
