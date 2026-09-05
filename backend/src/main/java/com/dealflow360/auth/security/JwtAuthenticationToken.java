package com.dealflow360.auth.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthPrincipal principal;
    private final String token;

    public JwtAuthenticationToken(AuthPrincipal principal, String token) {
        super(List.of(new SimpleGrantedAuthority(principal.role().name())));
        this.principal = principal;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public AuthPrincipal getPrincipal() {
        return principal;
    }
}