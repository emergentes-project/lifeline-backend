package com.upc.pe.lifeline.shared.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.upc.pe.lifeline.shared.application.ports.AccessTokenRevocationStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final RestAuthenticationEntryPoint exceptionHandler;
    private final AccessTokenRevocationStore accessTokenRevocationStore;

    public JwtAuthenticationFilter(JwtService jwtService, RestAuthenticationEntryPoint exceptionHandler,
                                   AccessTokenRevocationStore accessTokenRevocationStore) {
        this.jwtService = jwtService;
        this.exceptionHandler = exceptionHandler;
        this.accessTokenRevocationStore = accessTokenRevocationStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            if (!authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                    || authorization.substring(7).isBlank()) {
                throw new BadCredentialsException("Invalid authorization header");
            }
            String token = authorization.substring(7);
            Claims claims = jwtService.parseClaims(token);
            if (accessTokenRevocationStore.isRevoked(token)) {
                throw new BadCredentialsException("Access token has been revoked");
            }
            String authority = "service".equals(claims.get("token_use", String.class))
                    ? "SERVICE" : "ROLE_" + claims.get("role", String.class);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    claims.getSubject(), null, List.of(new SimpleGrantedAuthority(authority)));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException | BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            exceptionHandler.commence(request, response,
                    new BadCredentialsException("Invalid or expired access token"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
