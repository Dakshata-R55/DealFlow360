package com.dealflow360.fulfillment.service;

import com.dealflow360.catalog.model.BillingType;
import com.dealflow360.catalog.model.Product;
import com.dealflow360.catalog.repository.ProductRepository;
import com.dealflow360.customer.model.Customer;
import com.dealflow360.customer.repository.CustomerRepository;
import com.dealflow360.fulfillment.dto.FulfillmentAllocationResponse;
import com.dealflow360.fulfillment.dto.FulfillmentLineResponse;
import com.dealflow360.fulfillment.dto.FulfillmentListItemResponse;
import com.dealflow360.fulfillment.dto.FulfillmentOverrideRequest;
import com.dealflow360.fulfillment.dto.FulfillmentOverrideRow;
import com.dealflow360.fulfillment.dto.FulfillmentPlanResponse;
import com.dealflow360.fulfillment.model.AllocationKind;
import com.dealflow360.fulfillment.model.AllocationSource;
import com.dealflow360.fulfillment.model.FulfillmentAllocation;
import com.dealflow360.fulfillment.repository.FulfillmentAllocationRepository;
import com.dealflow360.quotation.model.Quotation;
import com.dealflow360.quotation.model.QuotationLine;
import com.dealflow360.quotation.model.QuotationStatus;
import com.dealflow360.quotation.repository.QuotationLineRepository;
import com.dealflow360.quotation.repository.QuotationRepository;
import com.dealflow360.shared.exception.BadRequestException;
import com.dealflow360.shared.exception.ConflictException;
import com.dealflow360.shared.exception.NotFoundException;
import com.dealflow360.warehouse.model.Warehouse;
import com.dealflow360.warehouse.model.WarehouseStock;
import com.dealflow360.warehouse.repository.WarehouseRepository;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FulfillmentService {

    private final QuotationRepository quotationRepository;
    private final QuotationLineRepository lineRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final FulfillmentAllocationRepository allocationRepository;
    private final FulfillmentPlanner planner;

    public FulfillmentService(
            QuotationRepository quotationRepository,
            QuotationLineRepository lineRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            WarehouseRepository warehouseRepository,
            FulfillmentAllocationRepository allocationRepository,
            FulfillmentPlanner planner) {
        this.quotationRepository = quotationRepository;
        this.lineRepository = lineRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.warehouseRepository = warehouseRepository;
        this.allocationRepository = allocationRepository;
        this.planner = planner;
    }

    public List<FulfillmentListItemResponse> list(long companyId) {
        List<FulfillmentListItemResponse> rows = new ArrayList<>();
        for (Quotation quotation : quotationRepository.findByCompany(companyId)) {
            if (quotation.status() != QuotationStatus.CONFIRMED) {
                continue;
            }
            FulfillmentPlanResponse plan = get(companyId, quotation.id());
            rows.add(new FulfillmentListItemResponse(
                    plan.quotationId(),
                    plan.quoteNumber(),
                    plan.customerName(),
                    plan.shipQty(),
                    plan.backorderQty(),
                    plan.warehouses()));
        }
        return rows;
    }

    public FulfillmentPlanResponse get(long companyId, long quotationId) {
        Quotation quotation = requireConfirmed(companyId, quotationId);
        return toPlan(companyId, quotation);
    }

    public FulfillmentSummary summary(long companyId, long quotationId) {
        List<FulfillmentAllocation> allocations = allocationRepository.findByQuotation(companyId, quotationId);
        int ship = 0;
        int back = 0;
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (FulfillmentAllocation allocation : allocations) {
            if (allocation.kind() == AllocationKind.SHIP) {
                ship += allocation.quantity();
                if (allocation.warehouseId() != null) {
                    warehouseRepository
                            .findById(allocation.warehouseId(), companyId)
                            .map(Warehouse::name)
                            .ifPresent(names::add);
                }
            } else {
                back += allocation.quantity();
            }
        }
        return new FulfillmentSummary(ship, back, List.copyOf(names));
    }

    @Transactional
    public FulfillmentPlanResponse planIfAbsent(long companyId, long quotationId) {
        if (!allocationRepository.findByQuotation(companyId, quotationId).isEmpty()) {
            return get(companyId, quotationId);
        }
        return recompute(companyId, quotationId, AllocationSource.AUTO);
    }

    @Transactional
    public FulfillmentPlanResponse auto(long companyId, long quotationId) {
        return recompute(companyId, quotationId, AllocationSource.AUTO);
    }

    @Transactional
    public FulfillmentPlanResponse override(long companyId, long quotationId, FulfillmentOverrideRequest request) {
        Quotation quotation = requireConfirmed(companyId, quotationId);
        QuotationLine line = lineRepository
                .findById(request.lineId(), quotation.id())
                .orElseThrow(() -> new NotFoundException("Quote line not found"));
        if (line.billingType() != BillingType.ONE_TIME) {
            throw new BadRequestException("Recurring lines are not fulfilled from warehouses");
        }
        int needed = quantity(line);
        List<FulfillmentOverrideRow> rows = request.rows();
        int sum = 0;
        Set<Long> warehouses = new HashSet<>();
        for (FulfillmentOverrideRow row : rows) {
            AllocationKind kind = parseKind(row.kind());
            sum += row.quantity();
            if (kind == AllocationKind.BACKORDER) {
                if (row.warehouseId() != null) {
                    throw new BadRequestException("Backorder cannot name a warehouse");
                }
            } else {
                if (row.warehouseId() == null) {
                    throw new BadRequestException("Ship rows need a warehouse");
                }
                if (!warehouses.add(row.warehouseId())) {
                    throw new BadRequestException("Do not split the same warehouse twice on one product");
                }
            }
        }
        if (sum != needed) {
            throw new BadRequestException("Split quantities must add up to the product quantity");
        }
        releaseLine(companyId, quotation.id(), line);
        List<WarehouseStock> stocks = warehouseRepository.lockActiveStockForProduct(companyId, line.productId());
        for (FulfillmentOverrideRow row : rows) {
            AllocationKind kind = parseKind(row.kind());
            if (kind == AllocationKind.SHIP) {
                WarehouseStock stock = stocks.stream()
                        .filter(item -> item.warehouseId() == row.warehouseId())
                        .findFirst()
                        .orElseThrow(() -> new BadRequestException("Warehouse is not active for this company"));
                if (!stock.hasInventoryRow() || stock.available() < row.quantity()) {
                    throw new ConflictException("Not enough available stock in " + stock.name());
                }
                warehouseRepository.addReserved(row.warehouseId(), line.productId(), row.quantity());
            }
            allocationRepository.insert(
                    companyId,
                    quotation.id(),
                    line.id(),
                    row.warehouseId(),
                    row.quantity(),
                    kind,
                    AllocationSource.OVERRIDE);
        }
        return toPlan(companyId, quotation);
    }

    @Transactional
    public FulfillmentPlanResponse consolidateBackorder(long companyId, long quotationId) {
        Quotation quotation = requireConfirmed(companyId, quotationId);
        List<FulfillmentAllocation> allocations = allocationRepository.findByQuotation(companyId, quotationId);
        Set<Long> lineIds = new HashSet<>();
        for (FulfillmentAllocation allocation : allocations) {
            if (allocation.kind() == AllocationKind.BACKORDER) {
                lineIds.add(allocation.quotationLineId());
            }
        }
        for (long lineId : lineIds) {
            List<FulfillmentAllocation> backorders = allocations.stream()
                    .filter(row -> row.quotationLineId() == lineId && row.kind() == AllocationKind.BACKORDER)
                    .toList();
            if (backorders.size() <= 1) {
                continue;
            }
            int total = backorders.stream().mapToInt(FulfillmentAllocation::quantity).sum();
            List<FulfillmentAllocation> ships = allocations.stream()
                    .filter(row -> row.quotationLineId() == lineId && row.kind() == AllocationKind.SHIP)
                    .toList();
            allocationRepository.deleteByLine(companyId, quotation.id(), lineId);
            for (FulfillmentAllocation ship : ships) {
                allocationRepository.insert(
                        companyId,
                        quotation.id(),
                        lineId,
                        ship.warehouseId(),
                        ship.quantity(),
                        AllocationKind.SHIP,
                        ship.source());
            }
            allocationRepository.insert(
                    companyId,
                    quotation.id(),
                    lineId,
                    null,
                    total,
                    AllocationKind.BACKORDER,
                    AllocationSource.OVERRIDE);
        }
        return toPlan(companyId, quotation);
    }

    private FulfillmentPlanResponse recompute(long companyId, long quotationId, AllocationSource source) {
        Quotation quotation = requireConfirmed(companyId, quotationId);
        releaseQuote(companyId, quotation);
        allocationRepository.deleteByQuotation(companyId, quotation.id());
        for (QuotationLine line : lineRepository.findByQuotation(quotation.id())) {
            if (line.billingType() != BillingType.ONE_TIME) {
                continue;
            }
            List<WarehouseStock> stocks = warehouseRepository.lockActiveStockForProduct(companyId, line.productId());
            for (FulfillmentPlanner.PlannedSplit split : planner.split(quantity(line), stocks)) {
                if (split.kind() == AllocationKind.SHIP && split.warehouseId() != null) {
                    warehouseRepository.addReserved(split.warehouseId(), line.productId(), split.quantity());
                }
                allocationRepository.insert(
                        companyId,
                        quotation.id(),
                        line.id(),
                        split.warehouseId(),
                        split.quantity(),
                        split.kind(),
                        source);
            }
        }
        return toPlan(companyId, quotation);
    }

    private void releaseQuote(long companyId, Quotation quotation) {
        for (QuotationLine line : lineRepository.findByQuotation(quotation.id())) {
            releaseLine(companyId, quotation.id(), line);
        }
    }

    private void releaseLine(long companyId, long quotationId, QuotationLine line) {
        for (FulfillmentAllocation allocation : allocationRepository.findByQuotation(companyId, quotationId)) {
            if (allocation.quotationLineId() != line.id() || allocation.kind() != AllocationKind.SHIP) {
                continue;
            }
            if (allocation.warehouseId() != null) {
                warehouseRepository.addReserved(allocation.warehouseId(), line.productId(), -allocation.quantity());
            }
        }
        allocationRepository.deleteByLine(companyId, quotationId, line.id());
    }

    private FulfillmentPlanResponse toPlan(long companyId, Quotation quotation) {
        Customer customer = customerRepository.findById(quotation.customerId(), companyId).orElse(null);
        List<FulfillmentAllocation> allocations = allocationRepository.findByQuotation(companyId, quotation.id());
        List<FulfillmentLineResponse> lines = new ArrayList<>();
        int shipQty = 0;
        int backorderQty = 0;
        LinkedHashSet<String> warehouses = new LinkedHashSet<>();
        for (QuotationLine line : lineRepository.findByQuotation(quotation.id())) {
            if (line.billingType() != BillingType.ONE_TIME) {
                continue;
            }
            Product product = productRepository.findById(line.productId(), companyId).orElse(null);
            List<WarehouseStock> stocks = warehouseRepository.findActiveStockForProduct(companyId, line.productId());
            List<FulfillmentAllocationResponse> rowAllocations = new ArrayList<>();
            for (FulfillmentAllocation allocation : allocations) {
                if (allocation.quotationLineId() != line.id()) {
                    continue;
                }
                String warehouseName = null;
                Integer available = null;
                if (allocation.warehouseId() != null) {
                    Warehouse warehouse = warehouseRepository
                            .findById(allocation.warehouseId(), companyId)
                            .orElse(null);
                    warehouseName = warehouse == null ? null : warehouse.name();
                    available = stocks.stream()
                            .filter(stock -> stock.warehouseId() == allocation.warehouseId())
                            .map(WarehouseStock::available)
                            .findFirst()
                            .orElse(null);
                    if (warehouseName != null) {
                        warehouses.add(warehouseName);
                    }
                }
                if (allocation.kind() == AllocationKind.SHIP) {
                    shipQty += allocation.quantity();
                } else {
                    backorderQty += allocation.quantity();
                }
                rowAllocations.add(new FulfillmentAllocationResponse(
                        allocation.id(),
                        allocation.quotationLineId(),
                        allocation.warehouseId(),
                        warehouseName,
                        allocation.quantity(),
                        allocation.kind().name(),
                        allocation.source().name(),
                        available));
            }
            lines.add(new FulfillmentLineResponse(
                    line.id(),
                    line.productId(),
                    product == null ? "Product" : product.name(),
                    quantity(line),
                    line.billingType().name(),
                    rowAllocations));
        }
        return new FulfillmentPlanResponse(
                quotation.id(),
                quotation.quoteNumber(),
                customer == null ? "" : customer.name(),
                quotation.status().name(),
                shipQty,
                backorderQty,
                List.copyOf(warehouses),
                lines);
    }

    private Quotation requireConfirmed(long companyId, long quotationId) {
        Quotation quotation = quotationRepository
                .findById(quotationId, companyId)
                .orElseThrow(() -> new NotFoundException("Quotation not found"));
        if (quotation.status() != QuotationStatus.CONFIRMED) {
            throw new ConflictException("Fulfillment starts after the customer confirms");
        }
        return quotation;
    }

    private static AllocationKind parseKind(String kind) {
        try {
            return AllocationKind.valueOf(kind.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Kind must be SHIP or BACKORDER");
        }
    }

    private static int quantity(QuotationLine line) {
        return line.quantity().setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public record FulfillmentSummary(int shipQty, int backorderQty, List<String> shipFrom) {}
}
