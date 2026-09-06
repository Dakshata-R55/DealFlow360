package com.dealflow360.standing.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.shared.api.ApiResponse;
import com.dealflow360.standing.dto.StandingProgressResponse;
import com.dealflow360.standing.dto.StandingRuleRequest;
import com.dealflow360.standing.dto.StandingRuleResponse;
import com.dealflow360.standing.service.StandingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StandingController {

    private final StandingService standingService;

    public StandingController(StandingService standingService) {
        this.standingService = standingService;
    }

    @GetMapping("/api/standing-rules")
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<StandingRuleResponse>> get(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, standingService.getRule(SecurityAuth.requireCompany(principal))));
    }

    @PutMapping("/api/standing-rules")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<StandingRuleResponse>> save(
            Authentication authentication, @Valid @RequestBody StandingRuleRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, standingService.saveRule(SecurityAuth.requireCompany(principal), request)));
    }

    @GetMapping("/api/customer/standing")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<StandingProgressResponse>>> customerStanding(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, standingService.progressForCustomer(principal.userId())));
    }
}
