package net.enelson.soppillars.model;

import java.util.Locale;

public enum VictoryEffectType {

    NONE,
    FIREWORKS,
    ENTITY_RAIN,
    BLOCK_RAIN;

    public static VictoryEffectType parse(String raw, VictoryEffectType fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return VictoryEffectType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
