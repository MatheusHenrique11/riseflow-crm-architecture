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
class PipelineDealIntegrationTest {

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
    void shouldCreatePipelineWithStagesAndRetrieveIt() throws Exception {
        UUID tenantId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/pipelines")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_pipeline:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Sales Pipeline",
                                  "description": "Main sales funnel",
                                  "defaultPipeline": true,
                                  "stages": [
                                    {"name": "Prospect", "position": 1, "probability": 10},
                                    {"name": "Qualified", "position": 2, "probability": 40}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", equalTo("Sales Pipeline")))
                .andExpect(jsonPath("$.defaultPipeline", equalTo(true)))
                .andExpect(jsonPath("$.stages", hasSize(2)));
    }

    @Test
    void shouldCreateDealAndMoveToDifferentStage() throws Exception {
        UUID tenantId = UUID.randomUUID();

        String pipelineBody = createPipeline(tenantId, "Test Pipeline");
        String pipelineId = extractId(pipelineBody);

        String stage1Body = addStage(tenantId, pipelineId, "Prospect", 1, 20);
        String stage1Id = extractStageId(stage1Body);

        String stage2Body = addStage(tenantId, pipelineId, "Proposal", 2, 60);
        String stage2Id = extractStageId(stage2Body);

        String dealLocation = mockMvc.perform(post("/api/v1/deals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Big Deal",
                                  "pipelineId": "%s",
                                  "stageId": "%s",
                                  "amount": 50000,
                                  "currency": "USD",
                                  "customFields": {}
                                }
                                """.formatted(pipelineId, stage1Id)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.probability", equalTo(20)))
                .andExpect(jsonPath("$.status", equalTo("OPEN")))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String dealId = dealLocation.substring(dealLocation.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/v1/deals/" + dealId + "/move")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stageId": "%s"}
                                """.formatted(stage2Id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageId", equalTo(stage2Id)))
                .andExpect(jsonPath("$.probability", equalTo(60)))
                .andExpect(jsonPath("$.status", equalTo("OPEN")));
    }

    @Test
    void shouldPreventDeletion_ofPipelineWithDeals() throws Exception {
        UUID tenantId = UUID.randomUUID();

        String pipelineBody = createPipeline(tenantId, "Busy Pipeline");
        String pipelineId = extractId(pipelineBody);

        String stageBody = addStage(tenantId, pipelineId, "Prospect", 1, 10);
        String stageId = extractStageId(stageBody);

        mockMvc.perform(post("/api/v1/deals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Blocking Deal",
                                  "pipelineId": "%s",
                                  "stageId": "%s",
                                  "amount": 1000,
                                  "currency": "USD",
                                  "customFields": {}
                                }
                                """.formatted(pipelineId, stageId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/pipelines/" + pipelineId)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_pipeline:write"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldListDealsFilteredByPipeline() throws Exception {
        UUID tenantId = UUID.randomUUID();

        String pipeline1Body = createPipeline(tenantId, "Pipeline A");
        String pipeline1Id = extractId(pipeline1Body);
        String stage1Body = addStage(tenantId, pipeline1Id, "Stage", 1, 30);
        String stage1Id = extractStageId(stage1Body);

        String pipeline2Body = createPipeline(tenantId, "Pipeline B");
        String pipeline2Id = extractId(pipeline2Body);
        String stage2Body = addStage(tenantId, pipeline2Id, "Stage", 1, 30);
        String stage2Id = extractStageId(stage2Body);

        createDeal(tenantId, "Deal in A", pipeline1Id, stage1Id);
        createDeal(tenantId, "Deal in B", pipeline2Id, stage2Id);

        mockMvc.perform(get("/api/v1/deals?pipelineId=" + pipeline1Id)
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo("Deal in A")));
    }

    @Test
    void shouldIsolateDealsBetweenTenants() throws Exception {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        String pipelineABody = createPipeline(tenantA, "Pipeline A");
        String pipelineAId = extractId(pipelineABody);
        String stageABody = addStage(tenantA, pipelineAId, "Stage", 1, 30);
        String stageAId = extractStageId(stageABody);
        createDeal(tenantA, "Deal of A", pipelineAId, stageAId);

        String pipelineBBody = createPipeline(tenantB, "Pipeline B");
        String pipelineBId = extractId(pipelineBBody);
        String stageBBody = addStage(tenantB, pipelineBId, "Stage", 1, 30);
        String stageBId = extractStageId(stageBBody);
        createDeal(tenantB, "Deal of B", pipelineBId, stageBId);

        mockMvc.perform(get("/api/v1/deals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantA.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", equalTo("Deal of A")));
    }

    @Test
    void shouldMoveDealToWonStage_whenProbabilityIs100() throws Exception {
        UUID tenantId = UUID.randomUUID();

        String pipelineBody = createPipeline(tenantId, "Close Pipeline");
        String pipelineId = extractId(pipelineBody);
        String openStageBody = addStage(tenantId, pipelineId, "Open", 1, 50);
        String openStageId = extractStageId(openStageBody);
        String wonStageBody = addStage(tenantId, pipelineId, "Won", 2, 100);
        String wonStageId = extractStageId(wonStageBody);

        String dealLocation = mockMvc.perform(post("/api/v1/deals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Closing Deal",
                                  "pipelineId": "%s",
                                  "stageId": "%s",
                                  "amount": 10000,
                                  "currency": "USD",
                                  "customFields": {}
                                }
                                """.formatted(pipelineId, openStageId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String dealId = dealLocation.substring(dealLocation.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/v1/deals/" + dealId + "/move")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stageId": "%s"}
                                """.formatted(wonStageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("WON")))
                .andExpect(jsonPath("$.probability", equalTo(100)));
    }

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
                .andReturn()
                .getResponse()
                .getContentAsString();
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
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void createDeal(UUID tenantId, String title, String pipelineId, String stageId) throws Exception {
        mockMvc.perform(post("/api/v1/deals")
                        .with(jwt()
                                .jwt(t -> t.claim("tenant_id", tenantId.toString()))
                                .authorities(new SimpleGrantedAuthority("SCOPE_deal:write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "pipelineId": "%s",
                                  "stageId": "%s",
                                  "amount": 1000,
                                  "currency": "USD",
                                  "customFields": {}
                                }
                                """.formatted(title, pipelineId, stageId)))
                .andExpect(status().isCreated());
    }

    private String extractId(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asText();
    }

    private String extractStageId(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asText();
    }
}
