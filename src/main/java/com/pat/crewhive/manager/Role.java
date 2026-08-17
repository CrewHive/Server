package com.pat.crewhive.manager;

import com.pat.crewhive.company.Company;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "role", indexes = {
        @Index(name = "idx_role_company_id", columnList = "company_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_role_role_name_company_id", columnNames = {"role_name", "company_id"})
})
public class Role {
    //todo modifica annotazioni json
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> users = new LinkedHashSet<>();

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    /**
     * Constructor for creating a new Role.
     *
     * @param role_name The name of the role.
     * @param company   The company to which the role belongs.
     */
    public Role() {
    }

    public Role(String role_name,
                Company company) {
        this.roleName = role_name;
        this.company = company;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Set<UserRole> getUsers() {
        return users;
    }

    public void setUsers(Set<UserRole> users) {
        this.users = users;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
