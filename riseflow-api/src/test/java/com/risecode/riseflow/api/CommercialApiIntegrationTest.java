package com.risecode.riseflow.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risecode.riseflow.RiseFlowApplication;
import java.time.LocalDate;
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
class CommercialApiIntegrationTest {

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

    @Test
    void shouldCreateProposalWithItems_andCalculateTotal() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String validUntil = LocalDate.now().plusDays(30).toString();

        mockMvc.perform(post("/api/v1/proposals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_proposal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "title": "Software License",
                                  "validUntil": "%s",
                                  "items": [
                                    {"description": "License A", "quantity": 2, "unitPrice": 500, "orderNo": 1},
                                    {"description": "License B", "quantity": 1, "unitPrice": 300, "orderNo": 2}
                                  ]
                                }
                                """.formatted(accountId, validUntil)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", equalTo("DRAFT")))
                .andExpect(jsonPath("$.totalValue").value(1300))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void shouldFullProposalToInvoiceFlow() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String validUntil = LocalDate.now().plusDays(30).toString();

        // 1. Create proposal
        String proposalLocation = mockMvc.perform(post("/api/v1/proposals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_proposal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "title": "Annual Contract",
                                  "validUntil": "%s",
                                  "items": [
                                    {"description": "Consulting", "quantity": 10, "unitPrice": 200, "orderNo": 1}
                                  ]
                                }
                                """.formatted(accountId, validUntil)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        String proposalId = lastSegment(proposalLocation);

        // 2. DRAFT → SENT
        mockMvc.perform(put("/api/v1/proposals/" + proposalId + "/status")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_proposal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "SENT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("SENT")));

        // 3. SENT → ACCEPTED
        mockMvc.perform(put("/api/v1/proposals/" + proposalId + "/status")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_proposal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACCEPTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("ACCEPTED")));

        // 4. Generate invoice from proposal
        String invoiceLocation = mockMvc.perform(post("/api/v1/proposals/" + proposalId + "/generate-invoice")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_invoice:write"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", equalTo("PENDING")))
                .andExpect(jsonPath("$.totalAmount").value(2000))
                .andExpect(jsonPath("$.proposalId", equalTo(proposalId)))
                .andReturn().getResponse().getHeader("Location");

        String invoiceId = lastSegment(invoiceLocation);

