package com.financeai.ai;

import com.financeai.dto.AiInsightResponse;
import com.financeai.entity.AiInsight;
import com.financeai.entity.Transaction;
import com.financeai.entity.TransactionType;
import com.financeai.entity.User;
import com.financeai.exception.ResourceNotFoundException;
import com.financeai.repository.AiInsightRepository;
import com.financeai.repository.TransactionRepository;
import com.financeai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core AI service providing expense categorization, monthly financial summaries,
 * and conversational finance assistance powered by Spring AI and OpenAI.
 *
 * <p>All prompt strings use structured system/user separation to prevent
 * prompt-injection attacks: user-supplied data is always interpolated into the
 * user turn, never the system turn.</p>
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private static final String VALID_CATEGORIES =
            "Food, Travel, Shopping, Bills, Health, Entertainment, Education, Other";

    private final ChatClient chatClient;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AiInsightRepository aiInsightRepository;

    public AiService(
            ChatClient.Builder chatClientBuilder,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            AiInsightRepository aiInsightRepository) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.aiInsightRepository = aiInsightRepository;
    }

    /**
     * Categorizes a transaction based on its description and amount using an LLM.
     * Returns one of the canonical categories or "Other" if no match is found.
     *
     * @param description free-text description of the expense
     * @param amount      transaction amount (used to add context to ambiguous descriptions)
     * @return one of: Food, Travel, Shopping, Bills, Health, Entertainment, Education, Other
     */
    public String categorizeExpense(String description, BigDecimal amount) {
        String systemPrompt = """
                You are a financial transaction categorizer.
                Your task is to classify a transaction into exactly one of the following categories:
                Food, Travel, Shopping, Bills, Health, Entertainment, Education, Other

                Rules:
                - Reply with only the single category word — no explanation, punctuation, or extra text.
                - If you are uncertain, reply with: Other
                - Examples:
                  "Dinner at Chipotle" → Food
                  "Uber to airport" → Travel
                  "Netflix subscription" → Entertainment
                  "Doctor visit co-pay" → Health
                  "Amazon order" → Shopping
                  "Electricity bill" → Bills
                  "Coursera course" → Education
                """;

        String userMessage = "Transaction: \"%s\" | Amount: %s".formatted(
                description, amount != null ? amount.toPlainString() : "unknown");

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

        String category = result != null ? result.strip() : "Other";
        log.info("Categorized expense '{}' as '{}'", description, category);

        // Validate the returned category is one of the known values; fall back to Other
        if (!VALID_CATEGORIES.contains(category)) {
            log.warn("LLM returned unexpected category '{}', defaulting to Other", category);
            return "Other";
        }
        return category;
    }

    /**
     * Generates and persists an AI-authored narrative summary of the user's finances
     * for the specified month.
     *
     * <p>The user's full transaction list for that month is injected as context into the
     * prompt so the model can reference actual spending data.</p>
     *
     * @param email email of the authenticated user
     * @param month target month in "YYYY-MM" format
     * @return the saved AiInsight as a response DTO
     */
    @Transactional
    public AiInsightResponse generateMonthlySummary(String email, String month) {
        User user = loadUser(email);

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid month format. Use YYYY-MM.");
        }

        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository
                .findByUserAndDateBetweenOrderByDateDesc(user, start, end);

        BigDecimal totalIncome = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpenses = sumByType(transactions, TransactionType.EXPENSE);
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        String transactionContext = buildTransactionContext(transactions);

        String systemPrompt = """
                You are a personal finance advisor writing a concise monthly financial summary.
                Be encouraging but honest. Highlight notable spending patterns.
                Write in second person ("You spent..."). Keep it under 200 words.
                Do not invent numbers — use only the data provided.
                """;

        String userMessage = """
                Generate a financial summary for %s.

                Financial Overview:
                - Total Income: $%s
                - Total Expenses: $%s
                - Net Savings: $%s

                Transactions this month:
                %s
                """.formatted(month, totalIncome.toPlainString(),
                totalExpenses.toPlainString(), netSavings.toPlainString(),
                transactionContext);

        String summary = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

        AiInsight insight = AiInsight.builder()
                .summary(summary)
                .user(user)
                .build();

        AiInsight saved = aiInsightRepository.save(insight);
        log.info("Generated monthly summary for user={} month={}", email, month);
        return AiInsightResponse.from(saved);
    }

    /**
     * Handles a conversational finance question from the user.
     * The user's recent transactions are injected into the system prompt so the
     * assistant can answer questions like "How much did I spend on food?" accurately.
     *
     * @param email       email of the authenticated user
     * @param userMessage the user's question or chat message
     * @return the AI assistant's reply
     */
    @Transactional(readOnly = true)
    public String chatWithAi(String email, String userMessage) {
        User user = loadUser(email);

        // Fetch up to 50 most recent transactions for context
        List<Transaction> recentTransactions = transactionRepository
                .findByUserOrderByDateDesc(user)
                .stream()
                .limit(50)
                .collect(Collectors.toList());

        BigDecimal totalIncome = sumByType(recentTransactions, TransactionType.INCOME);
        BigDecimal totalExpenses = sumByType(recentTransactions, TransactionType.EXPENSE);
        BigDecimal netSavings = totalIncome.subtract(totalExpenses);

        String transactionContext = buildTransactionContext(recentTransactions);

        String systemPrompt = """
                You are a knowledgeable and friendly personal finance assistant.
                Answer questions about the user's finances based solely on the data below.
                Be concise, specific, and actionable. Do not invent data not present below.
                If you cannot answer from the data provided, say so clearly.

                User Financial Data (most recent 50 transactions):
                - Total Income on record: $%s
                - Total Expenses on record: $%s
                - Net Savings: $%s

                Transactions:
                %s
                """.formatted(
                totalIncome.toPlainString(),
                totalExpenses.toPlainString(),
                netSavings.toPlainString(),
                transactionContext);

        String reply = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

        log.info("AI chat response generated for user={}", email);
        return reply;
    }

    /**
     * Retrieves the most recently generated AI insight for the user without
     * generating a new one. Returns null if no insights exist.
     *
     * @param email email of the authenticated user
     * @return the latest AiInsightResponse, or null if none exists
     */
    @Transactional(readOnly = true)
    public AiInsightResponse getLatestInsight(String email) {
        User user = loadUser(email);
        return aiInsightRepository.findTopByUserOrderByGeneratedAtDesc(user)
                .map(AiInsightResponse::from)
                .orElse(null);
    }

    // --------------- private helpers ---------------

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private BigDecimal sumByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Formats a list of transactions as a compact, numbered text block suitable
     * for inclusion in an LLM prompt.
     */
    private String buildTransactionContext(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return "No transactions found.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            sb.append(i + 1).append(". ")
                    .append(t.getDate()).append(" | ")
                    .append(t.getType()).append(" | ")
                    .append(t.getCategory()).append(" | $")
                    .append(t.getAmount().toPlainString());
            if (t.getDescription() != null && !t.getDescription().isBlank()) {
                sb.append(" | ").append(t.getDescription());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
