package com.BlackDot.Finance.Tracker.Auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security.permit-all-api:false}")
    private boolean permitAllApi;

    @Bean
    @Order(1)
    SecurityFilterChain oauthChain(
            HttpSecurity http,
            GithubAwareUserService githubUserService,
            OAuthSuccessHandler successHandler,
            OAuthFailureHandler failureHandler) throws Exception {
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(a -> a.anyRequest().permitAll())
            .oauth2Login(o -> o
                .userInfoEndpoint(u -> u
                    .userService(githubUserService)
                    .oidcUserService(new OidcUserService()))
                .successHandler(successHandler)
                .failureHandler(failureHandler));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http, JwtAuthConverter jwtAuthConverter) throws Exception {
        http.cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> {
                if (permitAllApi) {
                    a.anyRequest().permitAll();
                } else {
                    a.requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated();
                }
            });
        if (!permitAllApi) {
            http.oauth2ResourceServer(o ->
                    o.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));
        }
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean 
    PasswordEncoder passwordEncoder() { 
        return new BCryptPasswordEncoder(); 
    }
}