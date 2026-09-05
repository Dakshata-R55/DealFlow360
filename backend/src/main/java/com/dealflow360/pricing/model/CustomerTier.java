package com.dealflow360.pricing.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerTier(
        long id,
        long companyId,
        String name,
        BigDecimal defaultDiscountLimit,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
