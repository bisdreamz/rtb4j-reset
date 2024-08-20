package org.reset.server;

import com.smrtb.rtb4j.library.pipeline.AuctionPipeline;
import com.smrtb.rtb4j.library.rtb.NoBidReasons;
import com.smrtb.rtb4j.library.rtb.WebServer;
import com.smrtb.rtb4j.library.rtb.common.models.CodeDetail;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.AuctionContext;
import com.smrtb.rtb4j.openrtb2x.response.BidResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ServerRoutes {

    private static final Logger log = LogManager.getLogger(ServerRoutes.class);

    public static void registerRtbHandler(WebServer server, AuctionPipeline<AuctionContext> pipeline,
                                   String path) {
        server.registerBidRequestHandler(path, (req, vtx) -> {
            AuctionContext auctionContext = new AuctionContext(req);

            log.debug("Got bid request");

            return pipeline.process(auctionContext).thenApply(result -> {
                CodeDetail nbr = result.getNoBidReason();

                if (nbr != NoBidReasons.NONE) {
                    vtx.response().setStatusMessage(result.getNoBidReason().toString());

                    return new BidResponse().setId(req.getId())
                            .setNbr(result.getNoBidReason().getNbr());
                }

                return auctionContext.getBidResponse();
            });
        });
    }

    public static void registerRtbNobidHandler(WebServer server, String path) {
        server.registerBidRequestHandler(path, (rq, ctx) -> {
            return CompletableFuture.completedFuture(new BidResponse().setId(rq.getId()).setNbr(1));
        });
    }

}
