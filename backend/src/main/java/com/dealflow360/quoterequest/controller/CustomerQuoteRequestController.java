package com.dealflow360.quoterequest.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.quoterequest.dto.CreateQuoteRequestBody;
import com.dealflow360.quoterequest.dto.PatchQuoteRequestBody;
import com.dealflow360.quoterequest.dto.PatchQuoteRequestLineBody;
import com.dealflow360.quoterequest.dto.QuoteRequestLineBody;
import com.dealflow360.quoterequest.dto.QuoteRequestResponse;
import com.dealflow360.quoterequest.service.QuoteRequestService;
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
@RequestMapping("/api/customer/requests")
@PreAuthorize("hasAuthority('CUSTOMER')")
public class CustomerQuoteRequestController {

    private final QuoteRequestService quoteRequestService;

    public CustomerQuoteRequestController(QuoteRequestService quoteRequestService) {
        this.quoteRequestService = quoteRequestService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> create(
            Authentication authentication, @Valid @RequestBody CreateQuoteRequestBody body) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, quoteRequestService.create(principal.userId(), body)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<QuoteRequestResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quoteRequestService.listForCustomer(principal.userId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> get(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quoteRequestService.getForCustomer(principal.userId(), id)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> patch(
            Authentication authentication, @PathVariable long id, @Valid @RequestBody PatchQuoteRequestBody body) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quoteRequestService.patch(principal.userId(), id, body)));
    }

    @PostMapping("/{id}/lines")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> addLine(
            Authentication authentication, @PathVariable long id, @Valid @RequestBody QuoteRequestLineBody body) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED, quoteRequestService.addLine(principal.userId(), id, body)));
    }

    @PatchMapping("/{id}/lines/{lineId}")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> updateLine(
            Authentication authentication,
            @PathVariable long id,
            @PathVariable long lineId,
            @Valid @RequestBody PatchQuoteRequestLineBody body) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, quoteRequestService.updateLine(principal.userId(), id, lineId, body)));
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> deleteLine(
            Authentication authentication, @PathVariable long id, @PathVariable long lineId) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quoteRequestService.deleteLine(principal.userId(), id, lineId)));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> submit(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quoteRequestService.submit(principal.userId(), id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> cancel(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quoteRequestService.cancel(principal.userId(), id)));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<QuoteRequestResponse>> withdraw(
            Authentication authentication, @PathVariable long id) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, quoteRequestService.withdraw(principal.userId(), id)));
    }
}
