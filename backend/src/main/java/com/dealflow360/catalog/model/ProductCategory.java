package com.dealflow360.catalog.model;

import java.time.Instant;

public record ProductCategory(
        long id, long companyId, String name, boolean active, Instant createdAt, Instant updatedAt) {}
