package com.pat.crewhive.model.user.entity.role;

import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="role_id", nullable = false)
    private Long id;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    @OneToMany(mappedBy = "role", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private Set<UserRole> users = new LinkedHashSet<>();

    public Role(String role_name) {
        this.roleName = role_name;
    }
}
