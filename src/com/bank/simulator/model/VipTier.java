package com.bank.simulator.model;

/**
 * Represents the tier of VIP customers, determining their priority level.
 */
public enum VipTier {
    VVIP(1, "VVIP (High Priority)"),
    VIP(2, "VIP (Medium Priority)"),
    PREFERRED(3, "Preferred (Low Priority)");

    private final int level;
    private final String displayName;

    VipTier(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }
}
