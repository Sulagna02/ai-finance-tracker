package com.financeai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.ai.openai.api-key=test-key",
        "jwt.secret=test-secret-key-that-is-long-enough-for-hmac-sha256",
        "jwt.expiration.ms=86400000"
})
class AiFinanceTrackerApplicationTests {

    @Test
    void contextLoads() {
    }
}
