package com.pat.crewhive.model.util;

public enum ContractType {

    FULL_TIME("Full time"),
    PART_TIME_HORIZONTAL("Part time horizontal"),
    PART_TIME_VERTICAL("Part time vertical");

    private final String label;

    ContractType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
