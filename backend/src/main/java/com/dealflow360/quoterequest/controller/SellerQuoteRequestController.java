package com.dealflow360.quoterequest.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.quotation.dto.QuotationResponse;
import com.dealflow360.quoterequest.dto.QuoteRequestResponse;
import com.dealflow360.quoterequest.service.QuoteRequestService;
import com.dealflow360.shared.api.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
public class SellerQuoteRequestController {

    private final QuoteRequestService quoteRequestService;

    public SellerQuoteRequestController(QuoteRequestService quoteRequestService) {
        this.quoteRequestService = quoteRequestService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<QuoteRequestResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = seller(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, quoteRequestService.listForSeller(SecurityAuth.requireCompany(principal))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> get(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = seller(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, quoteRequestService.getForSeller(SecurityAuth.requireCompany(principal), id)));
    }

    @PostMapping("/{id}/convert-to-quotation")
    @PreAuthorize("hasAnyAuthority('SALES_REP','SALES_MANAGER')")
    public ResponseEntity<ApiResponse<QuotationResponse>> convert(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = seller(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                quoteRequestService.convertToQuotation(
                        SecurityAuth.requireCompany(principal), principal.userId(), id)));
    }

    private static AuthPrincipal seller(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return principal;
    }
}
