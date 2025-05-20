package com.pat.hours_calculator.model.time_management.entities.template;


import com.pat.hours_calculator.model.company.entity.Company;
import com.pat.hours_calculator.model.time_management.entities.shifttype.ShiftType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "shift_template")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Type(JsonType.class)
    @Column(name="shift_day", columnDefinition = "jsonb")
    // Example JSON structure: [[05/24/2025, [[id, shiftName, user, start, end], [id, shiftName, user, start, end]]], [05/25/2025, [[id, shiftName, user, start, end], [id, shiftName, user, start, end]]]]
    private LinkedHashMap<LocalDate, ArrayList<ArrayList<ShiftType>>> shiftDay;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
