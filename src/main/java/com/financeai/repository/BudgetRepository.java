package com.financeai.repository;

import com.financeai.entity.Budget;
import com.financeai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUser(User user);

    List<Budget> findByUserAndMonth(User user, String month);

    Optional<Budget> findByUserAndCategoryAndMonth(User user, String category, String month);
}
