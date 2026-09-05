package com.dealflow360.upsell.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.shared.api.ApiResponse;
import com.dealflow360.upsell.dto.UpsellRuleRequest;
import com.dealflow360.upsell.dto.UpsellRuleResponse;
import com.dealflow360.upsell.service.UpsellRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/upsell-rules")
public class UpsellRuleController {

    private final UpsellRuleService upsellRuleService;

    public UpsellRuleController(UpsellRuleService upsellRuleService) {
        this.upsellRuleService = upsellRuleService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<UpsellRuleResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, upsellRuleService.list(principal.companyId())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<UpsellRuleResponse>> create(
            Authentication authentication, @Valid @RequestBody UpsellRuleRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED, upsellRuleService.create(principal.companyId(), request)));
    }

    private static AuthPrincipal internal(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return principal;
    }
}
