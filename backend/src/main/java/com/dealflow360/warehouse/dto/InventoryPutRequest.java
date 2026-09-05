package com.dealflow360.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryPutRequest(
        @NotNull @Min(0) Integer onHand,
        @Min(0) Integer reserved,
        @NotNull @Min(0) Integer minStock,
        @NotNull @Min(0) Integer reorderQty) {}
