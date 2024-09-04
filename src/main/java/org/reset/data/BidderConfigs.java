package org.reset.data;

import com.smrtb.rtb4j.library.rtb.common.OpenrtbVersions;
import com.smrtb.rtb4j.library.rtb.common.models.BidderEndpoint;

import java.util.List;

public class BidderConfigs {

    public static List<BidderEndpoint> bidders() {
        return List.of(new BidderEndpoint()
                .setBidderId("bidder_id_test")
                .setBidderName("Test Bidder")
                .setName("Test Bidder Endpoint 1")
                .setId("bidder_id_test_endpoint_1")
                .setUrl("http://localhost:8081/auction")
                .setOpenrtbVersion(OpenrtbVersions.V_2_6)
                .setMultiImpSupport(true)
        );
    }
}
