package com.pat.crewhive.manager;

import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.company.Company;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "role", indexes = {
        @Index(name = "idx_role_company_id", columnList = "company_id"),
        @Index(name = "idx_role_active", columnList = "active"),
        @Index(name = "idx_role_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_role_deleted_by", columnList = "deleted_by")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_role_role_name_company_id", columnNames = {"role_name", "company_id"})
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE role SET active = false, deleted_at = now() WHERE role_id = ?")
public class Role extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private Set<UserRole> users = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    public Role() {
    }

    /**
     * Constructor for creating a new global Role.
     *
     * @param roleName The name of the new role.
     */
    public Role (String roleName) {
        this.roleName = roleName;
        this.company = null;
    }

    /**
     * Constructor for creating a new Role for a certain Company.
     *
     * @param role_name The name of the role.
     * @param company   The company to which the role belongs.
     */
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
