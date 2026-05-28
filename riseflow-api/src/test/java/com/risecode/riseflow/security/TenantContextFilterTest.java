package com.risecode.riseflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.risecode.riseflow.core.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantContextFilterTest {
    private final TenantContextFilter filter = new TenantContextFilter();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldExtractTenantIdFromHeaderAndClearAfterRequest() throws Exception {
        UUID tenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", tenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                assertThat(TenantContextHolder.requireTenantId()).isEqualTo(tenantId);

        filter.doFilter(request, response, chain);

        assertThat(TenantContextHolder.getTenantId()).isEmpty();
    }
}
