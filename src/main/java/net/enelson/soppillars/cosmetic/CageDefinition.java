package net.enelson.soppillars.cosmetic;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CageDefinition {

    private final String id;
    private final String displayName;
    private final String permission;
    private final Material icon;
    private final List<String> lore;

    public CageDefinition(String id, String displayName, String permission, Material icon, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.permission = permission == null ? "" : permission;
        this.icon = icon == null ? Material.GRAY_STAINED_GLASS : icon;
        this.lore = lore == null ? Collections.<String>emptyList() : new ArrayList<String>(lore);
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
}
