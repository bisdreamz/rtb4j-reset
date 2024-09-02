package org.reset.pipeline.stages.auction;

import com.smrtb.rtb4j.library.Rtb4j;
import com.smrtb.rtb4j.library.pipeline.AuctionStage;
import com.smrtb.rtb4j.library.pipeline.LifecycleTask;
import com.smrtb.rtb4j.library.pipeline.StageState;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.AuctionContext;
import com.smrtb.rtb4j.library.store.bq.BatchingBqStore;
import com.smrtb.rtb4j.library.store.bq.BigQueryStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reset.data.database.BqAuctionTableEntry;
import org.reset.data.database.BqAuctionTableTransform;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class BigqueryDbStage implements AuctionStage<AuctionContext>, LifecycleTask {

    private static Logger log = LogManager.getLogger(BigqueryDbStage.class);

    private final Rtb4j rtb4j;
    private BigQueryStore bqStore;
    private String bqProjectId;
    private String bqAuthJsonPath;

    private BatchingBqStore<AuctionContext> batchingBqStore;

    public BigqueryDbStage(Rtb4j rtb4j, String bqProjectId, String bqAuthJsonPath) {
        this.rtb4j = rtb4j;
        this.bqProjectId = bqProjectId;
        this.bqAuthJsonPath = bqAuthJsonPath;
    }

    public BigqueryDbStage(Rtb4j rtb4j, BigQueryStore bqStore) {
        this.rtb4j = rtb4j;
        this.bqStore = bqStore;
    }

    @Override
    public CompletableFuture<Void> startup() {
        try {
            if (bqStore == null)
                this.bqStore = new BigQueryStore(bqProjectId, bqAuthJsonPath);

            this.batchingBqStore = new BatchingBqStore<>(this.rtb4j, this.bqStore);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        this.batchingBqStore.registerTransform(
                new BqAuctionTableTransform("dataset", "table"),
                400,
                Duration.ofSeconds(5));

        return this.batchingBqStore.startup();
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        // Flush any remaining data
        return this.batchingBqStore.shutdown();
    }

    @Override
    public CompletableFuture<AuctionContext> process(StageState<AuctionContext> stageState) {
        AuctionContext ctx = stageState.context();


        log.info("Queued batching auction context");

        if (this.batchingBqStore == null)
            return CompletableFuture.failedFuture
                    (new IllegalStateException("Startup lifecycle hook not called or complete"));

        this.batchingBqStore.queue(ctx);

        return CompletableFuture.completedFuture(ctx);
    }

}
