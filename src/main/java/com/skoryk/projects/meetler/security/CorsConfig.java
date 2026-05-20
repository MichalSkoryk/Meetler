package com.skoryk.projects.meetler.security;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@ConfigurationProperties(prefix = "web.cors")
@Getter
@Setter
public class CorsConfig {

  private List<CorsMapping> mappings;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    if (mappings != null) {
      for (CorsMapping mapping : mappings) {
        CorsConfiguration config = new CorsConfiguration();

        if (mapping.getAllowedOrigins() != null && !mapping.getAllowedOrigins().isEmpty()) {
          config.setAllowedOrigins(Arrays.asList(mapping.getAllowedOrigins().split(",")));
        }

        if (mapping.getAllowedMethods() != null && !mapping.getAllowedMethods().isEmpty()) {
          config.setAllowedMethods(Arrays.asList(mapping.getAllowedMethods().split(",")));
        } else {
          config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        }

        if (mapping.getAllowedHeaders() != null && !mapping.getAllowedHeaders().isEmpty()) {
          config.setAllowedHeaders(Arrays.asList(mapping.getAllowedHeaders().split(",")));
        } else {
          config.setAllowedHeaders(List.of("*"));
        }

        config.setAllowCredentials(mapping.isAllowCredentials());

        source.registerCorsConfiguration(mapping.getPath(), config);
      }
    }

    return source;
  }

  @Getter
  @Setter
  public static class CorsMapping {
    private String path;
    private String allowedOrigins;
    private String allowedMethods;
    private String allowedHeaders;
    private boolean allowCredentials;
  }
}
