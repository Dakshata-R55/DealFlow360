package com.dealflow360.marketplace.dto;

import com.dealflow360.catalog.model.BillingType;
import java.math.BigDecimal;

public record PublicProductResponse(
        long id,
        String name,
        String categoryName,
        String description,
        String unit,
        BigDecimal indicativePrice,
        BigDecimal categoryDiscountPercent,
        BillingType billingType,
        boolean active) {}
