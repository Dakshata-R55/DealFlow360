package com.dealflow360.catalog.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Product(
        long id,
        long companyId,
        long categoryId,
        String name,
        String description,
        String unit,
        BigDecimal basePrice,
        BigDecimal costPrice,
        BigDecimal taxPercent,
        BillingType billingType,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
