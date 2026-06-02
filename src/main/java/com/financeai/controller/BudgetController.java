package com.financeai.controller;

import com.financeai.dto.BudgetRequest;
import com.financeai.dto.BudgetResponse;
import com.financeai.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Budget management endpoints. All routes require a valid JWT.
 */
@RestController
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Creates a budget entry for a category and month.
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> create(
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        BudgetResponse response = budgetService.create(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all budgets for the authenticated user.
     * Optionally filter by month using the {@code month} query parameter (YYYY-MM).
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAll(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<BudgetResponse> budgets = (month != null)
                ? budgetService.getByMonth(email, month)
                : budgetService.getAll(email);
        return ResponseEntity.ok(budgets);
    }
}
