package com.skoryk.projects.meetler.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
        .info(
            new Info()
                .title("Your API")
                .version("v1")
                .description("API documentation with JWT authentication"))
        .tags(
            List.of(
                new Tag().name("Authentication").description("Auth endpoints"),
                new Tag().name("User Management").description("Application user management"),
                new Tag().name("Groups").description("Group management"),
                new Tag()
                    .name("Group Administration")
                    .description("Group role and ownership management"),
                new Tag().name("Group Invites").description("Invites"),
                new Tag().name("Group Members").description("Membership management"),
                new Tag()
                    .name("Group Events")
                    .description("Group event proposals, confirmation, and event responses"),
                new Tag().name("Calendars").description("CRUD for calendars"),
                new Tag()
                    .name("External Calendar OAuth")
                    .description("OAuth connection flow for external calendar providers"),
                new Tag()
                    .name("Availability Templates")
                    .description(
                        "Availability templates, one-off blocks, recurring rules, source calendars, and resolved availability windows")))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
