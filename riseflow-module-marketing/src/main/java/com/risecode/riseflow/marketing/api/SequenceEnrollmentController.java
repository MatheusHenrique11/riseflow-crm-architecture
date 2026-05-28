package com.risecode.riseflow.marketing.api;

import com.risecode.riseflow.marketing.domain.EnrollmentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
public class SequenceEnrollmentController {

    private final SequenceEnrollmentService enrollmentService;

    public SequenceEnrollmentController(SequenceEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_marketing:read')")
    List<EnrollmentResponse> listEnrollments(@RequestParam(required = false) EnrollmentStatus status) {
        return enrollmentService.listEnrollments(status);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SCOPE_marketing:write')")
    EnrollmentResponse cancelEnrollment(@PathVariable UUID id) {
        return enrollmentService.cancelEnrollment(id);
    }
}
