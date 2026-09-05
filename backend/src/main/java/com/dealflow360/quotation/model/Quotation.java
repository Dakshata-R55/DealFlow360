package com.dealflow360.quotation.model;

import com.dealflow360.policy.model.RiskLevel;
import java.math.BigDecimal;
import java.time.Instant;

public record Quotation(
        long id,
        long companyId,
        String quoteNumber,
        long customerId,
        long salesRepId,
        long priceListId,
        QuotationStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal totalCost,
        BigDecimal marginAmount,
        BigDecimal marginPercent,
        BigDecimal riskScore,
        RiskLevel riskLevel,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant managerApprovedAt,
        Instant financeApprovedAt) {}
