package com.dealflow360.quotation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PatchQuotationLineRequest(
        @Positive BigDecimal quantity,
        @DecimalMin("0") @DecimalMax("100") BigDecimal discountPercent) {}
