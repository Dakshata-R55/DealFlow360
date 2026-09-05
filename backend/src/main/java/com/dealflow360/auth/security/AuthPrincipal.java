package com.dealflow360.auth.security;

import com.dealflow360.auth.model.UserRole;

public record AuthPrincipal(long userId, long companyId, UserRole role) {}