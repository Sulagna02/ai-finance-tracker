# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# Copy POM first — Maven will cache downloaded dependencies in a separate layer.
# This layer is only invalidated when pom.xml changes, not on source changes.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR, skipping tests
# (tests should run in CI against a real DB, not inside the Docker image build)
COPY src src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create a non-root user for security — never run application processes as root
RUN addgroup -S financeai && adduser -S financeai -G financeai
USER financeai

WORKDIR /app

# Copy only the compiled fat JAR from the build stage
COPY --from=build /workspace/target/*.jar app.jar

# Expose the Spring Boot default port
EXPOSE 8080

# Exec form ensures the JVM is PID 1 and receives OS signals for graceful shutdown
ENTRYPOINT ["java", "-jar", "app.jar"]
