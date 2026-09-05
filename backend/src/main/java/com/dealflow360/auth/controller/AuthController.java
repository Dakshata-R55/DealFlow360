package com.dealflow360.auth.controller;

import com.dealflow360.auth.dto.AuthSessionResponse;
import com.dealflow360.auth.dto.AuthUserResponse;
import com.dealflow360.auth.dto.LoginRequest;
import com.dealflow360.auth.dto.SignupRequest;
import com.dealflow360.auth.security.AuthPrincipal;
import com.dealflow360.auth.service.AuthService;
import com.dealflow360.shared.api.ApiResponse;
import com.dealflow360.shared.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> signup(@Valid @RequestBody SignupRequest request) {
        AuthSessionResponse session = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(HttpStatus.CREATED, session));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthSessionResponse session = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, session));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new UnauthorizedException("Unauthorized");
        }
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, authService.currentUser(principal)));
    }
}