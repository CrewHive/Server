package com.pat.crewhive.manager;

import com.pat.crewhive.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operazioni sui ruoli che servono a {@link com.pat.crewhive.user.UserService} e
 * {@link com.pat.crewhive.company.CompanyAccessService}, tenute qui invece che in
 * {@link RoleService} apposta: {@code RoleService} dipende già da {@code UserService}
 * e da {@code CompanyService}, quindi se quei due dipendessero a loro volta da
 * {@code RoleService} si creerebbe un ciclo di bean Spring. Questo servizio dipende
 * solo da {@link RoleRepository}, quindi può essere iniettato ovunque senza rischio.
 */
@Service
public class RoleAssignmentService {

    private final RoleRepository roleRepository;

    public RoleAssignmentService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Retrieves or creates the global base role ("ROLE_USER") assigned to every user
     * without a more specific role.
     */
    @Transactional
    public Role getOrCreateGlobalRoleUser() {

        String name = "ROLE_USER";
        return roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull(name)
                .orElseGet(() -> roleRepository.save(new Role(name, null)));
    }

    /**
     * Strips every role from the user except the global base role, adding it if missing.
     * Da richiamare ogni volta che il legame tra utente e company si rompe (l'utente
     * lascia la company, ne viene rimosso, oppure la company stessa viene eliminata) o
     * quando l'account viene disattivato: senza una company, all'utente deve restare
     * solo "ROLE_USER" (che sia un ruolo company-specific o quello globale ROLE_MANAGER).
     */
    @Transactional
    public void resetToBaseRole(User user) {

        Role base = getOrCreateGlobalRoleUser();

        List<Role> toRemove = user.getRoles().stream()
                .map(UserRole::getRole)
                .filter(r -> !r.equals(base))
                .toList(); // snapshot: removeRole muta la stessa collezione su cui stiamo iterando

        toRemove.forEach(user::removeRole);

        boolean hasBase = user.getRoles().stream().anyMatch(ur -> ur.getRole().equals(base));
        if (!hasBase) user.addRole(base);
    }
}
