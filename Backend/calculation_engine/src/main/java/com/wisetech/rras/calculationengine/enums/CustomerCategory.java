package com.wisetech.rras.calculationengine.enums;

public enum CustomerCategory {

    RETAIL("RETAIL"),
    SME("SME"),
    CORP("CORP"),
    SOVEREIGN("SOVEREIGN"),
    BANK("BANK");

    private final String value;

    CustomerCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CustomerCategory fromValue(String value) {
        return CustomerCategory.valueOf(value);
    }
}
