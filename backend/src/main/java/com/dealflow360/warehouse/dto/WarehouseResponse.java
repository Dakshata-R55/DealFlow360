package com.dealflow360.warehouse.dto;

import com.dealflow360.warehouse.model.Warehouse;
import java.math.BigDecimal;
import java.time.Instant;

public record WarehouseResponse(
        long id,
        String name,
        String location,
        BigDecimal shippingCostWeight,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.id(),
                warehouse.name(),
                warehouse.location(),
                warehouse.shippingCostWeight(),
                warehouse.active(),
                warehouse.createdAt(),
                warehouse.updatedAt());
    }
}
