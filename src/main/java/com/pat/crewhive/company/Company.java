package com.pat.crewhive.company;

import com.pat.crewhive.user.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_id", nullable = false)
    @Setter(AccessLevel.NONE)
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

    public Company(CompanyRegistrationDTO registrationDTO) {
        this.name = registrationDTO.getCompanyName();
        this.addressJSON = registrationDTO.getAddress();
        this.companyType = registrationDTO.getCompanyType();
    }

}