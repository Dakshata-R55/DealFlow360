package com.dealflow360.catalog.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductVariant(
        long id,
        long productId,
        String attributeName,
        String attributeValue,
        BigDecimal extraPrice,
        Instant createdAt,
        Instant updatedAt) {}
