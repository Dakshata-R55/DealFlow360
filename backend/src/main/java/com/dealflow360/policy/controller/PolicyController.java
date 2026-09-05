package com.dealflow360.policy.controller;

import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.policy.dto.ApprovalPolicyReplaceRequest;
import com.dealflow360.policy.dto.ApprovalPolicyResponse;
import com.dealflow360.policy.dto.DiscountPolicyReplaceRequest;
import com.dealflow360.policy.dto.DiscountPolicyResponse;
import com.dealflow360.policy.service.PolicyService;
import com.dealflow360.shared.api.ApiResponse;
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
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/api/discount-policy")
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<DiscountPolicyResponse>>> listDiscount(Authentication authentication) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, policyService.listDiscountPolicies(principal.companyId())));
    }

    @PutMapping("/api/discount-policy")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<DiscountPolicyResponse>>> replaceDiscount(
            Authentication authentication, @Valid @RequestBody DiscountPolicyReplaceRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, policyService.replaceDiscountPolicies(principal.companyId(), request)));
    }

    @GetMapping("/api/approval-policy")
    @PreAuthorize("hasAnyAuthority('ADMIN','SALES_REP','SALES_MANAGER','FINANCE_OPS')")
    public ResponseEntity<ApiResponse<List<ApprovalPolicyResponse>>> listApproval(Authentication authentication) {
        AuthPrincipal principal = internal(authentication);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, policyService.listApprovalPolicies(principal.companyId())));
    }

    @PutMapping("/api/approval-policy")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<ApprovalPolicyResponse>>> replaceApproval(
            Authentication authentication, @Valid @RequestBody ApprovalPolicyReplaceRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK, policyService.replaceApprovalPolicies(principal.companyId(), request)));
    }

    private static AuthPrincipal internal(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        SecurityAuth.requireInternal(principal);
        return principal;
    }
}
