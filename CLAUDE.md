# CLAUDE.md — AI Finance Tracker

## Project Overview

Production-grade AI-powered Finance Tracker built for portfolio, FAANG-level backend prep, and AI engineering practice.

**Repo:** `ai-finance-tracker-spring-ai`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot |
| AI | Spring AI (ChatClient) |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| Security | Spring Security + JWT |
| Frontend | React / Next.js |
| UI | Tailwind CSS |
| Charts | Recharts |
| AI Models | OpenAI (primary), Ollama / Gemini (optional) |
| Deployment | Docker + AWS (EC2, RDS, S3) |

---

## Package Structure

```
src/main/java/com/financeai/
├── config/         # Spring beans, security config, AI config
├── controller/     # REST controllers only — no business logic here
├── service/        # All business logic lives here
├── repository/     # Spring Data JPA interfaces
├── entity/         # JPA entities — never exposed in API responses
├── dto/            # Request/response objects for all APIs
├── security/       # JWT filter, UserDetailsService, token util
├── ai/             # Spring AI services (categorization, chat, summary)
├── analytics/      # Analytics queries and aggregation logic
├── exception/      # GlobalExceptionHandler + custom exceptions
└── util/           # Shared helpers
```

---

## Core Entities

```
User          — id, name, email, password (hashed)
Transaction   — id, amount, type (INCOME/EXPENSE), category, description, date, user
Budget        — id, category, limitAmount, month, user
AiInsight     — id, summary, generatedAt, user
```

---

## API Surface

### Auth
| Method | Endpoint |
|---|---|
| POST | /auth/register |
| POST | /auth/login |

### Transactions
| Method | Endpoint |
|---|---|
| POST | /transactions |
| GET | /transactions |
| DELETE | /transactions/{id} |

### Analytics
| Method | Endpoint |
|---|---|
| GET | /analytics/monthly |
| GET | /analytics/categories |
| GET | /analytics/savings |

### AI
| Method | Endpoint |
|---|---|
| POST | /ask-ai |
| POST | /finance-chat |
| GET | /ai-summary |

---

## Coding Rules

These are strict — follow them in every file generated or modified.

- **DTOs always** — never return JPA entities from controllers
- **@Valid on all request bodies** — validate at the controller boundary
- **Service layer owns logic** — controllers only delegate, no if/else business logic in controllers
- **GlobalExceptionHandler** — all errors flow through `@ControllerAdvice`, never raw exceptions to the client
- **SOLID principles** — single responsibility per class, depend on interfaces not implementations
- **No overengineering** — three similar lines beat a premature abstraction
- **No unnecessary comments** — name things well instead; only comment non-obvious WHY, never WHAT
- **Preserve existing methods** when refactoring — change incrementally, don't rewrite working code
- **Backward-compatible APIs** — do not break existing contracts without explicit instruction

---

## AI Integration Pattern

All AI features use Spring AI's `ChatClient`. Follow this pattern:

```java
@Service
public class AiService {

    private final ChatClient chatClient;

    public AiService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String categorizeExpense(String description) {
        String prompt = """
            Categorize this expense into exactly one word:
            Food, Travel, Shopping, Bills, Health, Entertainment, Other

            Expense: %s
            """.formatted(description);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
```

---

## Database Config

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/finance_ai
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

---

## Security Pattern

- JWT-based stateless authentication
- `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter`
- All `/auth/**` endpoints are public
- All other endpoints require a valid JWT
- Passwords hashed with BCrypt

---

## Development Roadmap

| Week | Focus |
|---|---|
| 1 | Spring Boot setup, PostgreSQL, CRUD APIs, Analytics, JWT Auth |
| 2 | Spring AI — categorization, summaries, finance chat |
| 3 | React frontend — dashboard, transaction table, charts, AI chat UI |
| 4 | Docker, cloud deployment, security hardening, cleanup |

**Current phase: Week 1 — Backend Foundations**

---

## Maven Dependencies (Key)

```xml
<!-- Spring Boot starters -->
<dependency>spring-boot-starter-web</dependency>
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-validation</dependency>

<!-- Database -->
<dependency>postgresql</dependency>

<!-- JWT -->
<dependency>jjwt-api</dependency>

<!-- Spring AI -->
<dependency>spring-ai-openai-spring-boot-starter</dependency>

<!-- Lombok -->
<dependency>lombok</dependency>
```

---

## What NOT to Do

- Do not expose entity classes in API responses
- Do not put business logic in controllers
- Do not catch and swallow exceptions silently
- Do not commit secrets or API keys — use environment variables
- Do not add features beyond the current week's scope without instruction
- Do not use field injection (`@Autowired` on fields) — use constructor injection
