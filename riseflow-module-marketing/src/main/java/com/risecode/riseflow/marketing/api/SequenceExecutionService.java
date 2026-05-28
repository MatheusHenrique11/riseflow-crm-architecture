package com.risecode.riseflow.marketing.api;

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
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceExecutionService {

    private static final Logger log = LoggerFactory.getLogger(SequenceExecutionService.class);

    private final SequenceEnrollmentRepository enrollmentRepository;
    private final SequenceStepRepository stepRepository;
    private final EmailTemplateRepository templateRepository;
    private final EmailSender emailSender;
    private final WhatsAppSender whatsAppSender;

    public SequenceExecutionService(SequenceEnrollmentRepository enrollmentRepository,
            SequenceStepRepository stepRepository,
            EmailTemplateRepository templateRepository,
            EmailSender emailSender,
            WhatsAppSender whatsAppSender) {
        this.enrollmentRepository = enrollmentRepository;
        this.stepRepository = stepRepository;
        this.templateRepository = templateRepository;
        this.emailSender = emailSender;
        this.whatsAppSender = whatsAppSender;
    }

    @Scheduled(fixedDelayString = "${marketing.execution.delay-ms:10000}")
    @Transactional
    public void processDueEnrollments() {
        List<SequenceEnrollment> due = enrollmentRepository
                .findAllByStatusAndNextExecutionTimeBefore(EnrollmentStatus.ACTIVE, Instant.now());
        for (SequenceEnrollment enrollment : due) {
            try {
                processEnrollment(enrollment);
            } catch (Exception ex) {
                log.error("Error processing enrollment {}: {}", enrollment.getId(), ex.getMessage());
            }
        }
    }

    public void processEnrollment(SequenceEnrollment enrollment) {
        int currentOrder = enrollment.getCurrentStepOrder();
        Optional<SequenceStep> stepOpt = stepRepository
                .findBySequenceIdAndStepOrder(enrollment.getSequenceId(), currentOrder + 1);

        if (stepOpt.isEmpty()) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollmentRepository.save(enrollment);
            log.info("Enrollment {} completed", enrollment.getId());
            return;
        }

        SequenceStep step = stepOpt.get();
        executeStep(step, enrollment);

        Optional<SequenceStep> nextStep = stepRepository
                .findBySequenceIdAndStepOrder(enrollment.getSequenceId(), currentOrder + 2);

        enrollment.setCurrentStepOrder(currentOrder + 1);
        if (nextStep.isPresent()) {
            long delaySeconds = (long) nextStep.get().getDelayMinutes() * 60;
            enrollment.setNextExecutionTime(Instant.now().plusSeconds(delaySeconds));
        } else {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
        }
        enrollmentRepository.save(enrollment);
    }

    private void executeStep(SequenceStep step, SequenceEnrollment enrollment) {
        String contact = enrollment.getContactEmail();
        if (step.getActionType() == ActionType.WAIT) {
            log.info("WAIT step for enrollment {}", enrollment.getId());
            return;
        }
        if (contact == null || contact.isBlank()) {
            log.warn("No contact info for enrollment {}, skipping step {}", enrollment.getId(), step.getId());
            return;
        }
        if (step.getActionType() == ActionType.EMAIL) {
            String subject = "Message";
            String body = step.getMessageBody();
            if (step.getTemplateId() != null) {
                Optional<EmailTemplate> tmpl = templateRepository.findById(step.getTemplateId());
                if (tmpl.isPresent()) {
                    subject = tmpl.get().getSubject();
                    body = tmpl.get().getBody();
                }
            }
            emailSender.sendEmail(contact, subject, body != null ? body : "");
        } else if (step.getActionType() == ActionType.WHATSAPP) {
            whatsAppSender.sendMessage(contact, step.getMessageBody() != null ? step.getMessageBody() : "");
        }
    }
}
