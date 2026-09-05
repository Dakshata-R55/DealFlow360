package com.dealflow360.policy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ApprovalPolicyReplaceRequest(@NotNull List<@Valid ApprovalPolicyRowRequest> policies) {}
