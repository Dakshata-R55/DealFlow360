package com.dealflow360.quotation.dto;

import java.math.BigDecimal;

public record RecommendationResponse(
        long productId, String productName, boolean promotion, BigDecimal marginDelta, BigDecimal score) {}
