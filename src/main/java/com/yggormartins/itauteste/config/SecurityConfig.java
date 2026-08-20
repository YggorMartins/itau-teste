package com.yggormartins.itauteste.config;

import com.yggormartins.itauteste.security.ClientIdentityResolver;
import com.yggormartins.itauteste.security.RateLimitFilter;
import com.yggormartins.itauteste.security.SecurityProblemWriter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
    @Bean
    RateLimitFilter rateLimitFilter(ClientIdentityResolver identityResolver,
                                    AppProperties properties,
                                    MeterRegistry meterRegistry) {
        return new RateLimitFilter(identityResolver, properties, meterRegistry);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AppProperties properties,
                                            RateLimitFilter rateLimitFilter) throws Exception {
        http
                // API stateless autenticada por bearer token: CSRF baseado em sessão não se aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; "
                                        + "form-action 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds())))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                SecurityProblemWriter.write(request, response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Não autenticado", "authentication_required",
                                        "Credencial ausente ou inválida"))
                        .accessDeniedHandler((request, response, exception) ->
                                SecurityProblemWriter.write(request, response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "Acesso negado", "access_denied",
                                        "A credencial não possui permissão suficiente")));

        if (properties.security().oauth2Enabled()) {
            http.authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                            .requestMatchers("/actuator/prometheus")
                                .hasAuthority("SCOPE_metrics.read")
                            .requestMatchers("POST", "/transacao")
                                .hasAuthority("SCOPE_transactions.write")
                            .requestMatchers("DELETE", "/transacao")
                                .hasAuthority("SCOPE_transactions.delete")
                            .requestMatchers("GET", "/estatistica")
                                .hasAuthority("SCOPE_statistics.read")
                            .anyRequest().denyAll())
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(Customizer.withDefaults())
                            .authenticationEntryPoint((request, response, exception) ->
                                    SecurityProblemWriter.write(request, response,
                                            HttpServletResponse.SC_UNAUTHORIZED,
                                            "Não autenticado", "authentication_required",
                                            "Credencial ausente ou inválida")));
        } else {
            // Modo compatível com o desafio. Produção deve usar o profile oauth2.
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        }

        // Depois da autenticação, o limiter consegue preferir o subject ao IP.
        http.addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
