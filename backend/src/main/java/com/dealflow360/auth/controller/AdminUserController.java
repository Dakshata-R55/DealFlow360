package com.dealflow360.auth.controller;

import com.dealflow360.auth.dto.CreateTeamUserRequest;
import com.dealflow360.auth.dto.PatchTeamUserRequest;
import com.dealflow360.auth.dto.TeamUserResponse;
import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.security.SecurityAuth;
import com.dealflow360.auth.service.AdminUserService;
import com.dealflow360.shared.api.ApiResponse;
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
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<TeamUserResponse>>> list(Authentication authentication) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        long companyId = SecurityAuth.requireCompany(principal);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, adminUserService.list(companyId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<TeamUserResponse>> create(
            Authentication authentication, @Valid @RequestBody CreateTeamUserRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        long companyId = SecurityAuth.requireCompany(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, adminUserService.create(companyId, request)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<TeamUserResponse>> updateActive(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody PatchTeamUserRequest request) {
        AuthPrincipal principal = SecurityAuth.require(authentication);
        long companyId = SecurityAuth.requireCompany(principal);
        return ResponseEntity.ok(ApiResponse.success(
                HttpStatus.OK,
                adminUserService.updateActive(companyId, principal.userId(), id, request.active())));
    }
}
