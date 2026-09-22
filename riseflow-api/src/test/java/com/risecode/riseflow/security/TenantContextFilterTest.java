package com.risecode.riseflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.risecode.riseflow.core.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantContextFilterTest {
    private final TenantContextFilter filter = new TenantContextFilter();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExtractTenantIdFromJwtClaimAndClearAfterRequest() throws Exception {
        UUID tenantId = UUID.randomUUID();
        authenticateWithTenantClaim(tenantId);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                assertThat(TenantContextHolder.requireTenantId()).isEqualTo(tenantId);

        filter.doFilter(request, response, chain);

        assertThat(TenantContextHolder.getTenantId()).isEmpty();
    }

    @Test
    void shouldIgnoreClientSuppliedHeaderAndUseJwtClaimInstead() throws Exception {
        UUID legitTenantId = UUID.randomUUID();
        UUID spoofedTenantId = UUID.randomUUID();
        authenticateWithTenantClaim(legitTenantId);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", spoofedTenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                assertThat(TenantContextHolder.requireTenantId()).isEqualTo(legitTenantId);

        filter.doFilter(request, response, chain);

        assertThat(TenantContextHolder.getTenantId()).isEmpty();
    }

    @Test
    void shouldLeaveTenantUnresolvedWhenNoJwtAuthenticationIsPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", UUID.randomUUID().toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                assertThat(TenantContextHolder.getTenantId()).isEmpty();

        filter.doFilter(request, response, chain);

        assertThat(TenantContextHolder.getTenantId()).isEmpty();
    }

    private void authenticateWithTenantClaim(UUID tenantId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("tenant_id", tenantId.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
