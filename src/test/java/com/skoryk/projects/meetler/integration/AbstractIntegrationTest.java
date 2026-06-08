package com.skoryk.projects.meetler.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

@Tag("integration")
@SpringBootTest(
    properties = {
      "spring.docker.compose.enabled=false",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=true"
    })
@AutoConfigureMockMvc
abstract class AbstractIntegrationTest {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("meetler_it")
          .withUsername("meetler")
          .withPassword("meetler");

  static {
    POSTGRES.start();
  }

  @Autowired protected MockMvc mockMvc;
  protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @DynamicPropertySource
  static void configurePostgres(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  protected String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  protected JsonNode body(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  protected String bearer(String token) {
    return "Bearer " + token;
  }

  protected Map<String, Object> registerRequest(String email) {
    return Map.of("email", email, "name", "Test User", "password", "password123");
  }
}
