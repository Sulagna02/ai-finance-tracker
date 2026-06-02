package com.financeai.repository;

import com.financeai.entity.AiInsight;
import com.financeai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiInsightRepository extends JpaRepository<AiInsight, Long> {

    List<AiInsight> findByUserOrderByGeneratedAtDesc(User user);

    Optional<AiInsight> findTopByUserOrderByGeneratedAtDesc(User user);
}
