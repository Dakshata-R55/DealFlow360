package com.dealflow360.auth.security;

import com.dealflow360.auth.model.UserRole;
import com.dealflow360.shared.exception.ForbiddenException;
import com.dealflow360.shared.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;

public final class SecurityAuth {

    private SecurityAuth() {}

    public static AuthPrincipal require(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new UnauthorizedException("Unauthorized");
        }
        return principal;
    }

    public static void requireInternal(AuthPrincipal principal) {
        if (principal.role() == UserRole.CUSTOMER) {
            throw new ForbiddenException("Access denied");
        }
    }

    public static long requireCompany(AuthPrincipal principal) {
        if (principal.companyId() == null) {
            throw new ForbiddenException("Access denied");
        }
        return principal.companyId();
    }
}
