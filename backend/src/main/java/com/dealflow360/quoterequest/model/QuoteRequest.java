package com.dealflow360.quoterequest.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record QuoteRequest(
        long id,
        String requestNumber,
        long customerUserId,
        long sellerCompanyId,
        QuoteRequestStatus status,
        LocalDate requestedDeliveryDate,
        BigDecimal targetBudget,
        BigDecimal expectedDiscountPercent,
        String notes,
        Long quotationId,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt) {}
