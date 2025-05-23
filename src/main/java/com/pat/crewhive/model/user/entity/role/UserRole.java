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
@Table(name = "user_role")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @OneToOne(optional = false, orphanRemoval = true)
    @MapsId("userId")
    private User user;

    @ManyToOne(cascade = CascadeType.PERSIST, optional = false)
    @MapsId("roleId")
    private Role role;

    public UserRole(User user, Role role) {
        this.user = user;
        this.role = role;
        this.id = new UserRoleId(user.getUserId(), role.getId());
    }



}
