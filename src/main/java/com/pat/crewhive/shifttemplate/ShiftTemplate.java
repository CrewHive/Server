package com.pat.crewhive.shifttemplate;


import com.pat.crewhive.company.Company;
import jakarta.persistence.*;

import java.time.OffsetTime;
import java.util.UUID;

@Entity
@Table(name = "shift_template", indexes = {
        @Index(name = "idx_shifttemplate_shift_name", columnList = "shift_name"),
        @Index(name = "idx_shifttemplate_company_id", columnList = "company_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_shifttemplate_shift_name_company_id", columnNames = {"shift_name", "company_id"})
})
public class ShiftTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="shift_id", nullable = false)
    private UUID shiftId;

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

    public ShiftTemplate() {
    }

    public ShiftTemplate(UUID shiftId, String shiftName, OffsetTime startShift, OffsetTime endShift,
                          String description, String color, Company company) {
        this.shiftId = shiftId;
        this.shiftName = shiftName;
        this.startShift = startShift;
        this.endShift = endShift;
        this.description = description;
        this.color = color;
        this.company = company;
    }

    public UUID getShiftId() {
        return shiftId;
    }

    public void setShiftId(UUID shiftId) {
        this.shiftId = shiftId;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public OffsetTime getStartShift() {
        return startShift;
    }

    public void setStartShift(OffsetTime startShift) {
        this.startShift = startShift;
    }

    public OffsetTime getEndShift() {
        return endShift;
    }

    public void setEndShift(OffsetTime endShift) {
        this.endShift = endShift;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

}
