package com.wisetech.rras.calculationengine.enums;

public enum AssetClassification {

    STANDARD("STANDARD"),      // 0–30 days past due
    WATCH("WATCH"),            // 31–60 days
    SUBSTANDARD("SUBSTANDARD"),// 61–90 days
    DOUBTFUL("DOUBTFUL"),      // 91–180 days
    LOSS("LOSS");              // 180+ days

    private final String value;

    AssetClassification(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AssetClassification fromValue(String value) {
        return AssetClassification.valueOf(value);
    }
}