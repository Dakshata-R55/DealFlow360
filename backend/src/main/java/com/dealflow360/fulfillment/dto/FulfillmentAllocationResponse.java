package com.dealflow360.fulfillment.dto;

public record FulfillmentAllocationResponse(
        long id,
        long quotationLineId,
        Long warehouseId,
        String warehouseName,
        int quantity,
        String kind,
        String source,
        Integer available) {}
