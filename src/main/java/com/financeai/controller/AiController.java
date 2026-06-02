package com.financeai.controller;

import com.financeai.ai.AiService;
import com.financeai.dto.AiChatRequest;
import com.financeai.dto.AiChatResponse;
import com.financeai.dto.AiInsightResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/**
 * Exposes all AI-powered endpoints: expense categorization via chat,
 * conversational finance assistant, and monthly AI insight generation.
 *
 * <p>All routes require a valid JWT. The authenticated user's email is
 * extracted from the SecurityContext via {@code @AuthenticationPrincipal}.</p>
 */
@RestController
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Accepts a free-text message from the user and routes it to the AI
     * for contextual expense analysis or general finance Q&A.
     *
     * <p>POST /ask-ai</p>
     *
     * @param request     contains the user's message (max 2000 chars, required)
     * @param userDetails authenticated user from the JWT filter
     * @return AI reply wrapped in AiChatResponse with a server timestamp
     */
    @PostMapping("/ask-ai")
    public ResponseEntity<AiChatResponse> askAi(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /ask-ai — user={}", userDetails.getUsername());
        String reply = aiService.chatWithAi(userDetails.getUsername(), request.getMessage());
        return ResponseEntity.ok(AiChatResponse.of(reply));
    }

    /**
     * Provides a richer conversational finance chat experience. The user's
     * recent transactions are injected as context so the AI can answer
     * personalised questions ("Did I overspend this month?", "Where should I cut costs?").
     *
     * <p>POST /finance-chat</p>
     *
     * @param request     contains the user's chat message
     * @param userDetails authenticated user from the JWT filter
     * @return AI reply with a server timestamp
     */
    @PostMapping("/finance-chat")
    public ResponseEntity<AiChatResponse> financeChat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("POST /finance-chat — user={}", userDetails.getUsername());
        String reply = aiService.chatWithAi(userDetails.getUsername(), request.getMessage());
        return ResponseEntity.ok(AiChatResponse.of(reply));
    }

    /**
     * Generates (or fetches) an AI-authored monthly financial summary for the
     * authenticated user. When a {@code month} parameter is provided the AI
     * generates a fresh summary for that month and persists it. Without the
     * parameter the latest stored insight is returned.
     *
     * <p>GET /ai-summary?month=YYYY-MM</p>
     *
     * @param month       optional target month in YYYY-MM format; defaults to current month
     * @param userDetails authenticated user from the JWT filter
     * @return the generated or most-recent AiInsightResponse
     */
    @GetMapping("/ai-summary")
    public ResponseEntity<AiInsightResponse> getAiSummary(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserDetails userDetails) {
        String targetMonth = (month != null) ? month : YearMonth.now().toString();
        log.info("GET /ai-summary — user={} month={}", userDetails.getUsername(), targetMonth);
        AiInsightResponse response = aiService.generateMonthlySummary(
                userDetails.getUsername(), targetMonth);
        return ResponseEntity.ok(response);
    }
}
