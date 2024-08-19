package org.reset.pipeline;

import com.smrtb.rtb4j.library.Rtb4j;
import com.smrtb.rtb4j.library.pipeline.AuctionPipeline;
import com.smrtb.rtb4j.library.pipeline.TaskPipeline;
import com.smrtb.rtb4j.library.rtb.NotificationUrlProducer;
import com.smrtb.rtb4j.library.rtb.common.Ip2LocationClient;
import com.smrtb.rtb4j.library.rtb.common.cache.LocalTrackerCache;
import com.smrtb.rtb4j.library.rtb.common.cache.TrackerValueCache;
import com.smrtb.rtb4j.library.rtb.pipeline.AuctionContext;
import com.smrtb.rtb4j.library.rtb.stages.auction.*;
import org.reset.pipeline.stages.BiddersMatchingStage;

public class AuctionPipelineBuilder {

    public static PipelineContext<AuctionContext> build(Rtb4j rtb4j,
                                                        TaskPipeline startupPipeline, TaskPipeline shutdownPipeline) {
        AuctionPipeline pipeline = new AuctionPipeline(rtb4j);

        // These are classes which we must inject and are shared dependencies
        TrackerValueCache trackerCache = new LocalTrackerCache();

        // These may be shared, but have required startup or shutdown lifecycle hooks
        Ip2LocationClient ip2location = new Ip2LocationClient("DB3", true, true,
                "LbdBrpQmS3LqpMQqkAmyv0bhha3HPkNLClojFvPxi93cX30eWKsa0ITPDh49NLRy");
        DeviceLookupYauaaStage deviceLookupYauaaStage = new DeviceLookupYauaaStage();
        BiddersRtbRequestStage rtbRequestStage = new BiddersRtbRequestStage(rtb4j);

        // Register stages in order which an auction request will flow through
        pipeline.then("BidReqDefaults", new BidRequestBasicDefaultsStage());
        pipeline.then("BidReqValidator", new BidRequestMinimalValidatorStage());
        pipeline.then("IPLookup", new BidRequestIPInfoStage(ip2location));
        pipeline.then("DeviceLookupYauaa", deviceLookupYauaaStage);
        //pipeline.then("BidderAdapters", new BidderRtbAdapterStage()
        //                .adapter(bidders.getFirst(), new SampleBidderAdapter()));
        pipeline.then("BiddersMatching", new BiddersMatchingStage());
        pipeline.then("MultiImpBreakout", new BidderMultiImpBreakoutStage());
        pipeline.then("BidderAuctions",  rtbRequestStage);
        pipeline.then("SimpleTracker", new NotificationInjectionStage(new NotificationUrlProducer(
                rtb4j.conf().getNotifications()), trackerCache));
        pipeline.then("CompileResponse", new BidResponseAggregationStage());

        startupPipeline.register(ip2location::startup);
        startupPipeline.register(rtbRequestStage::startup);

        shutdownPipeline.register(rtbRequestStage::shutdown);
        shutdownPipeline.register(ip2location::shutdown);
        shutdownPipeline.register(deviceLookupYauaaStage::shutdown);

        return new PipelineContext<>(pipeline, startupPipeline, shutdownPipeline);
    }

}
