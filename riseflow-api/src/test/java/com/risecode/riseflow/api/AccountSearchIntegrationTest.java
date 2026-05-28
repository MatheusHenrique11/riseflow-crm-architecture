package com.risecode.riseflow.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.risecode.riseflow.RiseFlowApplication;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
@SpringBootTest(classes = RiseFlowApplication.class)
@DirtiesContext
class AccountSearchIntegrationTest {

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
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> "http://localhost:8081/realms/riseflow");
    }

    @Test
    void shouldSearchAccountsByCustomFieldEquals() throws Exception {
        UUID tenantId = UUID.randomUUID();
        createAccount(tenantId, "Alpha Corp", "{\"tier\": \"Enterprise\"}");
        createAccount(tenantId, "Beta Corp", "{\"tier\": \"Starter\"}");

        mockMvc.perform(post("/api/v1/accounts/search")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "criteria": [{"fieldName": "tier", "operator": "EQUALS", "value": "Enterprise"}],
                                  "page": 0,
                                  "size": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo("Alpha Corp")));
    }

    @Test
    void shouldSearchAccountsByCustomFieldContains() throws Exception {
        UUID tenantId = UUID.randomUUID();
        createAccount(tenantId, "Alpha Corp", "{\"description\": \"A leading software company\"}");
        createAccount(tenantId, "Beta Corp", "{\"description\": \"A hardware manufacturer\"}");

        mockMvc.perform(post("/api/v1/accounts/search")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "criteria": [{"fieldName": "description", "operator": "CONTAINS", "value": "software"}],
                                  "page": 0,
                                  "size": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo("Alpha Corp")));
    }

    @Test
    void shouldSearchAccountsByCustomFieldGreaterThan() throws Exception {
        UUID tenantId = UUID.randomUUID();
        createAccount(tenantId, "Big Corp", "{\"revenue\": 1000000}");
        createAccount(tenantId, "Small Corp", "{\"revenue\": 10000}");

        mockMvc.perform(post("/api/v1/accounts/search")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "criteria": [{"fieldName": "revenue", "operator": "GT", "value": "500000"}],
                                  "page": 0,
                                  "size": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo("Big Corp")));
    }

    @Test
    void shouldSearchAccountsByCustomFieldExists() throws Exception {
        UUID tenantId = UUID.randomUUID();
        createAccount(tenantId, "Has VIP", "{\"vip\": true}");
        createAccount(tenantId, "No VIP", "{}");

        mockMvc.perform(post("/api/v1/accounts/search")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "criteria": [{"fieldName": "vip", "operator": "EXISTS"}],
                                  "page": 0,
                                  "size": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo("Has VIP")));
    }

    @Test
    void shouldReturnAllAccountsForTenantWithEmptyCriteria() throws Exception {
        UUID tenantId = UUID.randomUUID();
        createAccount(tenantId, "Corp A", "{}");
        createAccount(tenantId, "Corp B", "{}");

        mockMvc.perform(post("/api/v1/accounts/search")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"criteria": [], "page": 0, "size": 20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", equalTo(2)));
    }

    @Test
    void shouldSupportPaginationInSearch() throws Exception {
        UUID tenantId = UUID.randomUUID();
        for (int i = 1; i <= 5; i++) {
            createAccount(tenantId, "Corp " + i, "{\"active\": true}");
        }

        mockMvc.perform(post("/api/v1/accounts/search")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "criteria": [{"fieldName": "active", "operator": "EXISTS"}],
                                  "page": 0,
                                  "size": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", equalTo(5)))
                .andExpect(jsonPath("$.totalPages", equalTo(3)));
    }

    @Test
    void shouldIsolateSearchResultsByTenant() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        createAccount(tenantA, "TenantA Corp", "{\"tier\": \"Enterprise\"}");
        createAccount(tenantB, "TenantB Corp", "{\"tier\": \"Enterprise\"}");

        mockMvc.perform(post("/api/v1/accounts/search")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantA.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "criteria": [{"fieldName": "tier", "operator": "EQUALS", "value": "Enterprise"}],
                                  "page": 0,
                                  "size": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", equalTo("TenantA Corp")));
    }

    private void createAccount(UUID tenantId, String name, String customFields) throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "industry": "Software",
                                  "email": "hello@example.test",
                                  "phone": "123",
                                  "customFields": %s
                                }
                                """.formatted(name, customFields)))
                .andExpect(status().isCreated());
    }
}
