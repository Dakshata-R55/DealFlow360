package com.dealflow360.pricing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CustomerTierRequest(
        @NotBlank String name, @NotNull @DecimalMin("0") BigDecimal defaultDiscountLimit, Boolean active) {}
