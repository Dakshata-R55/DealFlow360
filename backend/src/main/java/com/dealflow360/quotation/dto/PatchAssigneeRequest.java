package com.dealflow360.quotation.dto;

import jakarta.validation.constraints.NotNull;

public record PatchAssigneeRequest(@NotNull Long salesRepId) {}
