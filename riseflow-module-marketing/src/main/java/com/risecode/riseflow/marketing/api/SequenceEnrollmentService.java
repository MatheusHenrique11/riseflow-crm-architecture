package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.core.exception.EntityNotFoundException;
import com.risecode.riseflow.core.exception.TenantMismatchException;
import com.risecode.riseflow.core.tenant.TenantContextHolder;
import com.risecode.riseflow.marketing.domain.EnrollmentStatus;
import com.risecode.riseflow.marketing.domain.SequenceEnrollment;
import com.risecode.riseflow.marketing.persistence.SequenceEnrollmentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SequenceEnrollmentService {

    private final SequenceEnrollmentRepository enrollmentRepository;

    public SequenceEnrollmentService(SequenceEnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> listEnrollments(EnrollmentStatus status) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        List<SequenceEnrollment> enrollments = (status != null)
                ? enrollmentRepository.findAllByTenantIdAndStatus(tenantId, status)
                : enrollmentRepository.findAllByTenantId(tenantId);
        return enrollments.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EnrollmentResponse cancelEnrollment(UUID id) {
        UUID tenantId = TenantContextHolder.requireTenantId();
        SequenceEnrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment not found: " + id));
        if (!enrollment.getTenantId().equals(tenantId)) {
            throw new TenantMismatchException("Enrollment tenant does not match");
        }
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        return toResponse(enrollmentRepository.save(enrollment));
    }

    EnrollmentResponse toResponse(SequenceEnrollment e) {
        return new EnrollmentResponse(e.getId(), e.getTenantId(), e.getEntityType(),
                e.getEntityId(), e.getSequenceId(), e.getCurrentStepOrder(),
                e.getStatus(), e.getContactEmail(), e.getNextExecutionTime());
    }
}
