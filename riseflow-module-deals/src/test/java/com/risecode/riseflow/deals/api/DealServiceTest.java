package com.risecode.riseflow.deals.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.accounts.customfields.persistence.CustomFieldDefinitionRepository;
import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.deals.domain.Deal;
import com.risecode.riseflow.deals.domain.DealStatus;
import com.risecode.riseflow.deals.domain.Pipeline;
import com.risecode.riseflow.deals.domain.Stage;
import com.risecode.riseflow.deals.persistence.DealRepository;
import com.risecode.riseflow.deals.persistence.PipelineRepository;
import com.risecode.riseflow.deals.persistence.StageRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DealServiceTest {

    @Mock
    private DealRepository dealRepository;

    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private StageRepository stageRepository;

    @Mock
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private DealService dealService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreateDeal_inheritingProbabilityFromStage() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Pipeline pipeline = pipeline(pipelineId, tenantId);
        Stage stage = stage(stageId, tenantId, pipelineId, 50);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage));
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "DEAL"))
                .thenReturn(List.of());
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> {
            Deal d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DealResponse response = dealService.createDeal(new DealRequest(
                "Big Deal", pipelineId, stageId, null, BigDecimal.valueOf(50000), "USD", null, null, null, Map.of()));

        assertThat(response.probability()).isEqualTo(50);
        assertThat(response.status()).isEqualTo(DealStatus.OPEN);
    }

    @Test
    void shouldDeriveDealStatus_WON_whenProbabilityIs100() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline(pipelineId, tenantId)));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage(stageId, tenantId, pipelineId, 100)));
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "DEAL"))
                .thenReturn(List.of());
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> {
            Deal d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DealResponse response = dealService.createDeal(new DealRequest(
                "Won Deal", pipelineId, stageId, null, BigDecimal.ZERO, "USD", null, null, null, Map.of()));

        assertThat(response.status()).isEqualTo(DealStatus.WON);
    }

    @Test
    void shouldDeriveDealStatus_LOST_whenProbabilityIs0() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline(pipelineId, tenantId)));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage(stageId, tenantId, pipelineId, 0)));
        when(customFieldDefinitionRepository.findAllByTenantIdAndEntityType(tenantId, "DEAL"))
                .thenReturn(List.of());
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> {
            Deal d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        DealResponse response = dealService.createDeal(new DealRequest(
                "Lost Deal", pipelineId, stageId, null, BigDecimal.ZERO, "USD", null, null, null, Map.of()));

        assertThat(response.status()).isEqualTo(DealStatus.LOST);
    }

    @Test
    void shouldMoveDeal_toNewStageInSamePipeline() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID newStageId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Deal deal = deal(dealId, tenantId, pipelineId, stageId, 20);
        Stage newStage = stage(newStageId, tenantId, pipelineId, 80);
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(stageRepository.findById(newStageId)).thenReturn(Optional.of(newStage));
        when(dealRepository.save(any(Deal.class))).thenAnswer(inv -> inv.getArgument(0));

        DealResponse response = dealService.moveDeal(dealId, new MoveStageRequest(newStageId));

        assertThat(response.stageId()).isEqualTo(newStageId);
        assertThat(response.probability()).isEqualTo(80);
        assertThat(response.status()).isEqualTo(DealStatus.OPEN);
    }

    @Test
    void shouldRejectMove_whenTargetStageIsInDifferentPipeline() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID otherPipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID foreignStageId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Deal deal = deal(dealId, tenantId, pipelineId, stageId, 20);
        Stage foreignStage = stage(foreignStageId, tenantId, otherPipelineId, 50);
        when(dealRepository.findById(dealId)).thenReturn(Optional.of(deal));
        when(stageRepository.findById(foreignStageId)).thenReturn(Optional.of(foreignStage));

        assertThatThrownBy(() -> dealService.moveDeal(dealId, new MoveStageRequest(foreignStageId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pipeline");
    }

    @Test
    void shouldThrowEntityNotFound_whenDealDoesNotExist() {
        UUID tenantId = UUID.randomUUID();
        UUID dealId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(dealRepository.findById(dealId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dealService.getDeal(dealId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldRejectDealCreation_whenStageNotInPipeline() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID otherPipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(pipeline(pipelineId, tenantId)));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(stage(stageId, tenantId, otherPipelineId, 30)));

        assertThatThrownBy(() -> dealService.createDeal(new DealRequest(
                "Bad Deal", pipelineId, stageId, null, BigDecimal.ZERO, "USD", null, null, null, Map.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pipeline");
    }

    private Pipeline pipeline(UUID id, UUID tenantId) {
        Pipeline p = new Pipeline();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setName("Sales");
        return p;
    }

    private Stage stage(UUID id, UUID tenantId, UUID pipelineId, int probability) {
        Stage s = new Stage();
        s.setId(id);
        s.setTenantId(tenantId);
        s.setPipelineId(pipelineId);
        s.setName("Stage");
        s.setPosition(1);
        s.setProbability(probability);
        return s;
    }

    private Deal deal(UUID id, UUID tenantId, UUID pipelineId, UUID stageId, int probability) {
        Deal d = new Deal();
        d.setId(id);
        d.setTenantId(tenantId);
        d.setPipelineId(pipelineId);
        d.setStageId(stageId);
        d.setTitle("My Deal");
        d.setAmount(BigDecimal.valueOf(10000));
        d.setCurrency("USD");
        d.setProbability(probability);
        d.setStatus(DealStatus.OPEN);
        return d;
    }
}
