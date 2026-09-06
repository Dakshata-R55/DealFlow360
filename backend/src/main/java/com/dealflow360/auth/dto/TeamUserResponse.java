package com.dealflow360.auth.dto;

import com.dealflow360.auth.model.UserRole;

public record TeamUserResponse(long id, String name, String email, UserRole role, boolean active) {}
