package com.dealflow360.pricing.dto;

import com.dealflow360.pricing.model.CustomerTier;
import java.math.BigDecimal;
import java.time.Instant;

public record CustomerTierResponse(
        long id, String name, BigDecimal defaultDiscountLimit, boolean active, Instant createdAt, Instant updatedAt) {

    public static CustomerTierResponse from(CustomerTier tier) {
        return new CustomerTierResponse(
                tier.id(),
                tier.name(),
                tier.defaultDiscountLimit(),
                tier.active(),
                tier.createdAt(),
                tier.updatedAt());
    }
}
