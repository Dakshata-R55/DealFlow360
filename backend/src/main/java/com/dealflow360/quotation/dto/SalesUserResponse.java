package com.dealflow360.quotation.dto;

import com.dealflow360.auth.model.User;
import com.dealflow360.auth.model.UserRole;

public record SalesUserResponse(long id, String name, String email, UserRole role) {

    public static SalesUserResponse from(User user) {
        return new SalesUserResponse(user.id(), user.name(), user.email(), user.role());
    }
}
