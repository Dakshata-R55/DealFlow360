package com.dealflow360.warehouse.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WarehouseRequest(
        @NotBlank String name,
        @NotBlank String location,
        @NotNull @DecimalMin("0") BigDecimal shippingCostWeight,
        Boolean active) {}
