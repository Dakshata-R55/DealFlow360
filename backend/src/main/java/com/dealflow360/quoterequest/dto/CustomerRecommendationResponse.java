package com.dealflow360.quoterequest.dto;

import java.math.BigDecimal;

public record CustomerRecommendationResponse(
        long productId, String productName, boolean promotion, BigDecimal unitPrice) {}
