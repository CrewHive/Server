package com.pat.crewhive.model.shift.shifttemplate.entity;


import com.pat.crewhive.model.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "shift_template", indexes = {
        @Index(name = "idx_shifttemplate_shift_name", columnList = "shift_name"),
        @Index(name = "idx_shifttemplate_company_id", columnList = "company_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_shifttemplate_shift_name_company_id", columnNames = {"shift_name", "company_id"})
})
public class ShiftTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="shift_id", nullable = false)
    private Long shiftId;

    @Column(name = "shift_name", nullable = false)
    private String shiftName;

    @Column(name = "start_shift", nullable = false)
    private OffsetTime startShift;

    @Column(name = "end_shift", nullable = false)
    private OffsetTime endShift;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "color", nullable = false)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
