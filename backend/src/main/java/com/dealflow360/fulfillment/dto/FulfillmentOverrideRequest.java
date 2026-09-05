package com.dealflow360.fulfillment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FulfillmentOverrideRequest(@NotNull Long lineId, @NotEmpty @Valid List<FulfillmentOverrideRow> rows) {}
