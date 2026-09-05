package com.dealflow360.shared.api;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ApiResponse<T>(boolean success, int status, T data, Instant timestamp) {

    public static <T> ApiResponse<T> of(boolean success, HttpStatus httpStatus, T data) {
        return new ApiResponse<>(success, httpStatus.value(), data, Instant.now());
    }

    public static <T> ApiResponse<T> success(HttpStatus httpStatus, T data) {
        return of(true, httpStatus, data);
    }

    public static <T> ApiResponse<T> failure(HttpStatus httpStatus, T data) {
        return of(false, httpStatus, data);
    }
}
