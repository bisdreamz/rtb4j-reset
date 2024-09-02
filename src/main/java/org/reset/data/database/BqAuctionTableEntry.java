package org.reset.data.database;

import com.smrtb.rtb4j.adcom.v1.DeviceTypes;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.AuctionContext;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.BidContext;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.BidResponseContext;
import com.smrtb.rtb4j.library.rtb.pipeline.auction.BidderContext;
import com.smrtb.rtb4j.openrtb2x.request.BidRequest;
import com.smrtb.rtb4j.openrtb2x.request.Imp;
import com.smrtb.rtb4j.openrtb2x.request.Publisher;
import com.smrtb.rtb4j.openrtb2x.response.Bid;

import java.util.HashMap;
import java.util.Map;

public class BqAuctionTableEntry {

    private String globalAuctionId;
    private String uniqueRequestId;
    private String noBidReason;
    private String auctionId;
    private Integer tmax;
    private String pubId;
    private String pubName;
    private String pubDomain;
    private String medId;
    private String medDomain;
    private String devOs;
    private String devType;
    private String devGeo;
    private String bidderId;
    private String bidderName;
    private String bidderEndpointId;
    private String bidderEndpointName;
    private Float impFloor;
    private String impFormat;
    private String bidGlobalId;
    private Float bidGross;
    private Float bidCost;
    private String bidRejectReason;
    private String bidCrid;
    private String bidCid;
    private String bidDealId;
    private String bidSeat;
    private Boolean bidResTimeout;
    private Integer bidResMs;

