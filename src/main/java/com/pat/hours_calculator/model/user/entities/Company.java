package com.pat.hours_calculator.model.user.entities;

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


    @OneToMany(mappedBy = "company", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<User> users = new LinkedHashSet<>();


    public Company() {
    }

    public Company(String name) {
        this.name = name;
    }

}