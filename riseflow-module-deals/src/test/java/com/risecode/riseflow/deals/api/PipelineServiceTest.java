package com.risecode.riseflow.deals.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.deals.domain.Pipeline;
import com.risecode.riseflow.deals.domain.Stage;
import com.risecode.riseflow.deals.persistence.DealRepository;
import com.risecode.riseflow.deals.persistence.PipelineRepository;
import com.risecode.riseflow.deals.persistence.StageRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock
    private PipelineRepository pipelineRepository;

    @Mock
    private StageRepository stageRepository;

    @Mock
    private DealRepository dealRepository;

    @InjectMocks
    private PipelineService pipelineService;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldCreatePipeline() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(inv -> {
            Pipeline p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PipelineResponse response = pipelineService.createPipeline(
                new PipelineRequest("Sales", "Main sales pipeline", false, List.of()));

        assertThat(response.name()).isEqualTo("Sales");
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.defaultPipeline()).isFalse();
    }

    @Test
    void shouldClearPreviousDefault_whenNewDefaultIsCreated() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Pipeline existingDefault = pipeline(UUID.randomUUID(), tenantId, "Old Default", true);
        when(pipelineRepository.findAllByTenantId(tenantId)).thenReturn(List.of(existingDefault));
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(inv -> {
            Pipeline p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        pipelineService.createPipeline(new PipelineRequest("New Default", null, true, List.of()));

        assertThat(existingDefault.isDefaultPipeline()).isFalse();
        verify(pipelineRepository).save(existingDefault);
    }

    @Test
    void shouldNotClearDefaults_whenCreatingNonDefaultPipeline() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(inv -> {
            Pipeline p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        pipelineService.createPipeline(new PipelineRequest("Side Pipeline", null, false, List.of()));

        verify(pipelineRepository, never()).findAllByTenantId(tenantId);
    }

    @Test
    void shouldPreventDeletion_whenPipelineHasDeals() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        Pipeline p = pipeline(pipelineId, tenantId, "Sales", false);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(p));
        when(dealRepository.existsByPipelineId(pipelineId)).thenReturn(true);

        assertThatThrownBy(() -> pipelineService.deletePipeline(pipelineId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deals");
    }

    @Test
    void shouldDeletePipeline_whenNoDealExists() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        Pipeline p = pipeline(pipelineId, tenantId, "Empty Pipeline", false);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(p));
        when(dealRepository.existsByPipelineId(pipelineId)).thenReturn(false);

        pipelineService.deletePipeline(pipelineId);

        verify(pipelineRepository).delete(p);
    }

    @Test
    void shouldAddStage_withUniquePosition() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        Pipeline p = pipeline(pipelineId, tenantId, "Sales", false);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(p));
        when(stageRepository.existsByPipelineIdAndPosition(pipelineId, 1)).thenReturn(false);
        when(stageRepository.save(any(Stage.class))).thenAnswer(inv -> {
            Stage s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        StageResponse response = pipelineService.addStage(pipelineId,
                new StageRequest("Qualification", 1, 20, null));

        assertThat(response.name()).isEqualTo("Qualification");
        assertThat(response.position()).isEqualTo(1);
        assertThat(response.probability()).isEqualTo(20);
    }

    @Test
    void shouldRejectStage_whenPositionAlreadyExists() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        Pipeline p = pipeline(pipelineId, tenantId, "Sales", false);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(p));
        when(stageRepository.existsByPipelineIdAndPosition(pipelineId, 1)).thenReturn(true);

        assertThatThrownBy(() -> pipelineService.addStage(pipelineId, new StageRequest("Dup", 1, 20, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("position");
    }

    @Test
    void shouldPreventStageDeletion_whenStageHasDeals() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        Pipeline p = pipeline(pipelineId, tenantId, "Sales", false);
        Stage s = stage(stageId, tenantId, pipelineId, "Prospect", 1);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(p));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(s));
        when(dealRepository.existsByStageId(stageId)).thenReturn(true);

        assertThatThrownBy(() -> pipelineService.deleteStage(pipelineId, stageId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("deals");
    }

    @Test
    void shouldDeleteStage_whenNoDealsExist() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        Pipeline p = pipeline(pipelineId, tenantId, "Sales", false);
        Stage s = stage(stageId, tenantId, pipelineId, "Prospect", 1);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.of(p));
        when(stageRepository.findById(stageId)).thenReturn(Optional.of(s));
        when(dealRepository.existsByStageId(stageId)).thenReturn(false);

        pipelineService.deleteStage(pipelineId, stageId);

        verify(stageRepository).delete(s);
    }

    @Test
    void shouldThrowEntityNotFound_whenPipelineDoesNotExist() {
        UUID tenantId = UUID.randomUUID();
        UUID pipelineId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        when(pipelineRepository.findById(pipelineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineService.getPipeline(pipelineId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private Pipeline pipeline(UUID id, UUID tenantId, String name, boolean isDefault) {
        Pipeline p = new Pipeline();
        p.setId(id);
        p.setTenantId(tenantId);
        p.setName(name);
        p.setDefaultPipeline(isDefault);
        return p;
    }

    private Stage stage(UUID id, UUID tenantId, UUID pipelineId, String name, int position) {
        Stage s = new Stage();
        s.setId(id);
        s.setTenantId(tenantId);
        s.setPipelineId(pipelineId);
        s.setName(name);
        s.setPosition(position);
        s.setProbability(20);
        return s;
    }
}
