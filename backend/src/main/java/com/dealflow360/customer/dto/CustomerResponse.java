package com.dealflow360.customer.dto;

import com.dealflow360.customer.model.Customer;
import java.time.Instant;

public record CustomerResponse(
        long id, String name, long customerTierId, boolean active, Instant createdAt, Instant updatedAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id(),
                customer.name(),
                customer.customerTierId(),
                customer.active(),
                customer.createdAt(),
                customer.updatedAt());
    }
}
