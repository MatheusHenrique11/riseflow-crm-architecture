package com.risecode.riseflow.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
@SpringBootTest(classes = RiseFlowApplication.class)
@DirtiesContext
class CustomFieldDefinitionIntegrationTest {

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
    void shouldCreateAndRetrieveCustomFieldDefinition() throws Exception {
        UUID tenantId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(post("/api/v1/custom-field-definitions")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityType": "ACCOUNT",
                                  "fieldName": "revenue",
                                  "fieldType": "NUMBER",
                                  "required": true,
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fieldName", equalTo("revenue")))
                .andExpect(jsonPath("$.fieldType", equalTo("NUMBER")))
                .andExpect(jsonPath("$.required", equalTo(true)))
                .andReturn();

        mockMvc.perform(get("/api/v1/custom-field-definitions?entityType=ACCOUNT")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fieldName", equalTo("revenue")));
    }

    @Test
    void shouldRejectDuplicateFieldNameForSameTenantAndEntityType() throws Exception {
        UUID tenantId = UUID.randomUUID();
        createNumberField(tenantId, "score");

        mockMvc.perform(post("/api/v1/custom-field-definitions")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityType": "ACCOUNT",
                                  "fieldName": "score",
                                  "fieldType": "TEXT",
                                  "required": false,
                                  "displayOrder": 2
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldCreatePicklistDefinitionWithOptions() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/custom-field-definitions")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityType": "ACCOUNT",
                                  "fieldName": "tier",
                                  "fieldType": "PICKLIST",
                                  "picklistOptions": ["Starter", "Growth", "Enterprise"],
                                  "required": false,
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.picklistOptions", hasSize(3)));
    }

    @Test
    void shouldRejectPicklistWithoutOptions() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/custom-field-definitions")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityType": "ACCOUNT",
                                  "fieldName": "tier",
                                  "fieldType": "PICKLIST",
                                  "required": false,
                                  "displayOrder": 1
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldIsolateDefinitionsBetweenTenants() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        createNumberField(tenantA, "revenueA");
        createNumberField(tenantB, "revenueB");

        mockMvc.perform(get("/api/v1/custom-field-definitions?entityType=ACCOUNT")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantA.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fieldName", equalTo("revenueA")));
    }

    @Test
    void shouldUpdateDefinition() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String id = createNumberField(tenantId, "myField");

        mockMvc.perform(put("/api/v1/custom-field-definitions/" + id)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityType": "ACCOUNT",
                                  "fieldName": "myField",
                                  "fieldType": "NUMBER",
                                  "required": true,
                                  "displayOrder": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.required", equalTo(true)))
                .andExpect(jsonPath("$.displayOrder", equalTo(5)));
    }

    @Test
    void shouldDeleteDefinition() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String id = createNumberField(tenantId, "toDelete");

        mockMvc.perform(delete("/api/v1/custom-field-definitions/" + id)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:write"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/custom-field-definitions?entityType=ACCOUNT")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private String createNumberField(UUID tenantId, String fieldName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/custom-field-definitions")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_custom-field:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "entityType": "ACCOUNT",
                                  "fieldName": "%s",
                                  "fieldType": "NUMBER",
                                  "required": false,
                                  "displayOrder": 1
                                }
                                """.formatted(fieldName)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        int start = body.indexOf("\"id\":\"") + 6;
        int end = body.indexOf("\"", start);
        return body.substring(start, end);
    }
}