        // 5. Cannot generate invoice again from same proposal
        mockMvc.perform(post("/api/v1/proposals/" + proposalId + "/generate-invoice")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_invoice:write"))))
                .andExpect(status().isUnprocessableEntity());

        // 6. Pay invoice
        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/pay")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_invoice:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod": "BANK_TRANSFER", "amount": 2000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("PAID")))
                .andExpect(jsonPath("$.paidAt", notNullValue()));
    }

    @Test
    void shouldCreateInvoiceDirectly_andMarkAsPaid() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String dueDate = LocalDate.now().plusDays(15).toString();

        String invoiceLocation = mockMvc.perform(post("/api/v1/invoices")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_invoice:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "dueDate": "%s",
                                  "items": [
                                    {"description": "Product X", "quantity": 5, "unitPrice": 100, "orderNo": 1}
                                  ]
                                }
                                """.formatted(accountId, dueDate)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber", notNullValue()))
                .andExpect(jsonPath("$.totalAmount").value(500))
                .andReturn().getResponse().getHeader("Location");

        String invoiceId = lastSegment(invoiceLocation);

        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/cancel")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_invoice:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("CANCELLED")));
    }

    @Test
    void shouldCreateCommission_whenDealMovedToWon_andApproveAfterInvoicePaid() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID responsibleId = UUID.randomUUID();
        String validUntil = LocalDate.now().plusDays(30).toString();

        // 1. Create pipeline + stages + deal
        String pipelineBody = createPipeline(tenantId, "Sales");
        String pipelineId = extractField(pipelineBody, "id");

        String openStageBody = addStage(tenantId, pipelineId, "Open", 1, 50);
        String openStageId = extractField(openStageBody, "id");

        String wonStageBody = addStage(tenantId, pipelineId, "Won", 2, 100);
        String wonStageId = extractField(wonStageBody, "id");

        String dealLocation = mockMvc.perform(post("/api/v1/deals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Won Deal",
                                  "pipelineId": "%s",
                                  "stageId": "%s",
                                  "responsibleId": "%s",
                                  "amount": 5000,
                                  "currency": "USD",
                                  "customFields": {}
                                }
                                """.formatted(pipelineId, openStageId, responsibleId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        String dealId = lastSegment(dealLocation);

        // 2. Move deal to WON → triggers DealWonEvent → commission created
        mockMvc.perform(post("/api/v1/deals/" + dealId + "/move")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stageId": "%s"}
                                """.formatted(wonStageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("WON")));

        // 3. Commission should be PENDING
        mockMvc.perform(get("/api/v1/commissions?dealId=" + dealId)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_commission:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", equalTo("PENDING")))
                .andExpect(jsonPath("$[0].amount").value(500)); // 10% of 5000

        // 4. Create proposal → send → accept → generate invoice → pay
        String proposalLocation = mockMvc.perform(post("/api/v1/proposals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_proposal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "dealId": "%s",
                                  "title": "Proposal for Won Deal",
                                  "validUntil": "%s",
                                  "items": [
                                    {"description": "Service", "quantity": 1, "unitPrice": 5000, "orderNo": 1}
                                  ]
                                }
                                """.formatted(UUID.randomUUID(), dealId, validUntil)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        String proposalId = lastSegment(proposalLocation);

        changeProposalStatus(tenantId, proposalId, "SENT");
        changeProposalStatus(tenantId, proposalId, "ACCEPTED");

        String invoiceLocation = mockMvc.perform(post("/api/v1/proposals/" + proposalId + "/generate-invoice")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_invoice:write"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        String invoiceId = lastSegment(invoiceLocation);

        // 5. Pay invoice → triggers InvoicePaidEvent → commission APPROVED
        mockMvc.perform(post("/api/v1/invoices/" + invoiceId + "/pay")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_invoice:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod": "WIRE_TRANSFER", "amount": 5000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("PAID")));

        // 6. Commission should now be APPROVED
        String commissionListBody = mockMvc.perform(get("/api/v1/commissions?dealId=" + dealId)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_commission:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status", equalTo("APPROVED")))
                .andReturn().getResponse().getContentAsString();

        String commissionId = extractField(objectMapper.readTree(commissionListBody).get(0).toString(), "id");

        // 7. Pay commission
        mockMvc.perform(post("/api/v1/commissions/" + commissionId + "/pay")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_commission:write"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("PAID")))
                .andExpect(jsonPath("$.paidAt", notNullValue()));
    }

    @Test
    void shouldIsolateProposalsBetweenTenants() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        String validUntil = LocalDate.now().plusDays(30).toString();

        createProposal(tenantA, "Proposal of A", validUntil);
        createProposal(tenantB, "Proposal of B", validUntil);

        mockMvc.perform(get("/api/v1/proposals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantA.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_proposal:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo("Proposal of A")));
    }

    @Test
    void shouldRejectInvalidStatusTransition() throws Exception {
        UUID tenantId = UUID.randomUUID();
        String validUntil = LocalDate.now().plusDays(30).toString();

        String proposalLocation = createProposal(tenantId, "Test Proposal", validUntil);
        String proposalId = lastSegment(proposalLocation);

        // DRAFT → ACCEPTED is invalid (must go through SENT)
        mockMvc.perform(put("/api/v1/proposals/" + proposalId + "/status")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_proposal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACCEPTED"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- Private helpers ---

    private String createPipeline(UUID tenantId, String name) throws Exception {
        return mockMvc.perform(post("/api/v1/pipelines")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_pipeline:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "defaultPipeline": false, "stages": []}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String addStage(UUID tenantId, String pipelineId, String name, int position, int probability)
            throws Exception {
        return mockMvc.perform(post("/api/v1/pipelines/" + pipelineId + "/stages")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_pipeline:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "position": %d, "probability": %d}
                                """.formatted(name, position, probability)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String createProposal(UUID tenantId, String title, String validUntil) throws Exception {
        return mockMvc.perform(post("/api/v1/proposals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_proposal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "%s",
                                  "title": "%s",
                                  "validUntil": "%s",
                                  "items": [
                                    {"description": "Item", "quantity": 1, "unitPrice": 100, "orderNo": 1}
                                  ]
                                }
                                """.formatted(UUID.randomUUID(), title, validUntil)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
    }

    private void changeProposalStatus(UUID tenantId, String proposalId, String status) throws Exception {
        mockMvc.perform(put("/api/v1/proposals/" + proposalId + "/status")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_proposal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "%s"}
                                """.formatted(status)))
                .andExpect(status().isOk());
    }

    private String lastSegment(String uri) {
        return uri.substring(uri.lastIndexOf('/') + 1);
    }

    private String extractField(String json, String field) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get(field).asText();
    }
}
