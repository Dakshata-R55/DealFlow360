package com.dealflow360.warehouse.model;

public record WarehouseInventory(
        long warehouseId, long productId, int onHand, int reserved, int minStock, int reorderQty) {

    public int available() {
        return onHand - reserved;
    }
}
