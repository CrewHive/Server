package com.pat.crewhive.model.shift.emptyshift.entity;


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
@Table(name = "empty_shift", indexes = {
        @Index(name = "idx_emptyshift_shift_name", columnList = "shift_name"),
        @Index(name = "idx_emptyshift_company_id", columnList = "company_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_emptyshift_order_number", columnNames = {"order_number", "company_id"})
})
public class ShiftTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="empty_shift_id", nullable = false)
    private Long emptyShiftId;

    @Column(name = "shift_name", nullable = false)
    private String shift_name;

    @Column(name = "start_shift", nullable = false)
    private OffsetTime start;

    @Column(name = "end_shift", nullable = false)
    private OffsetTime end;
    
    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "color", nullable = false)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
