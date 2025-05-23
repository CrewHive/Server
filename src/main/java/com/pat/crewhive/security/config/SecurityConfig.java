package com.pat.crewhive.security.config;

import com.pat.crewhive.security.exception.handler.RestAuthenticationEntryPoint;
import com.pat.crewhive.security.filter.JwtAuthenticationFilter;
import com.pat.crewhive.service.JwtService;
import com.pat.crewhive.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
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
    public PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder();}

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. disabilita sessione e form-login
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .securityContext(AbstractHttpConfigurer::disable)

                // 2. gestisci le eccezioni
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                )

                // 2. rendi accessibile senza jwt solo login e refresh
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/refresh")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                );

        // 3. inserisci il filtro JWT prima di tutti
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtService, userService),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}

