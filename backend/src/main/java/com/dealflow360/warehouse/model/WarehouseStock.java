package com.dealflow360.warehouse.model;

import java.math.BigDecimal;

public record WarehouseStock(
        long warehouseId,
        String name,
        BigDecimal shippingCostWeight,
        int onHand,
        int reserved,
        boolean hasInventoryRow) {

    public int available() {
        return onHand - reserved;
    }
}
