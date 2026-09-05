package com.dealflow360.quotation.dto;

import com.dealflow360.policy.model.RiskLevel;
import com.dealflow360.quotation.model.Quotation;
import com.dealflow360.quotation.model.QuotationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuotationResponse(
        long id,
        String quoteNumber,
        long customerId,
        String customerName,
        long customerTierId,
        String customerTierName,
        long salesRepId,
        long priceListId,
        String priceListName,
        QuotationStatus status,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal totalCost,
        BigDecimal marginAmount,
        BigDecimal marginPercent,
        BigDecimal riskScore,
        RiskLevel riskLevel,
        LikelyRoute likelyRoute,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        List<QuotationLineResponse> lines) {

    public record LikelyRoute(boolean requiresManager, boolean requiresFinance) {}

    public static QuotationResponse from(
            Quotation quotation,
            String customerName,
            long customerTierId,
            String customerTierName,
            String priceListName,
            LikelyRoute likelyRoute,
            List<QuotationLineResponse> lines) {
        return new QuotationResponse(
                quotation.id(),
                quotation.quoteNumber(),
                quotation.customerId(),
                customerName,
                customerTierId,
                customerTierName,
                quotation.salesRepId(),
                quotation.priceListId(),
                priceListName,
                quotation.status(),
                quotation.subtotal(),
                quotation.discountAmount(),
                quotation.totalAmount(),
                quotation.totalCost(),
                quotation.marginAmount(),
                quotation.marginPercent(),
                quotation.riskScore(),
                quotation.riskLevel(),
                likelyRoute,
                quotation.createdAt(),
                quotation.updatedAt(),
                quotation.submittedAt(),
                lines);
    }
}
