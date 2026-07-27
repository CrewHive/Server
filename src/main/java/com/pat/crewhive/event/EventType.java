package com.pat.crewhive.event;

public enum EventType {

    PUBLIC((short) 1, "Public"),
    PRIVATE((short) 2, "Private");

    private final short id;
    private final String label;

    EventType(short id, String label) {
        this.id = id;
        this.label = label;
    }

    public short getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
