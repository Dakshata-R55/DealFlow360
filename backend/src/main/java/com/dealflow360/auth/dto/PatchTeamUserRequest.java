package com.dealflow360.auth.dto;

import jakarta.validation.constraints.NotNull;

public record PatchTeamUserRequest(@NotNull Boolean active) {}
