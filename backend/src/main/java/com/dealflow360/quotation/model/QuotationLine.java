package com.dealflow360.quotation.model;

import com.dealflow360.catalog.model.BillingType;
import java.math.BigDecimal;
import java.time.Instant;

public record QuotationLine(
        long id,
        long quotationId,
        long productId,
        Long variantId,
        BigDecimal quantity,
        BigDecimal baseUnitPrice,
        BigDecimal resolvedUnitPrice,
        BigDecimal costPrice,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        BigDecimal allowedDiscountPercent,
        BigDecimal lineTotal,
        BigDecimal marginAmount,
        BigDecimal marginPercent,
        BillingType billingType,
        Instant createdAt,
        Instant updatedAt) {}
