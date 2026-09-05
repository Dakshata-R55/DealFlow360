package com.dealflow360.catalog.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.catalog.dto.CreateProductRequest;
import com.dealflow360.catalog.dto.PatchProductRequest;
import com.dealflow360.catalog.dto.ProductResponse;
import com.dealflow360.catalog.dto.ProductVariantResponse;
import com.dealflow360.catalog.dto.VariantRequest;
import com.dealflow360.catalog.service.CatalogService;
import com.dealflow360.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final CatalogService catalogService;

    public ProductController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, catalogService.listProducts(SecurityAuth.requireCompany(principal))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            Authentication authentication, @Valid @RequestBody CreateProductRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED, catalogService.createProduct(SecurityAuth.requireCompany(principal), request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<ProductResponse>> get(Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, catalogService.getProduct(SecurityAuth.requireCompany(principal), id)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            Authentication authentication, @PathVariable long id, @Valid @RequestBody PatchProductRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, catalogService.updateProduct(SecurityAuth.requireCompany(principal), id, request)));
    }

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            Authentication authentication, @PathVariable long id, @Valid @RequestBody VariantRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED, catalogService.createVariant(SecurityAuth.requireCompany(principal), id, request)));
    }

    @PatchMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            Authentication authentication,
            @PathVariable long id,
            @PathVariable long variantId,
            @Valid @RequestBody VariantRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, catalogService.updateVariant(SecurityAuth.requireCompany(principal), id, variantId, request)));
    }

    private static AuthPrincipal internal(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return principal;
    }
}
