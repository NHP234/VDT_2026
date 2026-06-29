package com.vdt2026.omnicare.inbox.config;

import com.vdt2026.omnicare.inbox.identity.application.AccessTokenService;
import com.vdt2026.omnicare.inbox.identity.application.AuthenticatedAgent;
import com.vdt2026.omnicare.inbox.identity.infrastructure.persistence.AgentRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenService accessTokenService;
    private final AgentRepository agentRepository;

    BearerTokenAuthenticationFilter(AccessTokenService accessTokenService, AgentRepository agentRepository) {
        this.accessTokenService = accessTokenService;
        this.agentRepository = agentRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            authenticate(authorization.substring(BEARER_PREFIX.length()));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        accessTokenService.verify(token)
            .filter(agent -> agentRepository.findById(agent.id()).filter(activeAgent -> activeAgent.active()).isPresent())
            .ifPresent(agent -> {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    agent,
                    token,
                    List.of(new SimpleGrantedAuthority("ROLE_AGENT"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
    }
}
