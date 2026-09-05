package com.dealflow360.auth.model;

import java.time.Instant;

public record User(
        long id,
        Long companyId,
        String name,
        String email,
        String passwordHash,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}