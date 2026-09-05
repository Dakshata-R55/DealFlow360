package com.dealflow360.dashboard.service;

import com.dealflow360.auth.model.UserRole;
import com.dealflow360.auth.repository.UserRepository;
import com.dealflow360.dashboard.dto.DashboardResponse;
import com.dealflow360.dashboard.dto.DashboardResponse.DashboardActionResponse;
import com.dealflow360.dashboard.dto.DashboardResponse.DashboardActivityResponse;
import com.dealflow360.dashboard.dto.DashboardResponse.DashboardBarResponse;
import com.dealflow360.dashboard.dto.DashboardResponse.DashboardKpiResponse;
import com.dealflow360.dashboard.dto.DashboardResponse.DashboardTableRowResponse;
import com.dealflow360.dashboard.dto.SearchHitResponse;
import com.dealflow360.dashboard.repository.DashboardRepository;
import com.dealflow360.dashboard.repository.DashboardRepository.CountRow;
import com.dealflow360.dashboard.repository.DashboardRepository.ProductRow;
import com.dealflow360.dashboard.repository.DashboardRepository.QuoteRow;
import com.dealflow360.dashboard.repository.DashboardRepository.WarehouseRow;
import com.dealflow360.quotation.model.QuotationStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final List<QuotationStatus> PIPELINE =
            List.of(
                    QuotationStatus.DRAFT,
                    QuotationStatus.PENDING_APPROVAL,
                    QuotationStatus.NEGOTIATION,
                    QuotationStatus.APPROVED,
                    QuotationStatus.CONFIRMED);

    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;

    public DashboardService(DashboardRepository dashboardRepository, UserRepository userRepository) {
        this.dashboardRepository = dashboardRepository;
        this.userRepository = userRepository;
    }

    public DashboardResponse get(long companyId, long userId, UserRole role) {
        String name = userRepository.findById(userId).map(user -> user.name()).orElse("");
        if (role == UserRole.ADMIN) {
            return admin(companyId, name);
        }
        return sales(companyId, name, role);
    }

    public List<SearchHitResponse> search(long companyId, UserRole role, String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return List.of();
        }
        List<SearchHitResponse> hits = new ArrayList<>();
        if (role != UserRole.ADMIN) {
            for (QuoteRow row : dashboardRepository.searchQuotes(companyId, q, 5)) {
                hits.add(new SearchHitResponse(
                        "quotation", row.id(), row.quoteNumber() + " · " + row.customerName(), "/quotations?quote=" + row.id()));
            }
            for (CountRow row : dashboardRepository.searchNamed(
                    "SELECT id, name AS label FROM customers WHERE company_id = ? AND name LIKE ? ORDER BY name LIMIT ?",
                    companyId,
                    q,
                    4)) {
                hits.add(new SearchHitResponse("customer", row.count(), row.key(), "/quotations"));
            }
        }
        for (CountRow row : dashboardRepository.searchNamed(
                "SELECT id, name AS label FROM products WHERE company_id = ? AND name LIKE ? ORDER BY name LIMIT ?",
                companyId,
                q,
                5)) {
            String href = role == UserRole.ADMIN ? "/admin/products/" + row.count() : "/quotations";
            hits.add(new SearchHitResponse("product", row.count(), row.key(), href));
        }
        if (role == UserRole.ADMIN) {
            for (CountRow row : dashboardRepository.searchNamed(
                    "SELECT id, name AS label FROM warehouses WHERE company_id = ? AND name LIKE ? ORDER BY name LIMIT ?",
                    companyId,
                    q,
                    3)) {
                hits.add(new SearchHitResponse("warehouse", row.count(), row.key(), "/admin/warehouses"));
            }
        }
        return hits;
    }

    private DashboardResponse sales(long companyId, String userName, UserRole role) {
        long total = dashboardRepository.countQuotes(companyId);
        long pending = dashboardRepository.countQuotesByStatus(companyId, QuotationStatus.PENDING_APPROVAL.name());
        BigDecimal revenue = dashboardRepository.confirmedRevenueThisMonth(companyId);
        long customers = dashboardRepository.countActiveCustomers(companyId);
        Map<String, Long> byStatus = new HashMap<>();
        for (CountRow row : dashboardRepository.quoteCountsByStatus(companyId)) {
            byStatus.put(row.key(), row.count());
        }
        List<DashboardBarResponse> bars = new ArrayList<>();
        for (QuotationStatus status : PIPELINE) {
            bars.add(new DashboardBarResponse(
                    pipelineLabel(status), byStatus.getOrDefault(status.name(), 0L), "/quotations"));
        }
        List<QuoteRow> recent = dashboardRepository.recentQuotes(companyId, 8);
        List<DashboardActivityResponse> activity = new ArrayList<>();
        for (QuoteRow row : recent) {
            activity.add(new DashboardActivityResponse(
                    row.quoteNumber() + " · " + pipelineLabel(QuotationStatus.valueOf(row.status())),
                    row.customerName(),
                    "/quotations?quote=" + row.id(),
                    row.updatedAt()));
        }
        List<DashboardTableRowResponse> table = new ArrayList<>();
        for (QuoteRow row : recent.stream().limit(6).toList()) {
            table.add(new DashboardTableRowResponse(
                    row.quoteNumber(),
                    row.customerName(),
                    pipelineLabel(QuotationStatus.valueOf(row.status())),
                    rupee(row.totalAmount()),
                    row.status(),
                    "/quotations?quote=" + row.id(),
                    row.updatedAt()));
        }
        List<DashboardActionResponse> actions = new ArrayList<>();
        actions.add(new DashboardActionResponse("New quotation", "/quotations"));
        actions.add(new DashboardActionResponse("Open board", "/quotations"));
        if (role == UserRole.FINANCE_OPS) {
            actions.add(new DashboardActionResponse("Fulfillment", "/fulfillment"));
        }
        return new DashboardResponse(
                greeting(userName),
                "Here's what's happening with your sales today.",
                "New quotation",
                "/quotations",
                List.of(
                        new DashboardKpiResponse("Total quotations", String.valueOf(total), "/quotations"),
                        new DashboardKpiResponse("Pending approvals", String.valueOf(pending), "/quotations"),
                        new DashboardKpiResponse("Revenue (this month)", rupee(revenue), "/quotations"),
                        new DashboardKpiResponse("Active customers", String.valueOf(customers), "/quotations")),
                "Quotation pipeline",
                bars,
                activity,
                "Recent quotations",
                table,
                actions);
    }

    private DashboardResponse admin(long companyId, String userName) {
        List<DashboardBarResponse> bars = new ArrayList<>();
        for (CountRow row : dashboardRepository.productCountsByCategory(companyId)) {
            bars.add(new DashboardBarResponse(row.key(), row.count(), "/admin/catalog"));
        }
        List<DashboardActivityResponse> activity = new ArrayList<>();
        for (ProductRow row : dashboardRepository.recentProducts(companyId, 5)) {
            activity.add(new DashboardActivityResponse(
                    row.name() + " updated",
                    row.categoryName(),
                    "/admin/products/" + row.id(),
                    row.updatedAt()));
        }
        for (WarehouseRow row : dashboardRepository.recentWarehouses(companyId, 3)) {
            activity.add(new DashboardActivityResponse(
                    row.name() + " warehouse", row.location(), "/admin/warehouses", row.updatedAt()));
        }
        activity.sort((a, b) -> b.at().compareTo(a.at()));
        if (activity.size() > 8) {
            activity = new ArrayList<>(activity.subList(0, 8));
        }
        List<DashboardTableRowResponse> table = new ArrayList<>();
        for (ProductRow row : dashboardRepository.recentProducts(companyId, 6)) {
            table.add(new DashboardTableRowResponse(
                    "P-" + row.id(),
                    row.name(),
                    row.categoryName(),
                    rupee(row.basePrice()),
                    row.billingType(),
                    "/admin/products/" + row.id(),
                    row.updatedAt()));
        }
        return new DashboardResponse(
                greeting(userName),
                "Here's what's happening with your catalog today.",
                "Add product",
                "/admin/catalog",
                List.of(
                        new DashboardKpiResponse(
                                "Products", String.valueOf(dashboardRepository.countProducts(companyId)), "/admin/catalog"),
                        new DashboardKpiResponse(
                                "Categories",
                                String.valueOf(dashboardRepository.countCategories(companyId)),
                                "/admin/catalog"),
                        new DashboardKpiResponse(
                                "Warehouses",
                                String.valueOf(dashboardRepository.countWarehouses(companyId)),
                                "/admin/warehouses"),
                        new DashboardKpiResponse(
                                "Plans", String.valueOf(dashboardRepository.countPlans(companyId)), "/admin/plans")),
                "Products by category",
                bars,
                activity,
                "Recent products",
                table,
                List.of(
                        new DashboardActionResponse("Catalog", "/admin/catalog"),
                        new DashboardActionResponse("Policies", "/admin/policies"),
                        new DashboardActionResponse("Warehouses", "/admin/warehouses"),
                        new DashboardActionResponse("Plans", "/admin/plans")));
    }

    private static String greeting(String name) {
        int hour = ZonedDateTime.now(ZONE).getHour();
        String when = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        String first = name == null || name.isBlank() ? "" : name.split("\\s+")[0];
        return first.isEmpty() ? when : when + ", " + first;
    }

    private static String pipelineLabel(QuotationStatus status) {
        return switch (status) {
            case DRAFT -> "Draft";
            case PENDING_APPROVAL -> "Pending approval";
            case NEGOTIATION -> "Negotiation";
            case APPROVED -> "Approved";
            case CONFIRMED -> "Confirmed";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
        };
    }

    private static String rupee(BigDecimal amount) {
        if (amount == null) {
            return "₹0";
        }
        return "₹" + amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
