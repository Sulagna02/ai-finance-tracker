package com.financeai.service;

import com.financeai.dto.CategorySummaryResponse;
import com.financeai.dto.MonthlySummaryResponse;
import com.financeai.dto.SavingsResponse;
import com.financeai.entity.TransactionType;
import com.financeai.entity.User;
import com.financeai.exception.ResourceNotFoundException;
import com.financeai.repository.TransactionRepository;
import com.financeai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Produces financial analytics aggregated over user transactions.
 */
@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public AnalyticsService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Computes total income, total expenses, net savings, and savings rate for a given month.
     *
     * @param email email of the authenticated user
     * @param month target month in YYYY-MM format (e.g. "2024-03")
     * @return monthly summary DTO
     */
    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(String email, String month) {
        User user = loadUser(email);
        YearMonth yearMonth = YearMonth.parse(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository
                .sumAmountByUserAndTypeAndDateBetween(user, TransactionType.INCOME, start, end);
        BigDecimal totalExpenses = transactionRepository
                .sumAmountByUserAndTypeAndDateBetween(user, TransactionType.EXPENSE, start, end);

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        double savingsRate = calculateSavingsRate(totalIncome, netSavings);

        return MonthlySummaryResponse.builder()
                .month(month)
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(netSavings)
                .savingsRatePercent(savingsRate)
                .build();
    }

    /**
     * Returns expense breakdown by category for the current month, with each category's
     * percentage share of total expenses.
     *
     * @param email email of the authenticated user
     * @return list of category summary DTOs ordered by amount descending
     */
    @Transactional(readOnly = true)
    public List<CategorySummaryResponse> getCategorySummary(String email) {
        User user = loadUser(email);
        YearMonth currentMonth = YearMonth.now();
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        List<Object[]> rows = transactionRepository.sumExpensesByCategoryAndDateBetween(user, start, end);

        BigDecimal grandTotal = rows.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> {
                    String category = (String) row[0];
                    BigDecimal total = (BigDecimal) row[1];
                    double pct = grandTotal.compareTo(BigDecimal.ZERO) == 0
                            ? 0.0
                            : total.divide(grandTotal, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue();
                    return CategorySummaryResponse.builder()
                            .category(category)
                            .totalAmount(total)
                            .percentageOfExpenses(pct)
                            .build();
                })
                .toList();
    }

    /**
     * Computes the overall savings summary across all time for the authenticated user.
     *
     * @param email email of the authenticated user
     * @return savings response DTO
     */
    @Transactional(readOnly = true)
    public SavingsResponse getSavings(String email) {
        User user = loadUser(email);

        LocalDate start = LocalDate.of(2000, 1, 1);
        LocalDate end = LocalDate.now();

        BigDecimal totalIncome = transactionRepository
                .sumAmountByUserAndTypeAndDateBetween(user, TransactionType.INCOME, start, end);
        BigDecimal totalExpenses = transactionRepository
                .sumAmountByUserAndTypeAndDateBetween(user, TransactionType.EXPENSE, start, end);

        BigDecimal netSavings = totalIncome.subtract(totalExpenses);
        double savingsRate = calculateSavingsRate(totalIncome, netSavings);

        return SavingsResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netSavings(netSavings)
                .savingsRatePercent(savingsRate)
                .period("all-time")
                .build();
    }

    private double calculateSavingsRate(BigDecimal income, BigDecimal netSavings) {
        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return netSavings.divide(income, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
