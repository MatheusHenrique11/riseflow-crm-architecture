package com.risecode.riseflow.marketing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.risecode.riseflow.marketing.api.SequenceExecutionService;
import com.risecode.riseflow.marketing.domain.ActionType;
import com.risecode.riseflow.marketing.domain.EmailTemplate;
import com.risecode.riseflow.marketing.domain.EnrollmentStatus;
import com.risecode.riseflow.marketing.domain.SequenceEnrollment;
import com.risecode.riseflow.marketing.domain.SequenceStep;
import com.risecode.riseflow.marketing.persistence.EmailTemplateRepository;
import com.risecode.riseflow.marketing.persistence.SequenceEnrollmentRepository;
import com.risecode.riseflow.marketing.persistence.SequenceStepRepository;
import com.risecode.riseflow.marketing.sender.EmailSender;
import com.risecode.riseflow.marketing.sender.WhatsAppSender;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SequenceExecutionServiceTest {

    @Mock
    private SequenceEnrollmentRepository enrollmentRepository;
    @Mock
    private SequenceStepRepository stepRepository;
    @Mock
    private EmailTemplateRepository templateRepository;
    @Mock
    private EmailSender emailSender;
    @Mock
    private WhatsAppSender whatsAppSender;

    @InjectMocks
    private SequenceExecutionService executionService;

    private SequenceEnrollment enrollment(int currentStepOrder) {
        SequenceEnrollment e = new SequenceEnrollment();
        e.setId(UUID.randomUUID());
        e.setTenantId(UUID.randomUUID());
        e.setSequenceId(UUID.randomUUID());
        e.setContactEmail("user@test.com");
        e.setCurrentStepOrder(currentStepOrder);
        e.setStatus(EnrollmentStatus.ACTIVE);
        e.setNextExecutionTime(Instant.now().minusSeconds(5));
        return e;
    }

    private SequenceStep stepOf(ActionType type, String body) {
        SequenceStep s = new SequenceStep();
        s.setId(UUID.randomUUID());
        s.setActionType(type);
        s.setMessageBody(body);
        s.setDelayMinutes(10);
        return s;
    }

    @Test
    void shouldSendEmailAndAdvanceEnrollment() {
        SequenceEnrollment e = enrollment(0);
        SequenceStep step1 = stepOf(ActionType.EMAIL, "Hello");
        SequenceStep step2 = stepOf(ActionType.WAIT, null);

        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 1)).thenReturn(Optional.of(step1));
        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 2)).thenReturn(Optional.of(step2));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executionService.processEnrollment(e);

        verify(emailSender).sendEmail("user@test.com", "Message", "Hello");
        assertThat(e.getCurrentStepOrder()).isEqualTo(1);
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(e.getNextExecutionTime()).isAfter(Instant.now().minusSeconds(1));
    }

    @Test
    void shouldCompleteEnrollmentWhenNoMoreSteps() {
        SequenceEnrollment e = enrollment(1);

        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 2)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executionService.processEnrollment(e);

        verify(emailSender, never()).sendEmail(any(), any(), any());
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void shouldCompleteEnrollmentAfterLastStep() {
        SequenceEnrollment e = enrollment(0);
        SequenceStep lastStep = stepOf(ActionType.EMAIL, "Final");

        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 1)).thenReturn(Optional.of(lastStep));
        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 2)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executionService.processEnrollment(e);

        verify(emailSender).sendEmail(any(), any(), eq("Final"));
        assertThat(e.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void shouldSendWhatsApp() {
        SequenceEnrollment e = enrollment(0);
        SequenceStep step = stepOf(ActionType.WHATSAPP, "Hi via WA");

        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 1)).thenReturn(Optional.of(step));
        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 2)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executionService.processEnrollment(e);

        verify(whatsAppSender).sendMessage("user@test.com", "Hi via WA");
    }

    @Test
    void shouldSkipSendingForWaitStep() {
        SequenceEnrollment e = enrollment(0);
        SequenceStep waitStep = stepOf(ActionType.WAIT, null);
        SequenceStep nextStep = stepOf(ActionType.EMAIL, "Next");

        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 1)).thenReturn(Optional.of(waitStep));
        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 2)).thenReturn(Optional.of(nextStep));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executionService.processEnrollment(e);

        verify(emailSender, never()).sendEmail(any(), any(), any());
        verify(whatsAppSender, never()).sendMessage(any(), any());
        assertThat(e.getCurrentStepOrder()).isEqualTo(1);
    }

    @Test
    void shouldUseEmailTemplateWhenPresent() {
        SequenceEnrollment e = enrollment(0);
        UUID templateId = UUID.randomUUID();
        SequenceStep step = stepOf(ActionType.EMAIL, "fallback");
        step.setTemplateId(templateId);

        EmailTemplate template = new EmailTemplate();
        template.setSubject("Welcome");
        template.setBody("Template body");

        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 1)).thenReturn(Optional.of(step));
        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 2)).thenReturn(Optional.empty());
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executionService.processEnrollment(e);

        verify(emailSender).sendEmail("user@test.com", "Welcome", "Template body");
    }

    @Test
    void shouldSkipStepWhenContactEmailMissing() {
        SequenceEnrollment e = enrollment(0);
        e.setContactEmail(null);
        SequenceStep step = stepOf(ActionType.EMAIL, "body");

        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 1)).thenReturn(Optional.of(step));
        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 2)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        executionService.processEnrollment(e);

        verify(emailSender, never()).sendEmail(any(), any(), any());
    }

    @Test
    void shouldCalculateNextExecutionTimeFromStepDelay() {
        SequenceEnrollment e = enrollment(0);
        SequenceStep step1 = stepOf(ActionType.WAIT, null);
        SequenceStep step2 = stepOf(ActionType.EMAIL, "body");
        step2.setDelayMinutes(30);

        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 1)).thenReturn(Optional.of(step1));
        when(stepRepository.findBySequenceIdAndStepOrder(e.getSequenceId(), 2)).thenReturn(Optional.of(step2));
        when(enrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        executionService.processEnrollment(e);

        assertThat(e.getNextExecutionTime()).isAfter(before.plusSeconds(29 * 60));
        assertThat(e.getNextExecutionTime()).isBefore(before.plusSeconds(31 * 60));
    }
}
