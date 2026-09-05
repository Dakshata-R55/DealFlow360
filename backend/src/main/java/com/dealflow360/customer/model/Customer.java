package com.dealflow360.customer.model;

import java.time.Instant;

public record Customer(
        long id,
        long companyId,
        String name,
        long customerTierId,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
