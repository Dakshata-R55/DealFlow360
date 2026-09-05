package com.dealflow360.quoterequest.model;

import java.time.Instant;

public record CompanyCustomer(
        long id,
        long sellerCompanyId,
        long customerUserId,
        long customerTierId,
        long sellerCustomerId,
        String status,
        Instant createdAt,
        Instant updatedAt) {}
