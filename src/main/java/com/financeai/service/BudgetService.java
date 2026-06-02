package com.financeai.service;

import com.financeai.dto.BudgetRequest;
import com.financeai.dto.BudgetResponse;
import com.financeai.entity.Budget;
import com.financeai.entity.User;
import com.financeai.exception.ResourceNotFoundException;
import com.financeai.repository.BudgetRepository;
import com.financeai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages budget records scoped to the authenticated user.
 */
@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository, UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a budget entry for a category and month for the authenticated user.
     *
     * @param request validated budget payload
     * @param email   email of the authenticated user
     * @return the persisted budget as a response DTO
     */
    @Transactional
    public BudgetResponse create(BudgetRequest request, String email) {
        User user = loadUser(email);

        Budget budget = Budget.builder()
                .category(request.getCategory())
                .limitAmount(request.getLimitAmount())
                .month(request.getMonth())
                .user(user)
                .build();

        Budget saved = budgetRepository.save(budget);
        log.info("Budget created: id={}, user={}, month={}", saved.getId(), email, saved.getMonth());
        return BudgetResponse.from(saved);
    }

    /**
     * Returns all budgets for the authenticated user.
     *
     * @param email email of the authenticated user
     * @return list of budget response DTOs
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getAll(String email) {
        User user = loadUser(email);
        return budgetRepository.findByUser(user)
                .stream()
                .map(BudgetResponse::from)
                .toList();
    }

    /**
     * Returns budgets for the authenticated user filtered by month.
     *
     * @param email email of the authenticated user
     * @param month month string in YYYY-MM format
     * @return filtered list of budget response DTOs
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getByMonth(String email, String month) {
        User user = loadUser(email);
        return budgetRepository.findByUserAndMonth(user, month)
                .stream()
                .map(BudgetResponse::from)
                .toList();
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
