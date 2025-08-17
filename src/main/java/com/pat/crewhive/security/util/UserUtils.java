package com.pat.crewhive.security.util;

import com.pat.crewhive.dto.UserDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility per accedere ai dati dell'utente corrente dal SecurityContext.
 */
public final class UserUtils {


    private UserUtils() {
    }


    /**
     * Restituisce l'Authentication correntemente in SecurityContext, o null se non autenticato.
     */
    public static Authentication getAuthentication() {

        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Ritorna true se c'è un utente autenticato (e non anonimo).
     */
    public static boolean isAuthenticated() {

        Authentication auth = getAuthentication();

        return auth != null
                && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"));
    }

    /**
     * Estrae il CustomUserDetails (il wrapper sulla tua entity User) dal contesto.
     * @return il CustomUserDetails o null se non autenticato o se il principal non è di questo tipo
     */
    public static CustomUserDetails getCustomUserDetails() {

        Authentication auth = getAuthentication();

        if (auth == null || !isAuthenticated()) return null;

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails) return (CustomUserDetails) principal;

        return null;
    }

    /**
     * Estrae il UserDTO (o modello equivalente) dell'utente corrente.
     * @return UserDTO o null se non autenticato
     */
    public static UserDTO getCurrentUser() {

        CustomUserDetails cud = getCustomUserDetails();

        return (cud != null ? new UserDTO(
                cud.getEmail(),
                cud.getUsername(),
                cud.getRole(),
                cud.getCompanyId()
        ) : null);
    }

    /**
     * Id dell'utente corrente, o null se non autenticato.
     */
    public static Long getCurrentUserId() {

        CustomUserDetails cud = getCustomUserDetails();

        return (cud != null ? cud.getUserId() : null);
    }

    /**
     * Username dell'utente corrente, o null se non autenticato.
     */
    public static String getCurrentUsername() {

        CustomUserDetails cud = getCustomUserDetails();

        return (cud != null ? cud.getUsername() : null);
    }

    /**
     * Role (singolo) dell'utente corrente, o null se non autenticato.
     */
    public static String getCurrentUserRole() {

        CustomUserDetails cud = getCustomUserDetails();

        return (cud != null ? cud.getRole() : null);
    }

    /**
     * Controlla se l'utente corrente ha il role specificato.
     */
    public static boolean hasRole(String role) {

        CustomUserDetails cud = getCustomUserDetails();

        if (cud == null) return false;

        return cud.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}

