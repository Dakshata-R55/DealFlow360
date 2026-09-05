package com.dealflow360.catalog.dto;

import com.dealflow360.catalog.model.BillingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotNull Long categoryId,
        @NotBlank String name,
        String description,
        @NotBlank String unit,
        @NotNull @DecimalMin("0") BigDecimal basePrice,
        @NotNull @DecimalMin("0") BigDecimal costPrice,
        @NotNull @DecimalMin("0") BigDecimal taxPercent,
        @NotNull BillingType billingType,
        Boolean active) {}
