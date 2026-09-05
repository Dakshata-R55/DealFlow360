package com.dealflow360.fulfillment.service;

import com.dealflow360.fulfillment.model.AllocationKind;
import com.dealflow360.warehouse.model.WarehouseStock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FulfillmentPlanner {

    public record PlannedSplit(Long warehouseId, int quantity, AllocationKind kind) {}

    public List<PlannedSplit> split(int needed, List<WarehouseStock> stocks) {
        List<PlannedSplit> planned = new ArrayList<>();
        int remaining = Math.max(0, needed);
        for (WarehouseStock stock : stocks) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(remaining, Math.max(0, stock.available()));
            if (take <= 0) {
                continue;
            }
            planned.add(new PlannedSplit(stock.warehouseId(), take, AllocationKind.SHIP));
            remaining -= take;
        }
        if (remaining > 0) {
            planned.add(new PlannedSplit(null, remaining, AllocationKind.BACKORDER));
        }
        return planned;
    }
}
