package com.dealflow360.quotation.dto;

import com.dealflow360.catalog.model.BillingType;
import com.dealflow360.quotation.model.QuotationLine;
import java.math.BigDecimal;

public record QuotationLineResponse(
        long id,
        long productId,
        String productName,
        Long variantId,
        String variantLabel,
        BigDecimal quantity,
        BigDecimal baseUnitPrice,
        BigDecimal resolvedUnitPrice,
        BigDecimal costPrice,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        BigDecimal allowedDiscountPercent,
        BigDecimal excess,
        BigDecimal lineTotal,
        BigDecimal marginAmount,
        BigDecimal marginPercent,
        BillingType billingType) {

    public static QuotationLineResponse from(QuotationLine line, String productName, String variantLabel) {
        BigDecimal excess = line.discountPercent().subtract(line.allowedDiscountPercent()).max(BigDecimal.ZERO);
        return new QuotationLineResponse(
                line.id(),
                line.productId(),
                productName,
                line.variantId(),
                variantLabel,
                line.quantity(),
                line.baseUnitPrice(),
                line.resolvedUnitPrice(),
                line.costPrice(),
                line.discountPercent(),
                line.discountAmount(),
                line.allowedDiscountPercent(),
                excess,
                line.lineTotal(),
                line.marginAmount(),
                line.marginPercent(),
                line.billingType());
    }
}
