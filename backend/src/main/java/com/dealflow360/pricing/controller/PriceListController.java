package com.dealflow360.pricing.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.pricing.dto.PriceListItemRequest;
import com.dealflow360.pricing.dto.PriceListItemResponse;
import com.dealflow360.pricing.dto.PriceListRequest;
import com.dealflow360.pricing.dto.PriceListResponse;
import com.dealflow360.pricing.service.PricingService;
import com.dealflow360.shared.api.ApiResponse;
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
@RequestMapping("/api/price-lists")
public class PriceListController {

    private final PricingService pricingService;

    public PriceListController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<PriceListResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, pricingService.listPriceLists(principal.companyId())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PriceListResponse>> create(
            Authentication authentication, @Valid @RequestBody PriceListRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED, pricingService.createPriceList(principal.companyId(), request)));
    }

    @PutMapping("/{id}/items/{productId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<PriceListItemResponse>> upsertItem(
            Authentication authentication,
            @PathVariable long id,
            @PathVariable long productId,
            @Valid @RequestBody PriceListItemRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, pricingService.upsertItem(principal.companyId(), id, productId, request)));
    }

    private static AuthPrincipal internal(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return principal;
    }
}
