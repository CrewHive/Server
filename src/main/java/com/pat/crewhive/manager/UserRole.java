package com.pat.crewhive.manager;


import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;


@Entity
@Table(name = "user_role", indexes = {
        @Index(name = "idx_userrole_user_id", columnList = "user_id"),
        @Index(name = "idx_userrole_role_id", columnList = "role_id"),
        @Index(name = "idx_userrole_active", columnList = "active"),
        @Index(name = "idx_userrole_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_userrole_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE user_role SET active = false, deleted_at = now() WHERE user_id = ? AND role_id = ?")
public class UserRole extends SoftDeletableEntity {

    @EmbeddedId
    private UserRoleId id = new UserRoleId();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId("userId")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @MapsId("roleId")
    private Role role;

    public UserRole() {
    }

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
    }

    public UserRoleId getId() {
        return id;
    }

    public void setId(UserRoleId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRole other)) return false;
        if (id == null || id.getUserId() == null || id.getRoleId() == null) return false;
        if (other.id == null || other.id.getUserId() == null || other.id.getRoleId() == null) return false;
        return id.equals(other.id);
    }

    /**
     * public int hashCode() { return 31; }
     * Si potrebbero usare gli id dei singoli oggetti e non dell'attuale(Perché creato solo al flush),
     * trattandosi di potenziali collezioni piccole, non ne vale la pena perché poi:
     * hashCode() dipende da una regola non scritta in nessun posto: "non chiamare mai addRole su uno User non ancora persistito".
     * Nessun tipo, nessuna asserzione, nessun test la fa rispettare — è una convenzione che chiunque tocchi questo codice in futuro deve conoscere e rispettare.
     * Basterebbe che domani qualcuno scriva, in perfetta buona fede, un metodo tipo:
     * User u = new User(...);
     * u.addRole(defaultRole);
     * u.addRole(managerRole);
     * userRepository.save(u);   // pattern "costruisco il grafo, poi salvo tutto insieme" — naturalissimo
     * per far ritornare esattamente il bug che stavi cercando di evitare
     * — e si manifesterebbe come "a volte un ruolo sparisce da un Set dopo il salvataggio", un sintomo intermittente e fastidiosissimo da diagnosticare,
     * non un errore di compilazione o un'eccezione chiara.
     * Questo perché quando crei un utente, l'id dell'utente viene assegnato al persist() (dentro save()), non prima;
     * nell'esempio sopra addRole viene chiamato prima del save, quindi l'oggetto userRole avrebbe l'id del lato user
     * null e se succedesse di lavorarci in memoria nella stessa transazione, ci sarebbero dei problemi con i set perché
     * risulterebbe un oggetto con un hashCode diverso da quello che si aspetta.
    */
    @Override public int hashCode() { return 31; }
}
