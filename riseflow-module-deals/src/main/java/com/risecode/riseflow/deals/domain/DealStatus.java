package com.risecode.riseflow.deals.domain;

public enum DealStatus {
    OPEN, WON, LOST;

    public static DealStatus fromProbability(int probability) {
        if (probability == 100) return WON;
        if (probability == 0) return LOST;
        return OPEN;
    }
}
