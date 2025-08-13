package com.pat.crewhive.model.user.wrapper;

import com.pat.crewhive.model.user.contract.Contract;
import com.pat.crewhive.model.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * CustomUserDetails implementa UserDetails e adatta la tua entity User
 * al modello di Spring Security.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Il nome utente usato da Spring Security.
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * La password (già codificata in BCrypt) dell'utente.
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Le authority (ruoli) con cui l'utente è autenticato.
     * Qui trasformiamo il campo role (es. "ADMIN") in "ROLE_ADMIN".
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user.getRole().getRole().getRoleName();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /**
     * Indica se l'account non è scaduto.
     * Se volessi, potresti ricavare la scadenza da user.getContract().
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica se l'account non è bloccato.
     * Potresti estendere la tua entity aggiungendo un flag "locked".
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
     *   return user.isWorking();
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Permette di accedere direttamente all'entity sottostante per
     * operazioni di business (ad es. getCompany(), getContract()...).
     */
    public User getUser() {
        return user;
    }


    public Long getUserId() {
        return user.getUserId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getRole() {
        return user.getRole().getRole().getRoleName();
    }

    public String getCompany() {
        return user.getCompany().getName();
    }

    public Contract getContract() {
        return user.getContract();
    }
}

