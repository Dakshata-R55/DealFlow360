package com.dealflow360.catalog.dto;

import com.dealflow360.catalog.model.BillingType;
import java.math.BigDecimal;

public record PatchProductRequest(
        Long categoryId,
        String name,
        String description,
        String unit,
        BigDecimal basePrice,
        BigDecimal costPrice,
        BigDecimal taxPercent,
        BillingType billingType,
        Boolean active) {}
