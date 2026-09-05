package com.dealflow360.auth.dto;

public record AuthSessionResponse(String accessToken, String tokenType, long expiresIn, AuthUserResponse user) {}