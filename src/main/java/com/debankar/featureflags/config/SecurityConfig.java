package com.debankar.featureflags.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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

    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator/health",
            "/api-docs",
            "/swagger-ui"
    );

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
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
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

                String path = request.getRequestURI();

                boolean isPublic = PUBLIC_PATHS.stream()
                        .anyMatch(path::startsWith);

                if (isPublic) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String apiKey = request.getHeader("X-API-Key");

                if (apiKey == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(
                            "{\"error\": \"Missing X-API-Key header\"}"
                    );
                    return;
                }

                if (path.startsWith("/admin") && apiKey.equals(adminApiKey)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if ((path.startsWith("/evaluate") ||
                        path.startsWith("/stream")) &&
                        apiKey.equals(clientApiKey)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(
                        "{\"error\": \"Invalid or insufficient API key\"}"
                );
            }
        };
    }
}
