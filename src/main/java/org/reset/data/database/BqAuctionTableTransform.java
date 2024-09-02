package org.reset.data.database;

import com.smrtb.rtb4j.library.rtb.pipeline.auction.AuctionContext;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.BidContext;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.BidResponseContext;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.BidderContext;
import com.smrtb.rtb4j.library.store.bq.BqDataTransform;
import com.smrtb.rtb4j.openrtb2x.request.BidRequest;
import com.smrtb.rtb4j.openrtb2x.request.Imp;

import java.util.*;

/**
 * Responsible for transforming bid requests and flattening them into individual {@link BqAuctionTableEntry}
 * objects. Each object represents a unique impression opportunity sent to a unique bidder.
 */
public class BqAuctionTableTransform implements BqDataTransform<AuctionContext> {

    private final String dataset;
    private final String table;

    public BqAuctionTableTransform(String dataset, String table) {
        this.dataset = dataset;
        this.table = table;
    }

    @Override
    public String dataset() {
        return this.dataset;
    }

    @Override
    public String table() {
        return this.table;
    }

    private List<BidContext> bidContextsForImp(Imp imp, BidResponseContext resCtx) {
        if (resCtx == null || resCtx.getSeatBidsContext() == null || resCtx.getSeatBidsContext().isEmpty())
            return Collections.emptyList();

        return resCtx.getSeatBidsContext().stream()
                .filter(sbc -> sbc != null && sbc.getBidContext() != null)
                .flatMap(sbc -> sbc.getBidContext().stream())
                .filter(bidc -> bidc != null && bidc.getBid().getImpid().equals(imp.getId()))
                .toList();
    }

    @Override
    public List<Map<String, Object>> transform(AuctionContext auctionContext) {
        List<Map<String, Object>> rows = new ArrayList<>(16);

        List<BidderContext> bidderContexts = auctionContext.getBidders();
        if (bidderContexts == null || bidderContexts.isEmpty()) {
            /* No bidder auctions to record, to at least record the requests we received */
            for (Imp imp : auctionContext.getBidRequest().getImp()) {
                rows.add(new BqAuctionTableEntry(auctionContext, imp, null, null, null).toMap());
            }

            return rows;
        }

        for (BidderContext bidderContext : bidderContexts) {
            String uniqueContextId = "BR-" + UUID.randomUUID().toString();

            BidRequest br = bidderContext.getBidRequest() != null ? bidderContext.getBidRequest()
                    : auctionContext.getBidRequest();
            for (Imp imp : br.getImp()) {
                // need to iterate over imps and bids to create a row for each
                // and that we are matching the bids to the correct impression they are for
                List<BidContext> bids = bidContextsForImp(imp, bidderContext.getBidResponseContext());
                if (bids.isEmpty()) {
                    // Record the bidder received this auction+imp request
                    rows.add(new BqAuctionTableEntry(auctionContext, imp, bidderContext, null, uniqueContextId).toMap());
                } else {
                    // Have bids, store an entry for each
                   bids.forEach(bidContext -> {
                       rows.add(new BqAuctionTableEntry(auctionContext, imp, bidderContext, bidContext, uniqueContextId).toMap());
                   });
                }
            }
        }

        return rows;
    }
}
