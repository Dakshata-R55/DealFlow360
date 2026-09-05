package com.dealflow360.warehouse.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.shared.api.ApiResponse;
import com.dealflow360.warehouse.dto.InventoryPutRequest;
import com.dealflow360.warehouse.dto.InventoryResponse;
import com.dealflow360.warehouse.dto.WarehouseRequest;
import com.dealflow360.warehouse.dto.WarehouseResponse;
import com.dealflow360.warehouse.service.WarehouseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<WarehouseResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, warehouseService.list(SecurityAuth.requireCompany(principal))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> create(
            Authentication authentication, @Valid @RequestBody WarehouseRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, warehouseService.create(SecurityAuth.requireCompany(principal), request)));
    }

    @GetMapping("/{id}/inventory")
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> listInventory(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, warehouseService.listInventory(SecurityAuth.requireCompany(principal), id)));
    }

    @PutMapping("/{id}/inventory/{productId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> upsertInventory(
            Authentication authentication,
            @PathVariable long id,
            @PathVariable long productId,
            @Valid @RequestBody InventoryPutRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, warehouseService.upsertInventory(SecurityAuth.requireCompany(principal), id, productId, request)));
    }

    private static AuthPrincipal internal(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return principal;
    }
}
