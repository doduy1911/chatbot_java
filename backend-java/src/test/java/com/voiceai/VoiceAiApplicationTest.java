package com.voiceai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "app.jwt.secret=testsecret12345678901234567890123456",
    "app.jwt.expiration-days=30"
})
class VoiceAiApplicationTest {

    @Test
    void contextLoads() {
    }
}
