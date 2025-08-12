package com.pat.crewhive.model.user.contract;


import com.pat.crewhive.dto.json.ContractJSON;
import com.pat.crewhive.model.user.entity.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="contract_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long contractId;

    @Type(JsonType.class)
    @Column(name = "contract", nullable = false, columnDefinition = "jsonb")
    private ContractJSON contract;

    @OneToOne(cascade = CascadeType.PERSIST, optional = false, orphanRemoval = true)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
