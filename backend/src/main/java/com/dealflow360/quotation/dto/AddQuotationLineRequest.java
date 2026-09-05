package com.dealflow360.quotation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AddQuotationLineRequest(
        @NotNull Long productId,
        Long variantId,
        @NotNull @Positive BigDecimal quantity,
        @DecimalMin("0") @DecimalMax("100") BigDecimal discountPercent) {}
