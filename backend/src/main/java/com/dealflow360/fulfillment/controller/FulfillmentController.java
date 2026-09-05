package com.dealflow360.fulfillment.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.fulfillment.dto.FulfillmentListItemResponse;
import com.dealflow360.fulfillment.dto.FulfillmentOverrideRequest;
import com.dealflow360.fulfillment.dto.FulfillmentPlanResponse;
import com.dealflow360.fulfillment.service.FulfillmentService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping("/api/fulfillment")
    @PreAuthorize("hasAnyAuthority('SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<FulfillmentListItemResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, fulfillmentService.list(SecurityAuth.requireCompany(principal))));
    }

    @GetMapping("/api/quotations/{id}/fulfillment")
    @PreAuthorize("hasAnyAuthority('SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<FulfillmentPlanResponse>> get(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, fulfillmentService.get(SecurityAuth.requireCompany(principal), id)));
    }

    @PostMapping("/api/quotations/{id}/fulfillment/auto")
    @PreAuthorize("hasAuthority('FINANCE_OPS')")
    public ResponseEntity<ApiResponse<FulfillmentPlanResponse>> auto(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, fulfillmentService.auto(SecurityAuth.requireCompany(principal), id)));
    }

    @PutMapping("/api/quotations/{id}/fulfillment")
    @PreAuthorize("hasAuthority('FINANCE_OPS')")
    public ResponseEntity<ApiResponse<FulfillmentPlanResponse>> override(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody FulfillmentOverrideRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, fulfillmentService.override(SecurityAuth.requireCompany(principal), id, request)));
    }

    @PostMapping("/api/quotations/{id}/fulfillment/consolidate-backorder")
    @PreAuthorize("hasAuthority('FINANCE_OPS')")
    public ResponseEntity<ApiResponse<FulfillmentPlanResponse>> consolidate(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, fulfillmentService.consolidateBackorder(SecurityAuth.requireCompany(principal), id)));
    }
}
