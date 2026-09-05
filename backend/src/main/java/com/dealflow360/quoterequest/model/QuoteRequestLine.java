package com.dealflow360.quoterequest.model;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteRequestLine(
        long id,
        long quoteRequestId,
        long productId,
        BigDecimal quantity,
        String notes,
        BigDecimal expectedDiscountPercent,
        Instant createdAt,
        Instant updatedAt) {}
