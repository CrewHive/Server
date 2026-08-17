package com.pat.crewhive.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;

import java.time.LocalDate;

public record ContractJSON(

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,

        @Nullable
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate endDate,

        int hoursPerWeek,

        boolean indefinite
) {

    public ContractJSON {
        if (indefinite) {
            endDate = null;
        }
    }
}
