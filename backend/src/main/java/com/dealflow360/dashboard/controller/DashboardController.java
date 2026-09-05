package com.dealflow360.dashboard.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.dashboard.dto.DashboardResponse;
import com.dealflow360.dashboard.dto.SearchHitResponse;
import com.dealflow360.dashboard.service.DashboardService;
import com.dealflow360.shared.api.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> get(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                dashboardService.get(SecurityAuth.requireCompany(principal), principal.userId(), principal.role())));
    }

    @GetMapping("/api/search")
    public ResponseEntity<ApiResponse<List<SearchHitResponse>>> search(
            Authentication authentication, @RequestParam(name = "q", defaultValue = "") String q) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                dashboardService.search(SecurityAuth.requireCompany(principal), principal.role(), q)));
    }
}
