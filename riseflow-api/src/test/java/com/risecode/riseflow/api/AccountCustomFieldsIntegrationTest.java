package com.risecode.riseflow.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AccountCustomFieldsIntegrationTest {

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
    void shouldPersistAndReturnCustomFieldsOnAccount() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme Corp",
                                  "industry": "Software",
                                  "email": "hello@acme.test",
                                  "phone": "123",
                                  "customFields": {"revenue": 500000, "tier": "Enterprise"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customFields", hasKey("revenue")))
                .andExpect(jsonPath("$.customFields.tier", equalTo("Enterprise")));
    }

    @Test
    void shouldRejectAccountWithMissingRequiredCustomField() throws Exception {
        UUID tenantId = UUID.randomUUID();
        createRequiredNumberField(tenantId, "revenue");

        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme Corp",
                                  "industry": "Software",
                                  "email": "hello@acme.test",
                                  "phone": "123",
                                  "customFields": {}
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectAccountWithInvalidPicklistValue() throws Exception {
        UUID tenantId = UUID.randomUUID();
        createPicklistField(tenantId, "tier", "Starter", "Growth", "Enterprise");

        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme Corp",
                                  "industry": "Software",
                                  "email": "hello@acme.test",
                                  "phone": "123",
                                  "customFields": {"tier": "Unknown"}
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldAcceptAccountWithValidPicklistValue() throws Exception {
        UUID tenantId = UUID.randomUUID();
        createPicklistField(tenantId, "tier", "Starter", "Growth", "Enterprise");

        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme Corp",
                                  "industry": "Software",
                                  "email": "hello@acme.test",
                                  "phone": "123",
                                  "customFields": {"tier": "Growth"}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customFields.tier", equalTo("Growth")));
    }

    @Test
    void shouldReturnCustomFieldsInGetById() throws Exception {
        UUID tenantId = UUID.randomUUID();

        String location = mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme",
                                  "customFields": {"score": 99}
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/v1/accounts/" + id)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_account:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customFields.score", equalTo(99)));
    }

    private void createRequiredNumberField(UUID tenantId, String fieldName) throws Exception {
        mockMvc.perform(post("/api/v1/custom-field-definitions")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityType": "ACCOUNT",
                                  "fieldName": "%s",
                                  "fieldType": "NUMBER",
                                  "required": true,
                                  "displayOrder": 1
                                }
                                """.formatted(fieldName)))
                .andExpect(status().isCreated());
    }

    private void createPicklistField(UUID tenantId, String fieldName, String... options) throws Exception {
        String optionsJson = String.join("\",\"", options);
        mockMvc.perform(post("/api/v1/custom-field-definitions")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityType": "ACCOUNT",
                                  "fieldName": "%s",
                                  "fieldType": "PICKLIST",
                                  "picklistOptions": ["%s"],
                                  "required": false,
                                  "displayOrder": 1
                                }
                                """.formatted(fieldName, optionsJson)))
                .andExpect(status().isCreated());
    }
}
