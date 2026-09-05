package com.dealflow360.fulfillment.model;

import java.time.Instant;

public record FulfillmentAllocation(
        long id,
        long companyId,
        long quotationId,
        long quotationLineId,
        Long warehouseId,
        int quantity,
        AllocationKind kind,
        AllocationSource source,
        Instant createdAt) {}
