package com.dealflow360.fulfillment.dto;

import java.util.List;

public record FulfillmentLineResponse(
        long lineId,
        long productId,
        String productName,
        int quantity,
        String billingType,
        List<FulfillmentAllocationResponse> allocations) {}
