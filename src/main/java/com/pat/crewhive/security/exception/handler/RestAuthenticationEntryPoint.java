package com.pat.crewhive.security.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Scegli lo status: 401 se non autenticato, 403 se vuoi differenziare
        int status = HttpServletResponse.SC_UNAUTHORIZED;
        response.setStatus(status);
        response.setContentType("application/json");
        // Mappa l’eccezione in un JSON coerente con il tuo GlobalExceptionHandler
        String body = mapper.writeValueAsString(Map.of(
                "status", status,
                "error", authException.getMessage()
        ));
        response.getWriter().write(body);
    }
}

