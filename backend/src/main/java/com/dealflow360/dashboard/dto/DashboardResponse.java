package com.dealflow360.dashboard.dto;

import java.time.Instant;
import java.util.List;

public record DashboardResponse(
        String greeting,
        String subtitle,
        String primaryCtaLabel,
        String primaryCtaHref,
        List<DashboardKpiResponse> kpis,
        String chartTitle,
        List<DashboardBarResponse> bars,
        List<DashboardActivityResponse> activity,
        String tableTitle,
        List<DashboardTableRowResponse> table,
        List<DashboardActionResponse> actions) {

    public record DashboardKpiResponse(String label, String value, String href) {}

    public record DashboardBarResponse(String label, long count, String href) {}

    public record DashboardActivityResponse(String title, String subtitle, String href, Instant at) {}

    public record DashboardTableRowResponse(
            String idLabel,
            String primary,
            String secondary,
            String amount,
            String status,
            String href,
            Instant updatedAt) {}

    public record DashboardActionResponse(String label, String href) {}
}
