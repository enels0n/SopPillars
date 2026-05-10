package net.enelson.soppillars.kit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class KitDefinition {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final String permission;
    private final List<ItemStack> items;

    public KitDefinition(String id, String displayName, Material icon, String permission, List<ItemStack> items) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.permission = permission;
        this.items = new ArrayList<ItemStack>(items);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getPermission() {
        return permission;
    }

    public List<ItemStack> getItems() {
        List<ItemStack> copy = new ArrayList<ItemStack>();
        for (ItemStack item : items) {
            copy.add(item == null ? null : item.clone());
        }
        return Collections.unmodifiableList(copy);
    }
}
