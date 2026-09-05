package com.dealflow360.quoterequest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PatchQuoteRequestLineBody(
        @Positive BigDecimal quantity,
        String notes,
        @DecimalMin("0") @DecimalMax("100") BigDecimal expectedDiscountPercent) {}
