package com.dealflow360.catalog.dto;

import com.dealflow360.catalog.model.ProductCategory;
import java.time.Instant;

public record CategoryResponse(long id, String name, boolean active, Instant createdAt, Instant updatedAt) {

    public static CategoryResponse from(ProductCategory category) {
        return new CategoryResponse(
                category.id(), category.name(), category.active(), category.createdAt(), category.updatedAt());
    }
}
