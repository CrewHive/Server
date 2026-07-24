package com.pat.crewhive.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * CustomUserDetails implementa UserDetails e adatta i dati utente
 * al modello di Spring Security.

 * In questa versione non dipende più da entità JPA lazy,
 * ma conserva solo i valori base presi dal JWT.
 */
public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String role;
    private final UUID companyId;
    private final boolean working;
    private final String jti;
    private final Date tokenExpiration;

    private final List<GrantedAuthority> authorities;

    /**
     * Costruisce un CustomUserDetails a partire dai claim del token.
     */
    public CustomUserDetails(UUID userId,
                             String email,
                             String firstName,
                             String lastName,
                             String role,
                             UUID companyId,
                             boolean working,
                             String jti,
                             Date tokenExpiration) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.companyId = companyId;
        this.working = working;
        this.jti = jti;
        this.tokenExpiration = tokenExpiration;

        this.authorities = List.of(new SimpleGrantedAuthority(role));
    }

    /**
     * Factory method comodo per costruire da claim.
     */
    public static CustomUserDetails fromClaims(UUID userId,
                                               String email,
                                               String firstName,
                                               String lastName,
                                               String role,
                                               UUID companyId,
                                               String jti,
                                               Date tokenExpiration) {
        return new CustomUserDetails(userId, email, firstName, lastName, role, companyId, true, jti, tokenExpiration);
    }

    /**
     * Il nome utente usato da Spring Security.
     */
    @Override
    @NullMarked
    public String getUsername() {
        return (firstName + " " + lastName);
    }

    /**
     * La password (non necessaria in contesto JWT).
     */
    @Override
    public String getPassword() {
        return null;
    }

    /**
     * Le authority (ruoli) con cui l'utente è autenticato.
     * Qui trasformiamo il campo role (es. "USER") in "ROLE_USER".
     */
    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Indica se l'account non è scaduto.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica se l'account non è bloccato.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indica se le credenziali (password) non sono scadute.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica se l'utente è abilitato.
     * Se vuoi disabilitare l'accesso a chi non è in servizio, puoi fare:
     *   return working;
     */
    @Override
    public boolean isEnabled() {
        return working;
    }


    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getRole() {
        return role;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getJti() {
        return jti;
    }

    public Date getTokenExpiration() {
        return tokenExpiration;
    }
}
