package com.dealflow360.warehouse.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Warehouse(
        long id,
        long companyId,
        String name,
        String location,
        BigDecimal shippingCostWeight,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
