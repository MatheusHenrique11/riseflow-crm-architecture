package com.risecode.riseflow.marketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.core.exception.BusinessException;
import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.marketing.api.CampaignRequest;
import com.risecode.riseflow.marketing.api.CampaignResponse;
import com.risecode.riseflow.marketing.api.CampaignService;
import com.risecode.riseflow.marketing.api.CampaignStatusRequest;
import com.risecode.riseflow.marketing.api.SequenceRequest;
import com.risecode.riseflow.marketing.api.SequenceStepRequest;
import com.risecode.riseflow.marketing.domain.ActionType;
import com.risecode.riseflow.marketing.domain.Campaign;
import com.risecode.riseflow.marketing.domain.CampaignStatus;
import com.risecode.riseflow.marketing.domain.EnrollmentStatus;
import com.risecode.riseflow.marketing.domain.Sequence;
import com.risecode.riseflow.marketing.domain.SequenceStep;
import com.risecode.riseflow.marketing.persistence.CampaignRepository;
import com.risecode.riseflow.marketing.persistence.EmailTemplateRepository;
import com.risecode.riseflow.marketing.persistence.SequenceEnrollmentRepository;
import com.risecode.riseflow.marketing.persistence.SequenceRepository;
import com.risecode.riseflow.marketing.persistence.SequenceStepRepository;
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
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private SequenceRepository sequenceRepository;
    @Mock
    private SequenceStepRepository stepRepository;
    @Mock
    private EmailTemplateRepository templateRepository;
    @Mock
    private SequenceEnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CampaignService campaignService;

    private final UUID tenantId = UUID.randomUUID();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    private void setTenant() {
        TenantContextHolder.setTenantId(tenantId);
    }

    private Campaign savedCampaign(UUID id, CampaignStatus status) {
        Campaign c = new Campaign();
        c.setId(id);
        c.setTenantId(tenantId);
        c.setName("Test");
        c.setStatus(status);
        return c;
    }

    @Test
    void shouldCreateCampaign() {
        setTenant();
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> {
            Campaign c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(sequenceRepository.findAllByCampaignId(any())).thenReturn(List.of());

        CampaignResponse response = campaignService.createCampaign(new CampaignRequest("Q4 Promo", "desc"));

        assertThat(response.name()).isEqualTo("Q4 Promo");
        assertThat(response.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(response.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void shouldListCampaigns() {
        setTenant();
        Campaign c = savedCampaign(UUID.randomUUID(), CampaignStatus.DRAFT);
        when(campaignRepository.findAllByTenantId(tenantId)).thenReturn(List.of(c));
        when(sequenceRepository.findAllByCampaignId(c.getId())).thenReturn(List.of());

        List<CampaignResponse> result = campaignService.listCampaigns();

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldActivateCampaignWithSequenceAndSteps() {
        setTenant();
        UUID id = UUID.randomUUID();
        Campaign campaign = savedCampaign(id, CampaignStatus.DRAFT);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));

        Sequence seq = new Sequence();
        seq.setId(UUID.randomUUID());
        seq.setTenantId(tenantId);
        seq.setName("Seq");
        when(sequenceRepository.findAllByCampaignId(id)).thenReturn(List.of(seq));

        SequenceStep step = new SequenceStep();
        step.setId(UUID.randomUUID());
        step.setStepOrder(1);
        step.setActionType(ActionType.EMAIL);
        when(stepRepository.findAllBySequenceIdOrderByStepOrder(seq.getId())).thenReturn(List.of(step));

        when(campaignRepository.save(campaign)).thenReturn(campaign);
        when(sequenceRepository.findAllByCampaignId(campaign.getId())).thenReturn(List.of(seq));

        CampaignResponse response = campaignService.changeStatus(id, new CampaignStatusRequest(CampaignStatus.ACTIVE));

        assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void shouldRejectActivationWithoutSequences() {
        setTenant();
        UUID id = UUID.randomUUID();
        Campaign campaign = savedCampaign(id, CampaignStatus.DRAFT);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));
        when(sequenceRepository.findAllByCampaignId(id)).thenReturn(List.of());

        assertThatThrownBy(() -> campaignService.changeStatus(id, new CampaignStatusRequest(CampaignStatus.ACTIVE)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sequences");
    }

    @Test
    void shouldRejectActivationWhenSequenceHasNoSteps() {
        setTenant();
        UUID id = UUID.randomUUID();
        Campaign campaign = savedCampaign(id, CampaignStatus.DRAFT);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));

        Sequence seq = new Sequence();
        seq.setId(UUID.randomUUID());
        seq.setTenantId(tenantId);
        seq.setName("Empty");
        when(sequenceRepository.findAllByCampaignId(id)).thenReturn(List.of(seq));
        when(stepRepository.findAllBySequenceIdOrderByStepOrder(seq.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> campaignService.changeStatus(id, new CampaignStatusRequest(CampaignStatus.ACTIVE)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("steps");
    }

    @Test
    void shouldRejectChangeStatusOnCompletedCampaign() {
        setTenant();
        UUID id = UUID.randomUUID();
        Campaign campaign = savedCampaign(id, CampaignStatus.COMPLETED);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> campaignService.changeStatus(id, new CampaignStatusRequest(CampaignStatus.DRAFT)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void shouldRejectStepWithDuplicateOrder() {
        setTenant();
        UUID seqId = UUID.randomUUID();
        Sequence seq = new Sequence();
        seq.setId(seqId);
        seq.setTenantId(tenantId);
        when(sequenceRepository.findById(seqId)).thenReturn(Optional.of(seq));
        when(stepRepository.existsBySequenceIdAndStepOrder(seqId, 1)).thenReturn(true);

        assertThatThrownBy(() -> campaignService.addStep(seqId,
                new SequenceStepRequest(1, 0, ActionType.WAIT, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldRejectEmailStepWithoutTemplateOrBody() {
        setTenant();
        UUID seqId = UUID.randomUUID();
        Sequence seq = new Sequence();
        seq.setId(seqId);
        seq.setTenantId(tenantId);
        when(sequenceRepository.findById(seqId)).thenReturn(Optional.of(seq));
        when(stepRepository.existsBySequenceIdAndStepOrder(seqId, 1)).thenReturn(false);

        assertThatThrownBy(() -> campaignService.addStep(seqId,
                new SequenceStepRequest(1, 0, ActionType.EMAIL, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("template or message body");
    }

    @Test
    void shouldDeleteCampaignWithNoActiveEnrollments() {
        setTenant();
        UUID id = UUID.randomUUID();
        Campaign campaign = savedCampaign(id, CampaignStatus.DRAFT);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));
        when(sequenceRepository.findAllByCampaignId(id)).thenReturn(List.of());

        campaignService.deleteCampaign(id);

        verify(campaignRepository).delete(campaign);
    }

    @Test
    void shouldBlockDeletionWithActiveEnrollments() {
        setTenant();
        UUID id = UUID.randomUUID();
        Campaign campaign = savedCampaign(id, CampaignStatus.ACTIVE);
        when(campaignRepository.findById(id)).thenReturn(Optional.of(campaign));

        Sequence seq = new Sequence();
        seq.setId(UUID.randomUUID());
        seq.setTenantId(tenantId);
        when(sequenceRepository.findAllByCampaignId(id)).thenReturn(List.of(seq));
        when(enrollmentRepository.existsBySequenceIdAndStatus(seq.getId(), EnrollmentStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> campaignService.deleteCampaign(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("active enrollments");
    }

    @Test
    void shouldThrowWhenCampaignNotFound() {
        setTenant();
        UUID id = UUID.randomUUID();
        when(campaignRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.getCampaign(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldAddSequenceToCampaign() {
        setTenant();
        UUID campaignId = UUID.randomUUID();
        Campaign campaign = savedCampaign(campaignId, CampaignStatus.DRAFT);
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

        Sequence seq = new Sequence();
        seq.setId(UUID.randomUUID());
        seq.setTenantId(tenantId);
        seq.setCampaignId(campaignId);
        seq.setName("Onboarding");
        when(sequenceRepository.save(any())).thenReturn(seq);
        when(stepRepository.findAllBySequenceIdOrderByStepOrder(seq.getId())).thenReturn(List.of());

        var response = campaignService.addSequence(campaignId, new SequenceRequest("Onboarding"));

        assertThat(response.name()).isEqualTo("Onboarding");
        assertThat(response.campaignId()).isEqualTo(campaignId);
    }
}
