package com.wisetech.rras.enums;

public enum RunStatus {

    DRAFT("DRAFT"),
    VALIDATED("VALIDATED"),
    CALCULATED("CALCULATED"),
    APPROVED("APPROVED"),
    FAILED("FAILED");

    private final String value;

    RunStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RunStatus fromValue(String value) {
        return RunStatus.valueOf(value);
    }
}