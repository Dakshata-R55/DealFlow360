package com.dealflow360.warehouse.service;

import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.shared.exception.NotFoundException;
import com.dealflow360.warehouse.dto.InventoryPutRequest;
import com.dealflow360.warehouse.dto.InventoryResponse;
import com.dealflow360.warehouse.dto.WarehouseRequest;
import com.dealflow360.warehouse.dto.WarehouseResponse;
import com.dealflow360.warehouse.model.Warehouse;
import com.dealflow360.warehouse.repository.WarehouseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    public WarehouseService(WarehouseRepository warehouseRepository, ProductRepository productRepository) {
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
    }

    public List<WarehouseResponse> list(long companyId) {
        return warehouseRepository.findByCompany(companyId).stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    @Transactional
    public WarehouseResponse create(long companyId, WarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.insert(
                companyId,
                request.name().trim(),
                request.location().trim(),
                request.shippingCostWeight(),
                request.active() == null || request.active());
        return WarehouseResponse.from(warehouse);
    }

    public List<InventoryResponse> listInventory(long companyId, long warehouseId) {
        requireWarehouse(companyId, warehouseId);
        return warehouseRepository.findInventory(warehouseId, companyId).stream()
                .map(InventoryResponse::from)
                .toList();
    }

    @Transactional
    public InventoryResponse upsertInventory(
            long companyId, long warehouseId, long productId, InventoryPutRequest request) {
        requireWarehouse(companyId, warehouseId);
        productRepository
                .findById(productId, companyId)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        int reserved = request.reserved() == null ? 0 : request.reserved();
        return InventoryResponse.from(warehouseRepository.upsertInventory(
                warehouseId, productId, request.onHand(), reserved, request.minStock(), request.reorderQty()));
    }

    private void requireWarehouse(long companyId, long warehouseId) {
        warehouseRepository
                .findById(warehouseId, companyId)
                .orElseThrow(() -> new NotFoundException("Warehouse not found"));
    }
}
