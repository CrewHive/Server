package com.pat.crewhive.model.util;

public enum EventType {
    PUBLIC("Public"),
    PRIVATE("Private");

    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
