package com.dealflow360.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VariantRequest(
        @NotBlank String attributeName,
        @NotBlank String attributeValue,
        @NotNull @DecimalMin("0") BigDecimal extraPrice) {}
