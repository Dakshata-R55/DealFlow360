package com.dealflow360.pricing.model;

import java.time.Instant;

public record PriceList(
        long id,
        long companyId,
        String name,
        String currency,
        long customerTierId,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
