package com.dealflow360.catalog.dto;

import com.dealflow360.catalog.model.ProductVariant;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductVariantResponse(
        long id,
        long productId,
        String attributeName,
        String attributeValue,
        BigDecimal extraPrice,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.id(),
                variant.productId(),
                variant.attributeName(),
                variant.attributeValue(),
                variant.extraPrice(),
                variant.createdAt(),
                variant.updatedAt());
    }
}
