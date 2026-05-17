package net.enelson.soppillars.cosmetic;

import net.enelson.soppillars.model.VictoryEffectType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VictoryEffectDefinition {

    private final String id;
    private final String displayName;
    private final String permission;
    private final Material icon;
    private final List<String> lore;
    private final VictoryEffectType type;
    private final EntityType entityType;
    private final Material blockMaterial;
    private final Sound sound;
    private final int soundIntervalTicks;
    private final float soundVolume;
    private final float soundPitch;

    public VictoryEffectDefinition(String id,
                                   String displayName,
                                   String permission,
                                   Material icon,
                                   List<String> lore,
                                   VictoryEffectType type,
                                   EntityType entityType,
                                   Material blockMaterial,
                                   Sound sound,
                                   int soundIntervalTicks,
                                   float soundVolume,
                                   float soundPitch) {
        this.id = id;
        this.displayName = displayName;
        this.permission = permission == null ? "" : permission;
        this.icon = icon == null ? Material.FIREWORK_ROCKET : icon;
        this.lore = lore == null ? Collections.<String>emptyList() : new ArrayList<String>(lore);
        this.type = type == null ? VictoryEffectType.FIREWORKS : type;
        this.entityType = entityType;
        this.blockMaterial = blockMaterial;
        this.sound = sound;
        this.soundIntervalTicks = Math.max(0, soundIntervalTicks);
        this.soundVolume = soundVolume <= 0.0F ? 1.0F : soundVolume;
        this.soundPitch = soundPitch <= 0.0F ? 1.0F : soundPitch;
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

    public VictoryEffectType getType() {
        return type;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public Sound getSound() {
        return sound;
    }

    public int getSoundIntervalTicks() {
        return soundIntervalTicks;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }
}
