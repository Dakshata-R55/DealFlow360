package com.dealflow360.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String name, Boolean active) {}
