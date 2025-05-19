package com.pat.hours_calculator.model.time_management.entities.template;


import com.pat.hours_calculator.model.company.entity.Company;
import com.pat.hours_calculator.model.time_management.entities.shifttype.ShiftType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

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
    private Long id;

    @Type(JsonType.class)
    @Column(name="shift_day", columnDefinition = "jsonb")
    private LinkedHashMap<String, ArrayList<ArrayList<ShiftType>>> shiftDay;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Company company;

}
