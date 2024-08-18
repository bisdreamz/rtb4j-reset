package pipeline.stages;

import com.smrtb.rtb4j.library.pipeline.AuctionStage;
import com.smrtb.rtb4j.library.pipeline.StageState;
import com.smrtb.rtb4j.library.rtb.NoBidReasons;
import com.smrtb.rtb4j.library.rtb.common.models.CodeDetail;
import com.smrtb.rtb4j.library.rtb.pipeline.AuctionContext;
import org.reset.data.BidderConfigs;

import java.util.concurrent.CompletableFuture;

/**
 * Stage is responsible for injecting the list of matching bidders which
 * are initially eligible to receive the request. Other stages may
 * set a <i>skipReason</i> on the {@link com.smrtb.rtb4j.library.rtb.pipeline.BidderContext}
 * if deemed ineligible for a request further down the pipeline
 */
public class BiddersMatchingStage implements AuctionStage<AuctionContext> {
    @Override
    public CompletableFuture<AuctionContext> process(StageState<AuctionContext> stageState) {
        stageState.context().setBidders(BidderConfigs.bidders());

        // Example useage of setting an NBR if we didnt have any dynamically
        // matching bidders based on any pretargeting rules e.g. GEO
        //stageState.context().setNoBidReason(
        //        NoBidReasons.NO_MATCHING_BIDDERS.ofDescription("No pretargeting bidder matches"));

        return CompletableFuture.completedFuture(stageState.context());
    }
}
