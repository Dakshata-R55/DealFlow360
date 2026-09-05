package com.dealflow360.quotation.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.quotation.dto.AddQuotationLineRequest;
import com.dealflow360.quotation.dto.CreateQuotationRequest;
import com.dealflow360.quotation.dto.PatchQuotationLineRequest;
import com.dealflow360.quotation.dto.QuotationResponse;
import com.dealflow360.quotation.dto.RecommendationResponse;
import com.dealflow360.quotation.service.QuotationService;
import com.dealflow360.shared.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quotations")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, quotationService.list(SecurityAuth.requireCompany(principal))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALES_REP')")
    public ResponseEntity<ApiResponse<QuotationResponse>> create(
            Authentication authentication, @Valid @RequestBody CreateQuotationRequest request) {
        AuthPrincipal principal = writer(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED,
                        quotationService.create(SecurityAuth.requireCompany(principal), principal.userId(), request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<QuotationResponse>> get(Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quotationService.get(SecurityAuth.requireCompany(principal), id)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('SALES_REP')")
    public ResponseEntity<ApiResponse<QuotationResponse>> saveDraft(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = writer(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quotationService.saveDraft(SecurityAuth.requireCompany(principal), id)));
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('SALES_REP')")
    public ResponseEntity<ApiResponse<QuotationResponse>> addLine(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody AddQuotationLineRequest request) {
        AuthPrincipal principal = writer(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED, quotationService.addLine(SecurityAuth.requireCompany(principal), id, request)));
    }

    @PatchMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('SALES_REP')")
    public ResponseEntity<ApiResponse<QuotationResponse>> updateLine(
            Authentication authentication,
            @PathVariable long id,
            @PathVariable long lineId,
            @Valid @RequestBody PatchQuotationLineRequest request) {
        AuthPrincipal principal = writer(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, quotationService.updateLine(SecurityAuth.requireCompany(principal), id, lineId, request)));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('SALES_REP')")
    public ResponseEntity<ApiResponse<QuotationResponse>> deleteLine(
            Authentication authentication, @PathVariable long id, @PathVariable long lineId) {
        AuthPrincipal principal = writer(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quotationService.deleteLine(SecurityAuth.requireCompany(principal), id, lineId)));
    }

    @PostMapping("/{id}/evaluate")
    @PreAuthorize("hasAuthority('SALES_REP')")
    public ResponseEntity<ApiResponse<QuotationResponse>> evaluate(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = writer(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quotationService.evaluate(SecurityAuth.requireCompany(principal), id)));
    }

    @GetMapping("/{id}/recommendations")
    @PreAuthorize("hasAnyAuthority('SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> recommendations(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, quotationService.recommendations(SecurityAuth.requireCompany(principal), id)));
    }

    @PostMapping("/{id}/recommendations/{productId}/dismiss")
    @PreAuthorize("hasAuthority('SALES_REP')")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> dismiss(
            Authentication authentication, @PathVariable long id, @PathVariable long productId) {
        AuthPrincipal principal = writer(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, quotationService.dismissRecommendation(SecurityAuth.requireCompany(principal), id, productId)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('SALES_REP')")
    public ResponseEntity<ApiResponse<QuotationResponse>> submit(Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = writer(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quotationService.submit(SecurityAuth.requireCompany(principal), id)));
    }

    private static AuthPrincipal internal(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return principal;
    }

    private static AuthPrincipal writer(Authentication authentication) {
        return internal(authentication);
    }
}
