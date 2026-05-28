package com.risecode.riseflow.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.risecode.riseflow.RiseFlowApplication;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
@SpringBootTest(classes = RiseFlowApplication.class)
class TenantAccountApiIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("riseflow")
            .withUsername("riseflow")
            .withPassword("riseflow");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:8081/realms/riseflow");
    }

    @Test
    void shouldCreateTenantAndAccountWithAuthenticatedRequests() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/tenants")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_tenant:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme",
                                  "domain": "acme",
                                  "ownerEmail": "owner@acme.test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.domain", equalTo("acme")));

        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt()
                                .jwt(token -> token.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme Corp",
                                  "industry": "Software",
                                  "email": "hello@acme.test",
                                  "phone": "+15551234567"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId", equalTo(tenantId.toString())))
                .andExpect(jsonPath("$.name", equalTo("Acme Corp")));
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenScopeIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_account:write"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldIsolateAccountsByTenantContext() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        createAccount(tenantA, "Tenant A Account");
        createAccount(tenantB, "Tenant B Account");

        mockMvc.perform(get("/api/v1/accounts")
                        .with(jwt()
                                .jwt(token -> token.claim("tenant_id", tenantA.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", equalTo("Tenant A Account")));
    }

    private void createAccount(UUID tenantId, String name) throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt()
                                .jwt(token -> token.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "industry": "Software",
                                  "email": "hello@example.test",
                                  "phone": "123"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated());
    }
}
