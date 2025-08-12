package com.pat.crewhive.model.user.entity.role;

import com.pat.crewhive.model.company.entity.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "role", indexes = {
        @Index(name = "idx_role_company_id", columnList = "company_id")
})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName;

    @OneToMany(mappedBy = "role", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private Set<UserRole> users = new LinkedHashSet<>();

    @ManyToOne(cascade = CascadeType.REMOVE, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    public Role(String role_name) {
        this.roleName = role_name;
    }
}
