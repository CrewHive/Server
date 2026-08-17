package com.pat.crewhive.manager;


import com.pat.crewhive.user.User;
import jakarta.persistence.*;

import java.util.UUID;


@Entity
@Table(name = "user_role", indexes = {
        @Index(name = "idx_userrole_user_id", columnList = "user_id"),
        @Index(name = "idx_userrole_role_id", columnList = "role_id")
})
public class UserRole {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @MapsId
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public UserRole() {
    }

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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
        return userId != null && userId.equals(other.userId);
    }
    @Override public int hashCode() { return 31; }
}
