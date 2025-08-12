package com.pat.crewhive.security.config;

import com.pat.crewhive.security.exception.handler.RestAuthenticationEntryPoint;
import com.pat.crewhive.security.filter.JwtAuthenticationFilter;
import com.pat.crewhive.service.JwtService;
import com.pat.crewhive.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserService userService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    public SecurityConfig(JwtService jwtService,
                          UserService userService,
                          RestAuthenticationEntryPoint restAuthenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // .securityContext(AbstractHttpConfigurer::disable)  // <— RIMUOVI QUESTA RIGA

                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/rotate", "/api/auth/register") // <— CONTROLLA CHE SIANO QUESTE LE TUE ROTTE
                        .permitAll()
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtService, userService),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}

