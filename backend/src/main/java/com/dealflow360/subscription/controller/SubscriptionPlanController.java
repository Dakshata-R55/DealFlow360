package com.dealflow360.subscription.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.shared.api.ApiResponse;
import com.dealflow360.subscription.dto.SubscriptionPlanRequest;
import com.dealflow360.subscription.dto.SubscriptionPlanResponse;
import com.dealflow360.subscription.service.SubscriptionPlanService;
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
@RequestMapping("/api/subscription-plans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    public SubscriptionPlanController(SubscriptionPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, planService.list(SecurityAuth.requireCompany(principal))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> create(
            Authentication authentication, @Valid @RequestBody SubscriptionPlanRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, planService.create(SecurityAuth.requireCompany(principal), request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> update(
            Authentication authentication, @PathVariable long id, @Valid @RequestBody SubscriptionPlanRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, planService.update(SecurityAuth.requireCompany(principal), id, request)));
    }

    private static AuthPrincipal internal(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return principal;
    }
}
