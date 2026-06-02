package com.financeai.controller;

import com.financeai.dto.TransactionRequest;
import com.financeai.dto.TransactionResponse;
import com.financeai.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for transactions. All routes require a valid JWT.
 * The authenticated user is resolved from the SecurityContext via @AuthenticationPrincipal.
 */
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Creates a new transaction for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TransactionResponse response = transactionService.create(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all transactions for the authenticated user, newest first.
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<TransactionResponse> transactions = transactionService.getAll(userDetails.getUsername());
        return ResponseEntity.ok(transactions);
    }

    /**
     * Deletes a transaction by id. The transaction must belong to the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        transactionService.delete(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
