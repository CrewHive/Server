package com.pat.crewhive.model.company.entity;

import com.pat.crewhive.dto.json.AddressJSON;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.util.CompanyType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.LinkedHashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Type(JsonType.class)
    @Column(name = "address", nullable = false, columnDefinition = "jsonb")
    private AddressJSON addressJSON;

    @OneToMany(mappedBy = "company", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<User> users = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false)
    private CompanyType companyType;

}