package com.wisetech.rras.enums;

public enum CalculationType {

    BI_WEEKLY("BI_WEEKLY"),
    MONTHLY("MONTHLY"),
    ANNUAL("ANNUAL");

    private final String value;

    CalculationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CalculationType fromValue(String value) {
        return CalculationType.valueOf(value);
    }
}