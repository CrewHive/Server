package com.pat.hours_calculator.model.company.entity;

import com.pat.hours_calculator.model.user.entity.User;
import com.pat.hours_calculator.model.util.CompanyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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


    @Column(name = "name", nullable = false, unique = true)
    private String name;


    @Column(name = "address", nullable = false)
    private String address;


    @OneToMany(mappedBy = "company", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<User> users = new LinkedHashSet<>();

    @Enumerated
    @Column(name = "company_type", nullable = false)
    private CompanyType companyType;

    public Company() {
    }

    public Company(String name, String address, CompanyType companyType) {
        this.name = name;
        this.address = address;
        this.companyType = companyType;
    }

}