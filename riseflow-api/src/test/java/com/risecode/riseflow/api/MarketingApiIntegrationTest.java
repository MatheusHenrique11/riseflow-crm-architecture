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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class MarketingApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("riseflow")
            .withUsername("riseflow")
            .withPassword("riseflow");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    // --- Email template CRUD ---

    @Test
    void shouldCreateAndListEmailTemplates() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/email-templates")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Welcome", "subject": "Welcome to RiseFlow", "body": "Hello!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("Welcome")))
                .andExpect(jsonPath("$.subject", equalTo("Welcome to RiseFlow")));

        mockMvc.perform(get("/api/v1/email-templates")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldUpdateEmailTemplate() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String templateBody = createTemplate(tenantId, "Old Name", "Old Subject", "Old body");
        String templateId = extractId(templateBody);

        mockMvc.perform(put("/api/v1/email-templates/" + templateId)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "New Name", "subject": "New Subject", "body": "New body"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("New Name")))
                .andExpect(jsonPath("$.subject", equalTo("New Subject")));
    }

    @Test
    void shouldDeleteEmailTemplate() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String templateBody = createTemplate(tenantId, "To Delete", "Subject", "Body");
        String templateId = extractId(templateBody);

        mockMvc.perform(delete("/api/v1/email-templates/" + templateId)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write"))))
                .andExpect(status().isNoContent());
    }

    // --- Campaign CRUD ---

    @Test
    void shouldCreateCampaign() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/campaigns")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Q4 Promo", "description": "Quarterly campaign"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("Q4 Promo")))
                .andExpect(jsonPath("$.status", equalTo("DRAFT")));
    }

    @Test
    void shouldCreateCampaignWithSequenceAndSteps() throws Exception {
        UUID tenantId = UUID.randomUUID();

        String campaignBody = createCampaign(tenantId, "Onboarding");
        String campaignId = extractId(campaignBody);

        String seqBody = mockMvc.perform(post("/api/v1/campaigns/" + campaignId + "/sequences")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Onboarding Sequence"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("Onboarding Sequence")))
                .andReturn().getResponse().getContentAsString();

        String seqId = extractId(seqBody);

        mockMvc.perform(post("/api/v1/sequences/" + seqId + "/steps")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepOrder": 1, "delayMinutes": 0, "actionType": "EMAIL", "messageBody": "Welcome!"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stepOrder", equalTo(1)))
                .andExpect(jsonPath("$.actionType", equalTo("EMAIL")));

        mockMvc.perform(post("/api/v1/sequences/" + seqId + "/steps")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepOrder": 2, "delayMinutes": 1440, "actionType": "WAIT"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/campaigns/" + campaignId)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequences", hasSize(1)))
                .andExpect(jsonPath("$.sequences[0].steps", hasSize(2)));
    }

    @Test
    void shouldActivateCampaignWithSequenceAndSteps() throws Exception {
        UUID tenantId = UUID.randomUUID();

        String campaignId = extractId(createCampaign(tenantId, "Launch Campaign"));
        String seqId = extractId(addSequence(tenantId, campaignId, "Email Seq"));
        addEmailStep(tenantId, seqId, 1, 0, "Hello world");

        mockMvc.perform(put("/api/v1/campaigns/" + campaignId + "/status")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("ACTIVE")));
    }

    @Test
    void shouldRejectActivationWithoutSteps() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String campaignId = extractId(createCampaign(tenantId, "Empty Campaign"));
        addSequence(tenantId, campaignId, "Empty Seq");

        mockMvc.perform(put("/api/v1/campaigns/" + campaignId + "/status")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectDuplicateStepOrder() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String campaignId = extractId(createCampaign(tenantId, "Campaign"));
        String seqId = extractId(addSequence(tenantId, campaignId, "Seq"));
        addEmailStep(tenantId, seqId, 1, 0, "First");

        mockMvc.perform(post("/api/v1/sequences/" + seqId + "/steps")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepOrder": 1, "delayMinutes": 0, "actionType": "WAIT"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldDeleteSequenceStep() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String campaignId = extractId(createCampaign(tenantId, "Campaign"));
        String seqId = extractId(addSequence(tenantId, campaignId, "Seq"));
        String stepBody = addEmailStep(tenantId, seqId, 1, 0, "Hello");
        String stepId = extractId(stepBody);

        mockMvc.perform(delete("/api/v1/steps/" + stepId)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldIsolateCampaignsBetweenTenants() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        createCampaign(tenantA, "Tenant A Campaign");

        mockMvc.perform(get("/api/v1/campaigns")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantB.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- Automation rule CRUD ---

    @Test
    void shouldCreateAndListAutomationRules() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String campaignId = extractId(createCampaign(tenantId, "Campaign"));
        String seqId = extractId(addSequence(tenantId, campaignId, "Seq"));

        mockMvc.perform(post("/api/v1/automation-rules")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New account rule",
                                  "triggerType": "ACCOUNT_CREATED",
                                  "sequenceId": "%s",
                                  "active": true
                                }
                                """.formatted(seqId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("New account rule")))
                .andExpect(jsonPath("$.triggerType", equalTo("ACCOUNT_CREATED")))
                .andExpect(jsonPath("$.active", equalTo(true)));

        mockMvc.perform(get("/api/v1/automation-rules")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void shouldUpdateAndDeleteAutomationRule() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String campaignId = extractId(createCampaign(tenantId, "Campaign"));
        String seqId = extractId(addSequence(tenantId, campaignId, "Seq"));

        String ruleBody = mockMvc.perform(post("/api/v1/automation-rules")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Rule", "triggerType": "ACCOUNT_CREATED", "sequenceId": "%s", "active": true}
                                """.formatted(seqId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String ruleId = extractId(ruleBody);

        mockMvc.perform(put("/api/v1/automation-rules/" + ruleId)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Updated Rule", "triggerType": "DEAL_STAGE_CHANGED", "sequenceId": "%s", "active": false}
                                """.formatted(seqId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", equalTo("Updated Rule")))
                .andExpect(jsonPath("$.active", equalTo(false)));

        mockMvc.perform(delete("/api/v1/automation-rules/" + ruleId)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write"))))
                .andExpect(status().isNoContent());
    }

    // --- Enrollment ---

    @Test
    void shouldListEmptyEnrollments() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/enrollments")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- Private helpers ---

    private String createTemplate(UUID tenantId, String name, String subject, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/email-templates")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "subject": "%s", "body": "%s"}
                                """.formatted(name, subject, body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String createCampaign(UUID tenantId, String name) throws Exception {
        return mockMvc.perform(post("/api/v1/campaigns")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String addSequence(UUID tenantId, String campaignId, String name) throws Exception {
        return mockMvc.perform(post("/api/v1/campaigns/" + campaignId + "/sequences")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String addEmailStep(UUID tenantId, String seqId, int order, int delay, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/sequences/" + seqId + "/steps")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_marketing:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stepOrder": %d, "delayMinutes": %d, "actionType": "EMAIL", "messageBody": "%s"}
                                """.formatted(order, delay, body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String extractId(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asText();
    }
}
