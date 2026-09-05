package com.dealflow360.quoterequest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PatchQuoteRequestBody(
        LocalDate requestedDeliveryDate,
        @DecimalMin("0") @DecimalMax("100") BigDecimal expectedDiscountPercent,
        String notes) {}
