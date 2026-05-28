package com.risecode.riseflow.security;

import com.risecode.riseflow.core.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextFilter extends OncePerRequestFilter {
    static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            resolveTenantId(request);
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void resolveTenantId(HttpServletRequest request) {
        String headerTenantId = request.getHeader(TENANT_HEADER);
        if (headerTenantId != null && !headerTenantId.isBlank()) {
            TenantContextHolder.setTenantId(UUID.fromString(headerTenantId));
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Jwt jwt = jwtAuthentication.getToken();
            String tenantId = jwt.getClaimAsString("tenant_id");
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContextHolder.setTenantId(UUID.fromString(tenantId));
            }
        }
    }
}
