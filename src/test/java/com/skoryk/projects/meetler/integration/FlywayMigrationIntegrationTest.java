package com.skoryk.projects.meetler.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayMigrationIntegrationTest extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void migrationsCreateExpectedCoreTables() {
    Integer successfulMigrations =
        jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success = true", Integer.class);

    assertThat(successfulMigrations).isGreaterThanOrEqualTo(18);
    assertThat(tableExists("app_user")).isTrue();
    assertThat(tableExists("calendar")).isTrue();
    assertThat(tableExists("availability_template")).isTrue();
    assertThat(tableExists("calendar_event")).isTrue();
    assertThat(tableExists("group_member")).isTrue();
    assertThat(tableExists("guest_login_token")).isTrue();
    assertThat(tableExists("password_reset_token")).isTrue();
    assertThat(tableExists("group_event")).isTrue();
    assertThat(tableExists("group_event_participant")).isTrue();
    assertThat(tableExists("subscription_plan")).isTrue();
    assertThat(tableExists("user_subscription")).isTrue();
  }

  private boolean tableExists(String tableName) {
    Boolean exists =
        jdbcTemplate.queryForObject(
            """
            select exists (
              select 1
              from information_schema.tables
              where table_schema = 'public'
                and table_name = ?
            )
            """,
            Boolean.class,
            tableName);
    return Boolean.TRUE.equals(exists);
  }
}
