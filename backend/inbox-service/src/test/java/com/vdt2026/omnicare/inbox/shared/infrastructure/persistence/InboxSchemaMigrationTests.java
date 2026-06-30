package com.vdt2026.omnicare.inbox.shared.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class InboxSchemaMigrationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void migrateSchema() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate();

        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void appliesAllInboxMigrationsAndSeedData() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE",
            Integer.class
        );
        Integer agentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agents", Integer.class);
        Integer conversationCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM conversations", Integer.class);
        String messagesExternalIndex = jdbcTemplate.queryForObject(
            "SELECT to_regclass('public.ux_messages_external_message')::text",
            String.class
        );

        assertThat(appliedMigrations).isEqualTo(2);
        assertThat(agentCount).isEqualTo(3);
        assertThat(conversationCount).isEqualTo(2);
        assertThat(messagesExternalIndex).isEqualTo("ux_messages_external_message");
    }

    @Test
    void rejectsDuplicateProcessedEventIds() {
        jdbcTemplate.update(
            """
            INSERT INTO processed_events (id, event_id, event_type, source, correlation_id, processed_at)
            VALUES (?, ?, 'message-received', 'schema-test', 'corr-schema-test', now())
            """,
            uuid("70000000-0000-0000-0000-000000000001"),
            "event-schema-duplicate"
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
            """
            INSERT INTO processed_events (id, event_id, event_type, source, correlation_id, processed_at)
            VALUES (?, ?, 'message-received', 'schema-test', 'corr-schema-test', now())
            """,
            uuid("70000000-0000-0000-0000-000000000002"),
            "event-schema-duplicate"
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsDuplicateInboundProviderMessageIdsButAllowsNullOutboundProviderIds() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            """
            INSERT INTO messages (
                id, conversation_id, channel, direction, delivery_status, provider_account_id,
                external_message_id, content, occurred_at, created_at
            )
            VALUES (
                ?, '40000000-0000-0000-0000-000000000001', 'FACEBOOK', 'INBOUND', 'RECEIVED',
                'local-page-id', 'mid.local.facebook.1', 'Duplicate inbound', now(), now()
            )
            """,
            uuid("71000000-0000-0000-0000-000000000001")
        )).isInstanceOf(DataAccessException.class);

        assertThatCode(() -> {
            insertOutboundMessageWithNullExternalId("71000000-0000-0000-0000-000000000002");
            insertOutboundMessageWithNullExternalId("71000000-0000-0000-0000-000000000003");
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicateConversationAndChannelIdentityExternalKeys() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            """
            INSERT INTO channel_identities (
                id, customer_id, channel, provider_account_id, external_identity_id, display_name, created_at
            )
            VALUES (
                ?, '20000000-0000-0000-0000-000000000001', 'FACEBOOK', 'local-page-id',
                'fb-user-a', 'Duplicate Identity', now()
            )
            """,
            uuid("72000000-0000-0000-0000-000000000001")
        )).isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
            """
            INSERT INTO conversations (
                id, customer_id, channel_identity_id, channel, source_type, status, assigned_agent_id,
                provider_account_id, external_conversation_id, subject, last_message_preview,
                last_activity_at, created_at, updated_at
            )
            VALUES (
                ?, '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001',
                'FACEBOOK', 'MESSAGE', 'OPEN', NULL, 'local-page-id', 'messenger:fb-user-a',
                NULL, 'Duplicate conversation', now(), now(), now()
            )
            """,
            uuid("73000000-0000-0000-0000-000000000001")
        )).isInstanceOf(DataAccessException.class);
    }

    @Test
    void rejectsCaseInsensitiveDuplicateAgentEmails() {
        assertThatThrownBy(() -> jdbcTemplate.update(
            """
            INSERT INTO agents (id, email, display_name, password_hash, active, created_at)
            VALUES (?, 'AGENT@example.test', 'Duplicate Agent', '{noop}password', TRUE, now())
            """,
            uuid("74000000-0000-0000-0000-000000000001")
        )).isInstanceOf(DataAccessException.class);
    }

    private void insertOutboundMessageWithNullExternalId(String id) {
        jdbcTemplate.update(
            """
            INSERT INTO messages (
                id, conversation_id, channel, direction, delivery_status, provider_account_id,
                external_message_id, content, occurred_at, created_at
            )
            VALUES (
                ?, '40000000-0000-0000-0000-000000000001', 'FACEBOOK', 'OUTBOUND', 'QUEUED',
                'local-page-id', NULL, 'Queued reply', now(), now()
            )
            """,
            uuid(id)
        );
    }

    private static UUID uuid(String value) {
        return UUID.fromString(value);
    }
}
