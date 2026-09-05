package com.dealflow360.marketplace.dto;

import java.util.List;

public record SellerCompanyResponse(long id, String name, String code, String description, List<String> categories) {}
