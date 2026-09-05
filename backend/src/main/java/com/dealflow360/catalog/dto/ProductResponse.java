package com.dealflow360.catalog.dto;

import com.dealflow360.catalog.model.BillingType;
import com.dealflow360.catalog.model.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        long id,
        long categoryId,
        String name,
        String description,
        String unit,
        BigDecimal basePrice,
        BigDecimal costPrice,
        BigDecimal taxPercent,
        BillingType billingType,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<ProductVariantResponse> variants) {

    public static ProductResponse from(Product product, List<ProductVariantResponse> variants) {
        return new ProductResponse(
                product.id(),
                product.categoryId(),
                product.name(),
                product.description(),
                product.unit(),
                product.basePrice(),
                product.costPrice(),
                product.taxPercent(),
                product.billingType(),
                product.active(),
                product.createdAt(),
                product.updatedAt(),
                variants);
    }
}
