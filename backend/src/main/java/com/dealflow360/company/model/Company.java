package com.dealflow360.company.model;

import java.time.Instant;

public record Company(
        long id,
        String name,
        String code,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