    public BqAuctionTableEntry(AuctionContext ctx, Imp imp, BidderContext bidderCtx, BidContext bidCtx,
                                                                String uniqueRequestId) {
        BidRequest br = ctx.getBidRequest();

        Publisher pub = br.getApp() != null ? br.getApp().getPublisher()
                : br.getSite().getPublisher();

        this.globalAuctionId = ctx.getGlobalId();
        this.uniqueRequestId = uniqueRequestId;
        this.noBidReason = ctx.getNoBidReason().toString();
        this.auctionId = br.getId();
        this.tmax = br.getTmax();
        if (pub != null) {
            this.pubId = pub.getId();
            this.pubDomain = pub.getDomain();
            this.pubName = pub.getName();
        }
        if (br.getApp() != null) {
            this.medId = br.getApp().getId();
            this.medDomain = br.getApp().getBundle();
        } else {
            this.medId = br.getSite().getId();
            this.medDomain = br.getSite().getDomain();
        }
        this.devOs = br.getDevice().getOs();
        this.devGeo = br.getDevice().getGeo().getCountry();
        if (br.getDevice().getDevicetype() != null) {
            this.devType = switch (br.getDevice().getDevicetype()) {
                case DeviceTypes.MOBILE_TABLET_GENERAL__1, DeviceTypes.PHONE__4 -> "Mobile";
                case DeviceTypes.CONNECTED_TV__3, DeviceTypes.SET_TOP_BOX__7 -> "CTV";
                case DeviceTypes.TABLET__5 -> "Tablet";
                case DeviceTypes.CONNECTED_DEV__6 -> "IOT";
                case DeviceTypes.PERSONAL_COMP__2 -> "Desktop";
                case DeviceTypes.OOH__8 -> "DOOH";
                default -> "Unknown";
            };
        }
        this.bidderId = bidderCtx.getBidderEndpoint().getBidderId();
        this.bidderName = bidderCtx.getBidderEndpoint().getBidderName();
        this.bidderEndpointId = bidderCtx.getBidderEndpoint().getId();
        this.bidderEndpointName = bidderCtx.getBidderEndpoint().getName();
        this.impFormat = imp.getVideo() != null ? "VIDEO" : "DISPLAY";
        this.impFloor = imp.getBidfloor();

        BidResponseContext bidResponseContext = bidderCtx.getBidResponseContext();
        if (bidResponseContext != null) {
            this.bidResMs = bidResponseContext.getResponseLatency();
            this.bidResTimeout = bidResponseContext.isTimeout();
        }

        if (bidCtx != null && bidCtx.getBid() != null) {
            Bid bid = bidCtx.getBid();

            this.bidGlobalId = bidCtx.getGlobalId();
            this.bidCrid = bid.getCrid();
            this.bidCid = bid.getCid();
            this.bidGross = bid.getPrice();
            this.bidCost = bid.getPrice(); // until margin added TODO
            this.bidDealId = bid.getDealid();
            this.bidSeat = bidCtx.getSeatBidContext().getSeatBid().getSeat();
            this.bidRejectReason = bidCtx.getLossCode() != null ? bidCtx.getLossCode().toString() : null;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("globalAuctionId", globalAuctionId);
        map.put("noBidReason", noBidReason);
        map.put("auctionId", auctionId);
        map.put("tmax", tmax);
        map.put("pubId", pubId);
        map.put("pubName", pubName);
        map.put("pubDomain", pubDomain);
        map.put("medId", medId);
        map.put("medDomain", medDomain);
        map.put("devOs", devOs);
        map.put("devType", devType);
        map.put("devGeo", devGeo);
        map.put("bidderId", bidderId);
        map.put("bidderName", bidderName);
        map.put("bidderEndpointId", bidderEndpointId);
        map.put("bidderEndpointName", bidderEndpointName);
        map.put("uniqueRequestId", uniqueRequestId);
        map.put("impFloor", impFloor);
        map.put("impFormat", impFormat);
        map.put("bidGlobalId", bidGlobalId);
        map.put("bidGross", bidGross);
        map.put("bidCost", bidCost);
        map.put("bidRejectReason", bidRejectReason);
        map.put("bidCrid", bidCrid);
        map.put("bidCid", bidCid);
        map.put("bidDealId", bidDealId);
        map.put("bidSeat", bidSeat);
        map.put("bidResTimeout", bidResTimeout);
        map.put("bidResMs", bidResMs);

        return map;
    }

    /**
     * RTB4J assigned global identifier for the incoming auction request this entry belongs to
     * @return GlobalId String from {@link AuctionContext}
     */
    public String getGlobalAuctionId() {
        return globalAuctionId;
    }

    public BqAuctionTableEntry setGlobalAuctionId(String globalAuctionId) {
        this.globalAuctionId = globalAuctionId;
        return this;
    }

    /**
     * @return An arbitrary identifier to recognize table entries that belong
     * to same outgoing bid request to a unique demand partner.
     */
    public String getUniqueRequestId() {
        return uniqueRequestId;
    }

    public BqAuctionTableEntry setUniqueRequestId(String uniqueRequestId) {
        this.uniqueRequestId = uniqueRequestId;
        return this;
    }

    public String getNoBidReason() {
        return noBidReason;
    }

    public BqAuctionTableEntry setNoBidReason(String noBidReason) {
        this.noBidReason = noBidReason;
        return this;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public BqAuctionTableEntry setAuctionId(String auctionId) {
        this.auctionId = auctionId;
        return this;
    }

    public Integer getTmax() {
        return tmax;
    }

    public BqAuctionTableEntry setTmax(Integer tmax) {
        this.tmax = tmax;
        return this;
    }

    public String getPubId() {
        return pubId;
    }

    public BqAuctionTableEntry setPubId(String pubId) {
        this.pubId = pubId;
        return this;
    }

    public String getPubName() {
        return pubName;
    }

    public BqAuctionTableEntry setPubName(String pubName) {
        this.pubName = pubName;
        return this;
    }

    public String getPubDomain() {
        return pubDomain;
    }

    public BqAuctionTableEntry setPubDomain(String pubDomain) {
        this.pubDomain = pubDomain;
        return this;
    }

    public String getMedId() {
        return medId;
    }

    public BqAuctionTableEntry setMedId(String medId) {
        this.medId = medId;
        return this;
    }

    public String getMedDomain() {
        return medDomain;
    }

    public BqAuctionTableEntry setMedDomain(String medDomain) {
        this.medDomain = medDomain;
        return this;
    }

    public String getDevOs() {
        return devOs;
    }

    public BqAuctionTableEntry setDevOs(String devOs) {
        this.devOs = devOs;
        return this;
    }

    public String getDevType() {
        return devType;
    }

    public BqAuctionTableEntry setDevType(String devType) {
        this.devType = devType;
        return this;
    }

    public String getDevGeo() {
        return devGeo;
    }

    public BqAuctionTableEntry setDevGeo(String devGeo) {
        this.devGeo = devGeo;
        return this;
    }

    public String getBidderId() {
        return bidderId;
    }

    public BqAuctionTableEntry setBidderId(String bidderId) {
        this.bidderId = bidderId;
        return this;
    }

    public String getBidderName() {
        return bidderName;
    }

    public BqAuctionTableEntry setBidderName(String bidderName) {
        this.bidderName = bidderName;
        return this;
    }

    public String getBidderEndpointId() {
        return bidderEndpointId;
    }

    public BqAuctionTableEntry setBidderEndpointId(String bidderEndpointId) {
        this.bidderEndpointId = bidderEndpointId;
        return this;
    }

    public String getBidderEndpointName() {
        return bidderEndpointName;
    }

    public BqAuctionTableEntry setBidderEndpointName(String bidderEndpointName) {
        this.bidderEndpointName = bidderEndpointName;
        return this;
    }

    public Float getImpFloor() {
        return impFloor;
    }

    public BqAuctionTableEntry setImpFloor(Float impFloor) {
        this.impFloor = impFloor;
        return this;
    }

    public String getImpFormat() {
        return impFormat;
    }

    public BqAuctionTableEntry setImpFormat(String impFormat) {
        this.impFormat = impFormat;
        return this;
    }

    public String getBidGlobalId() {
        return bidGlobalId;
    }

    public BqAuctionTableEntry setBidGlobalId(String bidGlobalId) {
        this.bidGlobalId = bidGlobalId;
        return this;
    }

    public Float getBidGross() {
        return bidGross;
    }

    public BqAuctionTableEntry setBidGross(Float bidGross) {
        this.bidGross = bidGross;
        return this;
    }

    public Float getBidCost() {
        return bidCost;
    }

    public BqAuctionTableEntry setBidCost(Float bidCost) {
        this.bidCost = bidCost;
        return this;
    }

    public String getBidRejectReason() {
        return bidRejectReason;
    }

    public BqAuctionTableEntry setBidRejectReason(String bidRejectReason) {
        this.bidRejectReason = bidRejectReason;
        return this;
    }

    public String getBidCrid() {
        return bidCrid;
    }

    public BqAuctionTableEntry setBidCrid(String bidCrid) {
        this.bidCrid = bidCrid;
        return this;
    }

    public String getBidCid() {
        return bidCid;
    }

    public BqAuctionTableEntry setBidCid(String bidCid) {
        this.bidCid = bidCid;
        return this;
    }

    public String getBidDealId() {
        return bidDealId;
    }

    public BqAuctionTableEntry setBidDealId(String bidDealId) {
        this.bidDealId = bidDealId;
        return this;
    }

    public String getBidSeat() {
        return bidSeat;
    }

    public BqAuctionTableEntry setBidSeat(String bidSeat) {
        this.bidSeat = bidSeat;
        return this;
    }

    public Boolean getBidResTimeout() {
        return bidResTimeout;
    }

    public BqAuctionTableEntry setBidResTimeout(Boolean bidResTimeout) {
        this.bidResTimeout = bidResTimeout;
        return this;
    }

    public Integer getBidResMs() {
        return bidResMs;
    }

    public BqAuctionTableEntry setBidResMs(Integer bidResMs) {
        this.bidResMs = bidResMs;
        return this;
    }
}
