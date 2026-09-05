package com.dealflow360.quoterequest.dto;

import com.dealflow360.quoterequest.model.QuoteRequestStatus;
import com.dealflow360.quotation.model.QuotationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record QuoteRequestResponse(
        long id,
        String requestNumber,
        long sellerCompanyId,
        String sellerCompanyName,
        long customerUserId,
        String customerName,
        QuoteRequestStatus status,
        String statusLabel,
        LocalDate requestedDeliveryDate,
        BigDecimal expectedDiscountPercent,
        String notes,
        Long quotationId,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        String customerTierName,
        BigDecimal catalogMrpTotal,
        BigDecimal indicativeTotal,
        BigDecimal expectedTotal,
        QuotationStatus quotationStatus,
        BigDecimal quotationTotal,
        List<QuoteRequestLineResponse> lines) {}
