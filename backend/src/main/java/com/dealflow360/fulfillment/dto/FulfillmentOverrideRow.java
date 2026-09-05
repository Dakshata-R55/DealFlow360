package com.dealflow360.fulfillment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FulfillmentOverrideRow(Long warehouseId, @NotNull @Min(1) Integer quantity, @NotBlank String kind) {}
