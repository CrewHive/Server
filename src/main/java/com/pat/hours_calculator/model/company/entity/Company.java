package com.pat.hours_calculator.model.company.entity;

import com.pat.hours_calculator.dto.json.AddressJSON;
import com.pat.hours_calculator.model.time_management.entities.template.Template;
import com.pat.hours_calculator.model.user.entity.User;
import com.pat.hours_calculator.model.util.CompanyType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
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

    @Type(JsonType.class)
    @Column(name = "address", nullable = false, columnDefinition = "jsonb")
    private AddressJSON addressJSON;

    @OneToMany(mappedBy = "company", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<User> users = new LinkedHashSet<>();

    @Enumerated
    @Column(name = "company_type", nullable = false)
    private CompanyType companyType;

    @OneToMany(mappedBy = "company", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Template> templates = new ArrayList<>();

}