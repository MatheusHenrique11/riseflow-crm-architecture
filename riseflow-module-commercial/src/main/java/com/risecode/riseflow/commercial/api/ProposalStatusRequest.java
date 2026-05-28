package com.risecode.riseflow.commercial.api;

import com.risecode.riseflow.commercial.domain.ProposalStatus;
import jakarta.validation.constraints.NotNull;

public record ProposalStatusRequest(@NotNull ProposalStatus status) {
}
