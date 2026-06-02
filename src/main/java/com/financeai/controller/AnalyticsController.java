package com.financeai.controller;

import com.financeai.dto.CategorySummaryResponse;
import com.financeai.dto.MonthlySummaryResponse;
import com.financeai.dto.SavingsResponse;
import com.financeai.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * Read-only analytics endpoints. All routes require a valid JWT.
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Returns income, expenses, net savings, and savings rate for the given month.
     * Defaults to the current month when the {@code month} parameter is omitted.
     *
     * @param month optional YYYY-MM string (e.g. "2024-03")
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserDetails userDetails) {
        String targetMonth = (month != null) ? month : YearMonth.now().toString();
        MonthlySummaryResponse summary = analyticsService.getMonthlySummary(
                userDetails.getUsername(), targetMonth);
        return ResponseEntity.ok(summary);
    }

    /**
     * Returns expense breakdown by category for the current month.
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategorySummaryResponse>> getCategorySummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CategorySummaryResponse> summaries = analyticsService.getCategorySummary(
                userDetails.getUsername());
        return ResponseEntity.ok(summaries);
    }

    /**
     * Returns overall savings statistics across all time.
     */
    @GetMapping("/savings")
    public ResponseEntity<SavingsResponse> getSavings(
            @AuthenticationPrincipal UserDetails userDetails) {
        SavingsResponse savings = analyticsService.getSavings(userDetails.getUsername());
        return ResponseEntity.ok(savings);
    }
}
