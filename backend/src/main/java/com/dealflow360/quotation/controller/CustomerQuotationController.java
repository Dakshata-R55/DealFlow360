package com.dealflow360.quotation.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.quotation.dto.CustomerCounterRequest;
import com.dealflow360.quotation.dto.CustomerQuotationResponse;
import com.dealflow360.quotation.service.CustomerQuotationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/quotations")
@PreAuthorize("hasAuthority('CUSTOMER')")
public class CustomerQuotationController {

    private final CustomerQuotationService customerQuotationService;

    public CustomerQuotationController(CustomerQuotationService customerQuotationService) {
        this.customerQuotationService = customerQuotationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerQuotationResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, customerQuotationService.list(principal.userId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerQuotationResponse>> get(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, customerQuotationService.get(principal.userId(), id)));
    }

    @PostMapping("/{id}/counter")
    public ResponseEntity<ApiResponse<CustomerQuotationResponse>> counter(
            Authentication authentication, @PathVariable long id, @Valid @RequestBody CustomerCounterRequest body) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, customerQuotationService.counter(principal.userId(), id, body)));
    }

    @PostMapping("/{id}/confirm-credit")
    public ResponseEntity<ApiResponse<CustomerQuotationResponse>> confirmCredit(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, customerQuotationService.confirmCredit(principal.userId(), id)));
    }
}
