package com.dealflow360.quotation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CustomerCounterRequest(
        @DecimalMin("0") @DecimalMax("100") BigDecimal expectedDiscountPercent,
        @Valid List<CustomerCounterLine> lines) {

    public record CustomerCounterLine(
            @NotNull Long productId, @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal expectedDiscountPercent) {}
}
