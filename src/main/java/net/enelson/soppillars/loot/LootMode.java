package net.enelson.soppillars.loot;

import java.util.Locale;

public enum LootMode {
    WHITELIST,
    BLACKLIST,
    MIXED;

    public LootMode next() {
        switch (this) {
            case WHITELIST:
                return BLACKLIST;
            case BLACKLIST:
                return MIXED;
            case MIXED:
            default:
                return WHITELIST;
        }
    }

    public static LootMode parse(String raw, LootMode fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return LootMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
