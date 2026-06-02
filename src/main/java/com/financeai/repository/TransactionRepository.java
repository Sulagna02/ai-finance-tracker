package com.financeai.repository;

import com.financeai.entity.Transaction;
import com.financeai.entity.TransactionType;
import com.financeai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserOrderByDateDesc(User user);

    List<Transaction> findByUserAndDateBetweenOrderByDateDesc(User user, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserAndTypeOrderByDateDesc(User user, TransactionType type);

    /**
     * Returns the total amount for a given user, transaction type, and date range.
     * Returns 0 (via COALESCE) when no matching rows exist so callers never receive null.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = :type
              AND t.date BETWEEN :startDate AND :endDate
            """)
    BigDecimal sumAmountByUserAndTypeAndDateBetween(
            @Param("user") User user,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Aggregates expense amounts by category for a user within a date range.
     * Returns Object[] rows of [category (String), total (BigDecimal)].
     */
    @Query("""
            SELECT t.category, COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.user = :user
              AND t.type = 'EXPENSE'
              AND t.date BETWEEN :startDate AND :endDate
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> sumExpensesByCategoryAndDateBetween(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
