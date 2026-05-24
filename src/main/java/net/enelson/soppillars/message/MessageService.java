package net.enelson.soppillars.message;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.enelson.soppillars.SopPillarsPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public final class MessageService {

    private final SopPillarsPlugin plugin;

    public MessageService(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, null);
    }

    public void send(CommandSender sender, String path, Map<String, String> replacements) {
        sender.sendMessage(resolve(sender, withPrefix(get(path)), replacements));
    }

    public String get(String path) {
        return plugin.getConfig().getString("messages." + path, "&cMissing message: " + path);
    }

    public String resolve(String input, Map<String, String> replacements) {
        return resolve(null, input, replacements);
    }

    public String resolve(CommandSender sender, String input, Map<String, String> replacements) {
        String result = input == null ? "" : input;
        result = applyPluginReplacements(result, replacements);
        result = applyPlaceholderApi(sender, result);
        result = applyPluginReplacements(result, replacements);
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String applyPluginReplacements(String input, Map<String, String> replacements) {
        String result = input == null ? "" : input;
        if (replacements == null) {
            return result;
        }
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    private String applyPlaceholderApi(CommandSender sender, String input) {
        if (!(sender instanceof Player)) {
            return input;
        }
        Plugin papi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null || !papi.isEnabled()) {
            return input;
        }
        return PlaceholderAPI.setPlaceholders((Player) sender, input);
    }

    private String withPrefix(String message) {
        return plugin.getConfig().getString("messages.prefix", "") + message;
    }
}
