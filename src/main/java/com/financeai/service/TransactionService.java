package com.financeai.service;

import com.financeai.dto.TransactionRequest;
import com.financeai.dto.TransactionResponse;
import com.financeai.entity.Transaction;
import com.financeai.entity.User;
import com.financeai.exception.ResourceNotFoundException;
import com.financeai.repository.TransactionRepository;
import com.financeai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages CRUD operations for transactions scoped to the authenticated user.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new transaction for the given user.
     *
     * @param request validated transaction payload
     * @param email   email of the authenticated user (from SecurityContext)
     * @return the persisted transaction as a response DTO
     */
    @Transactional
    public TransactionResponse create(TransactionRequest request, String email) {
        User user = loadUser(email);

        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .description(request.getDescription())
                .date(request.getDate())
                .user(user)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: id={}, user={}", saved.getId(), email);
        return TransactionResponse.from(saved);
    }

    /**
     * Returns all transactions belonging to the authenticated user, newest first.
     *
     * @param email email of the authenticated user
     * @return list of transaction response DTOs
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll(String email) {
        User user = loadUser(email);
        return transactionRepository.findByUserOrderByDateDesc(user)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    /**
     * Deletes a transaction by id. Only the owning user may delete their own transactions.
     *
     * @param id    transaction id
     * @param email email of the authenticated user
     */
    @Transactional
    public void delete(Long id, String email) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        if (!transaction.getUser().getEmail().equals(email)) {
            throw new ResourceNotFoundException("Transaction", id);
        }

        transactionRepository.delete(transaction);
        log.info("Transaction deleted: id={}, user={}", id, email);
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
