package com.pat.crewhive.model.user.entity.role;


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

    @EmbeddedId
    private UserRoleId id;

    @OneToOne(optional = false, orphanRemoval = true)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId("userId")
    private User user;

    @ManyToOne(cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    @MapsId("roleId")
    private Role role;

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
        this.id = new UserRoleId(user.getUserId(), role.getRoleId());
    }



}
