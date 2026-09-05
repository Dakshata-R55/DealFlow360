package com.dealflow360.auth.dto;

import com.dealflow360.auth.model.UserRole;

public record AuthUserResponse(
        long id, String name, String email, UserRole role, long companyId, String companyName) {}