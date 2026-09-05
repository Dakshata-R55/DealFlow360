package com.dealflow360.quotation.dto;

import jakarta.validation.constraints.NotNull;

public record CreateQuotationRequest(@NotNull Long customerId) {}
