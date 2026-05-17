package net.enelson.soppillars.cosmetic;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BurstEffectDefinition {

    private final String id;
    private final String displayName;
    private final String permission;
    private final Material icon;
    private final List<String> lore;
    private final BurstEffectType type;
    private final Particle particle;
    private final int particleCount;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double extra;
    private final Sound sound;
    private final float soundVolume;
    private final float soundPitch;

    public BurstEffectDefinition(String id,
                                 String displayName,
                                 String permission,
                                 Material icon,
                                 List<String> lore,
                                 BurstEffectType type,
                                 Particle particle,
                                 int particleCount,
                                 double offsetX,
                                 double offsetY,
                                 double offsetZ,
                                 double extra,
                                 Sound sound,
                                 float soundVolume,
                                 float soundPitch) {
        this.id = id;
        this.displayName = displayName;
        this.permission = permission == null ? "" : permission;
        this.icon = icon == null ? Material.NETHER_STAR : icon;
        this.lore = lore == null ? Collections.<String>emptyList() : new ArrayList<String>(lore);
        this.type = type == null ? BurstEffectType.PARTICLE_BURST : type;
        this.particle = particle;
        this.particleCount = Math.max(0, particleCount);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.extra = extra;
        this.sound = sound;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPermission() {
        return permission;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public BurstEffectType getType() {
        return type;
    }

    public Particle getParticle() {
        return particle;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public double getExtra() {
        return extra;
    }

    public Sound getSound() {
        return sound;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }
}
