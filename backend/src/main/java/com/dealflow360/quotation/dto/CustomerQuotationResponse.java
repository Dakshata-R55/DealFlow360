package com.dealflow360.quotation.dto;

import com.dealflow360.quotation.model.QuotationStatus;
import java.math.BigDecimal;
import java.util.List;

public record CustomerQuotationResponse(
        long id,
        String quoteNumber,
        long sellerCompanyId,
        String sellerCompanyName,
        QuotationStatus status,
        String statusLabel,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        long sourceRequestId,
        String sourceRequestNumber,
        BigDecimal expectedDiscountPercent,
        List<CustomerQuotationLineResponse> lines) {}
