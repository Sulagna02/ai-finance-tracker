# SpendSense — AI Personal Finance Tracker
*Track smarter. Spend wiser.*

A personal finance tracker with AI built in: log income and expenses, get them categorized automatically, see monthly analytics, and ask questions about your own spending in plain English.

**Backend:** Jaiwant Singh Raghav · **Frontend (React, in progress):** Sulagna Dutta Choudhuri

---

## What's built (backend)

- **Auth:** register and log in with JWT (Spring Security, stateless); passwords hashed with BCrypt
- **Transactions:** create, list and delete income and expense records per user
- **Budgets:** set a spending limit per category per month
- **Analytics:** monthly income, expenses, net savings and savings rate; breakdown by category
- **AI (Spring AI + OpenAI `gpt-4o-mini`):**
  - Automatic expense categorization. The model's answer is checked against a fixed category list, and anything unexpected falls back to `Other`.
  - AI-written monthly spending summary, saved to the database
  - Finance chat assistant that answers using the user's own recent transactions as context
- **Engineering:** DTOs at the API boundary (entities never exposed), `@Valid` request validation, central `@ControllerAdvice` error handling, Flyway database migrations, constructor injection, multi-stage Docker build that runs as a non-root user

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3, Spring Security, Spring Data JPA |
| AI | Spring AI (OpenAI) |
| Database | PostgreSQL + Flyway |
| Auth | JWT (jjwt) |
| Build / run | Maven, Docker |

## Architecture

```
Client (React, in progress)
        │  REST + JWT
        ▼
Spring Boot API ── controllers → services → repositories
        │                   │
        │                   └── Spring AI ChatClient → OpenAI
        ▼
   PostgreSQL (schema managed by Flyway)
```

## API

| Method | Endpoint | Description |
|---|---|---|
| POST | `/auth/register` | Create an account |
| POST | `/auth/login` | Log in, returns a JWT |
| POST | `/transactions` | Add an income or expense |
| GET | `/transactions` | List your transactions |
| DELETE | `/transactions/{id}` | Delete a transaction |
| POST | `/budgets` | Set a monthly category budget |
| GET | `/budgets` | List your budgets |
| GET | `/analytics/monthly` | Monthly totals and savings rate |
| GET | `/analytics/categories` | Spending by category (current month) |
| GET | `/analytics/savings` | Savings overview |
| POST | `/ask-ai` | Ask a question about your finances |
| POST | `/finance-chat` | Chat with the finance assistant |
| GET | `/ai-summary?month=YYYY-MM` | AI-written monthly summary |

All endpoints except `/auth/**` need an `Authorization: Bearer <token>` header.

## Run locally

**Prerequisites:** Java 21, Maven, PostgreSQL (or Docker), an OpenAI API key.

1. Create a database named `finance_ai`.
2. Copy `.env.example` to `.env` and fill in your values. Secrets are read only from environment variables and are never committed.
3. Run:
   ```bash
   export $(grep -v '^#' .env | xargs)
   mvn spring-boot:run
   ```
   Flyway creates the tables on startup.

**With Docker:**
```bash
docker build -t spendsense .
docker run --env-file .env -p 8080:8080 spendsense
```

## Roadmap

- [ ] React frontend: dashboard, transaction table, charts, AI chat (in progress)
- [ ] Unit and integration tests (JUnit 5, Mockito, Testcontainers)
- [ ] Cloud deployment (AWS EC2 + RDS)
- [ ] Spending prediction and unusual-expense alerts
- [ ] Receipt upload (S3)
- [ ] Caching (Redis) and event streaming (Kafka)
