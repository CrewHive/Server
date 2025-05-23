package com.pat.crewhive.dto.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ContractJSON {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @Nullable
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private int hoursPerWeek;
    private boolean indefinite;

    public ContractJSON() {}

    public ContractJSON(LocalDate startDate, @Nullable LocalDate endDate, int hoursPerWeek, boolean indefinite) {

        this.startDate = startDate;
        this.hoursPerWeek = hoursPerWeek;
        this.indefinite = indefinite;

        if (indefinite) {
            this.endDate = null;
        } else {
            this.endDate = endDate;
        }
    }
}
