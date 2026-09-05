package com.dealflow360.quotation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AddQuotationLineRequest(
        @NotNull Long productId, Long variantId, @NotNull @Positive BigDecimal quantity) {}
