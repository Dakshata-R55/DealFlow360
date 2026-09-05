package com.dealflow360.fulfillment.dto;

import java.util.List;

public record FulfillmentPlanResponse(
        long quotationId,
        String quoteNumber,
        String customerName,
        String status,
        int shipQty,
        int backorderQty,
        List<String> warehouses,
        List<FulfillmentLineResponse> lines) {}
