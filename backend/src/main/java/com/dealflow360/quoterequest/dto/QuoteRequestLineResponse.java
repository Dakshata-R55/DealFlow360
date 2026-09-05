package com.dealflow360.quoterequest.dto;

import com.dealflow360.catalog.model.BillingType;
import java.math.BigDecimal;

public record QuoteRequestLineResponse(
        long id,
        long productId,
        String productName,
        String categoryName,
        String unit,
        BillingType billingType,
        BigDecimal quantity,
        String notes,
        BigDecimal mrp,
        BigDecimal lineMrp,
        BigDecimal categoryDiscountPercent,
        BigDecimal standingDiscountPercent,
        BigDecimal availableDiscountPercent,
        BigDecimal expectedDiscountPercent,
        boolean independentExpected,
        BigDecimal appliedExpectedPercent,
        BigDecimal indicativeUnitPrice,
        BigDecimal indicativeLineTotal,
        BigDecimal expectedUnitPrice,
        BigDecimal expectedLineTotal) {}
