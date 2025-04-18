package com.pat.hours_calculator.model.company.entity;

import com.pat.hours_calculator.dto.json.AddressJSON;
import com.pat.hours_calculator.model.user.entity.User;
import com.pat.hours_calculator.model.util.CompanyType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @NotNull(message = "Name is required")
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @NotNull(message = "Address is required")
    @Type(JsonType.class)
    @Column(name = "address", nullable = false, columnDefinition = "jsonb")
    private AddressJSON addressJSON;

    @OneToMany(mappedBy = "company", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<User> users = new LinkedHashSet<>();

    @NotNull(message = "Company type is required")
    @Enumerated
    @Column(name = "company_type", nullable = false)
    private CompanyType companyType;

    public Company() {
    }

    public Company(String name, AddressJSON addressJSON, CompanyType companyType) {
        this.name = name;
        this.addressJSON = addressJSON;
        this.companyType = companyType;
    }

}