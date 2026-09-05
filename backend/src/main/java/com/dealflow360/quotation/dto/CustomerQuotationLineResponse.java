package com.dealflow360.quotation.dto;

import com.dealflow360.catalog.model.BillingType;
import java.math.BigDecimal;

public record CustomerQuotationLineResponse(
        long id,
        long productId,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPercent,
        BigDecimal lineTotal,
        BillingType billingType) {}
