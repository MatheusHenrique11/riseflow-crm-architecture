package com.risecode.riseflow.accounts.search;

import jakarta.validation.Valid;
import java.util.List;

public record SearchRequest(
        @Valid List<SearchCriteria> criteria,
        int page,
        int size
) {
    public SearchRequest {
        if (criteria == null) criteria = List.of();
        if (page < 0) page = 0;
        if (size <= 0 || size > 200) size = 20;
    }
}
