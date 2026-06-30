package com.vdt2026.omnicare.inbox.config;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vdt2026.omnicare.inbox.identity.application.AccessTokenService;
import com.vdt2026.omnicare.inbox.identity.application.AuthenticatedAgent;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentEntity;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = SecurityConfigTests.TestApplication.class)
@AutoConfigureMockMvc
class SecurityConfigTests {

    private static final UUID AGENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final AuthenticatedAgent AUTHENTICATED_AGENT = new AuthenticatedAgent(
        AGENT_ID,
        "agent@example.test",
        "Agent One"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenService accessTokenService;

    @Autowired
    private AgentRepository agentRepository;

    @BeforeEach
    void setUp() {
        reset(accessTokenService, agentRepository);
    }

    @Test
    void protectedEndpointRejectsMissingBearerTokenWithProblemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/protected-test"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Authentication required"))
            .andExpect(jsonPath("$.detail").value("A valid Bearer token is required."))
            .andExpect(jsonPath("$.instance").value("/api/v1/protected-test"));
    }

    @Test
    void protectedEndpointRejectsInvalidBearerToken() throws Exception {
        when(accessTokenService.verify("bad-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/protected-test").header("Authorization", "Bearer bad-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void protectedEndpointRejectsTokenForInactiveAgent() throws Exception {
        when(accessTokenService.verify("inactive-token")).thenReturn(Optional.of(AUTHENTICATED_AGENT));
        when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent(false)));

        mockMvc.perform(get("/api/v1/protected-test").header("Authorization", "Bearer inactive-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void protectedEndpointAllowsValidBearerToken() throws Exception {
        when(accessTokenService.verify("valid-token")).thenReturn(Optional.of(AUTHENTICATED_AGENT));
        when(agentRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent(true)));

        mockMvc.perform(get("/api/v1/protected-test").header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("agent@example.test"));
    }

    @Test
    void operationalEndpointsArePublicAndReturnCorrelationHeader() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Correlation-Id", "corr-security-test"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Correlation-Id", "corr-security-test"))
            .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk());
    }

    @Test
    void loginEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.public").value(true));
    }

    private AgentEntity agent(boolean active) {
        return new AgentEntity(AGENT_ID, "agent@example.test", "Agent One", "{noop}password", active, Instant.EPOCH);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
    })
    @Import({SecurityConfig.class, BearerTokenAuthenticationFilter.class, CorrelationIdFilter.class, TestEndpoints.class, TestBeans.class})
    static class TestApplication {
    }

    static class TestBeans {

        @Bean
        AccessTokenService accessTokenService() {
            return Mockito.mock(AccessTokenService.class);
        }

        @Bean
        AgentRepository agentRepository() {
            return Mockito.mock(AgentRepository.class);
        }
    }

    @RestController
    static class TestEndpoints {

        @GetMapping("/api/v1/protected-test")
        Map<String, String> protectedEndpoint(@AuthenticationPrincipal AuthenticatedAgent agent) {
            return Map.of("email", agent.email());
        }

        @PostMapping("/api/v1/auth/login")
        Map<String, Boolean> login() {
            return Map.of("public", true);
        }

        @GetMapping("/actuator/health")
        Map<String, String> health() {
            return Map.of("status", "UP");
        }

        @GetMapping("/actuator/metrics")
        Map<String, Boolean> metrics() {
            return Map.of("public", true);
        }

        @GetMapping("/actuator/prometheus")
        String prometheus() {
            return "";
        }
    }
}
