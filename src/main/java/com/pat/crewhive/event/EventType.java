package com.pat.crewhive.event;

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
