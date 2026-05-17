package net.enelson.soppillars.cosmetic;

import java.util.Locale;

public enum BurstEffectType {
    PARTICLE_BURST,
    TOTEM,
    LIGHTNING_FAKE;

    public static BurstEffectType parse(String raw, BurstEffectType fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
