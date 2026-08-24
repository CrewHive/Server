package com.pat.crewhive.security.key;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Set;

@Configuration
public class JwtKeyConfig {

    private static final Set<String> RECOGNIZED_PROFILES = Set.of("cloud", "onpremise");

    private final PemUtils pemUtils;
    private final Environment environment;

    public JwtKeyConfig(PemUtils pemUtils, Environment environment) {
        this.pemUtils = pemUtils;
        this.environment = environment;
    }

    /**
     * Fails fast, with an explicit message, when the app is started without picking a source
     * for the JWT keys: SPRING_PROFILES_ACTIVE must be "cloud" (AWS Parameter Store) or
     * "onpremise" (environment variables), otherwise key loading below would fail with a much
     * less helpful error.
     */
    @PostConstruct
    void validateActiveProfile() {
        boolean hasRecognizedProfile = List.of(environment.getActiveProfiles())
                .stream()
                .anyMatch(RECOGNIZED_PROFILES::contains);

        if (!hasRecognizedProfile) {
            throw new IllegalStateException(
                    "Nessun profilo Spring valido attivo. Imposta SPRING_PROFILES_ACTIVE=cloud " +
                            "(chiavi JWT da AWS Parameter Store) oppure SPRING_PROFILES_ACTIVE=onpremise " +
                            "(chiavi JWT da variabili d'ambiente) in .env/docker-compose.");
        }
    }

    @Bean
    public PrivateKey jwtPrivateKey() throws Exception {
        return pemUtils.loadPrivateKey();
    }

    @Bean
    public PublicKey jwtPublicKey() throws Exception {
        return pemUtils.loadPublicKey();
    }
}
