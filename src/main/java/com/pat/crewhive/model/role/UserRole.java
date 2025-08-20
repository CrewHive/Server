package com.pat.crewhive.model.role;


import com.pat.crewhive.model.role.entity.Role;
import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_role", indexes = {
        @Index(name = "idx_userrole_user_id", columnList = "user_id"),
        @Index(name = "idx_userrole_role_id", columnList = "role_id")
})
public class UserRole {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @MapsId
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRole)) return false;
        UserRole other = (UserRole) o;
        return userId != null && userId.equals(other.userId);
    }
    @Override public int hashCode() { return 31; }
}
