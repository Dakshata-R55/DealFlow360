package com.dealflow360.warehouse.dto;

import com.dealflow360.warehouse.model.WarehouseInventory;

public record InventoryResponse(
        long warehouseId, long productId, int onHand, int reserved, int available, int minStock, int reorderQty) {

    public static InventoryResponse from(WarehouseInventory inventory) {
        return new InventoryResponse(
                inventory.warehouseId(),
                inventory.productId(),
                inventory.onHand(),
                inventory.reserved(),
                inventory.available(),
                inventory.minStock(),
                inventory.reorderQty());
    }
}
