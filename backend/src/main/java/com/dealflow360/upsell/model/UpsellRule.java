package com.dealflow360.upsell.model;

import java.math.BigDecimal;
import java.time.Instant;

public record UpsellRule(
        long id,
        long companyId,
        long triggerProductId,
        long suggestedProductId,
        BigDecimal score,
        BigDecimal promotionBoost,
        BigDecimal minMarginPct,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
