package com.pat.hours_calculator.model.time_management.entities.shift.emptyshift;


import com.pat.hours_calculator.model.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

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
public class EmptyShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "shift_name", nullable = false)
    private String shift_name;

    @Column(name = "start_shift", nullable = false)
    private LocalTime start;

    @Column(name = "end_shift", nullable = false)
    private LocalTime end;

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

}
