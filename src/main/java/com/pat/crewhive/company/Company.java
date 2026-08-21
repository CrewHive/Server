package com.pat.crewhive.company;

import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "company", indexes = {
        @Index(name = "idx_company_active", columnList = "active"),
        @Index(name = "idx_company_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_company_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE company SET active = false, deleted_at = now() WHERE company_id = ?")
public class Company extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Type(JsonType.class)
    @Column(name = "address", columnDefinition = "jsonb")
    private AddressJSON addressJSON;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private Set<User> users = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false)
    private CompanyType companyType;

    public Company() {
    }

    public Company(UUID companyId, String name, AddressJSON addressJSON, Set<User> users, CompanyType companyType) {
        this.companyId = companyId;
        this.name = name;
        this.addressJSON = addressJSON;
        this.users = users;
        this.companyType = companyType;
    }

    public Company(CompanyRegistrationDTO registrationDTO) {
        this.name = registrationDTO.companyName();
        this.addressJSON = registrationDTO.address();
        this.companyType = registrationDTO.companyType();
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AddressJSON getAddressJSON() {
        return addressJSON;
    }

    public void setAddressJSON(AddressJSON addressJSON) {
        this.addressJSON = addressJSON;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public CompanyType getCompanyType() {
        return companyType;
    }

    public void setCompanyType(CompanyType companyType) {
        this.companyType = companyType;
    }

}