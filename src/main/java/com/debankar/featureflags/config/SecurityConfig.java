package com.debankar.featureflags.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${feature-flags.admin-api-key}")
    private String adminApiKey;

    @Value("${feature-flags.client-api-key}")
    private String clientApiKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        apiKeyFilter(),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/evaluate/**", "/stream/**")
                        .hasAnyRole("ADMIN", "CLIENT")
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public OncePerRequestFilter apiKeyFilter() {
        return new OncePerRequestFilter() {

            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String path   = request.getRequestURI();
                String apiKey = request.getHeader("X-API-Key");

                // Public paths — skip key check entirely
                if (isPublicPath(path)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if (apiKey == null) {
                    sendUnauthorized(response,
                            "Missing X-API-Key header");
                    return;
                }

                // Admin key — grants ROLE_ADMIN
                if (apiKey.equals(adminApiKey)) {
                    setAuthentication("admin",
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                    filterChain.doFilter(request, response);
                    return;
                }

                // Client key — grants ROLE_CLIENT
                if (apiKey.equals(clientApiKey)) {
                    setAuthentication("client",
                            List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
                    filterChain.doFilter(request, response);
                    return;
                }

                sendUnauthorized(response, "Invalid API key");
            }

            private boolean isPublicPath(String path) {
                return path.startsWith("/actuator/health")
                        || path.startsWith("/actuator/prometheus")
                        || path.startsWith("/api-docs")
                        || path.startsWith("/swagger-ui")
                        || path.equals("/error");
            }

            private void setAuthentication(
                    String principal,
                    List<SimpleGrantedAuthority> authorities) {

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                principal, null, authorities);
                SecurityContextHolder.getContext()
                        .setAuthentication(auth);
            }

            private void sendUnauthorized(HttpServletResponse response,
                                          String message)
                    throws IOException {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\": \"" + message + "\"}");
            }
        };
    }
}