package com.dealflow360.quoterequest.dto;

import jakarta.validation.constraints.NotNull;

public record CreateQuoteRequestBody(@NotNull Long sellerCompanyId) {}
