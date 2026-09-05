package com.dealflow360.fulfillment.dto;

import java.util.List;

public record FulfillmentListItemResponse(
        long quotationId,
        String quoteNumber,
        String customerName,
        int shipQty,
        int backorderQty,
        List<String> warehouses) {}
