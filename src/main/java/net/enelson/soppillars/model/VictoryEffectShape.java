package net.enelson.soppillars.model;

import java.util.Locale;

public enum VictoryEffectShape {

    SQUARE,
    CIRCLE;

    public static VictoryEffectShape parse(String raw, VictoryEffectShape fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            return VictoryEffectShape.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
