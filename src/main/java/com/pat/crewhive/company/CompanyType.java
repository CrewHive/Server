package com.pat.crewhive.company;

public enum CompanyType {
    HOSPITAL("Hospital"),
    RESTAURANT("Restaurant"),
    BAR("Bar"),
    OTHER("Other");

    private final String label;

    CompanyType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
