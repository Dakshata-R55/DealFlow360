package com.dealflow360.customer.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.customer.dto.CustomerResponse;
import com.dealflow360.customer.service.CustomerService;
import com.dealflow360.shared.api.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, customerService.list(SecurityAuth.requireCompany(principal))));
    }

    private static AuthPrincipal internal(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return principal;
    }
}
