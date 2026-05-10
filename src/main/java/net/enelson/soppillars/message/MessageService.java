package net.enelson.soppillars.message;

import net.md_5.bungee.api.ChatColor;
import net.enelson.soppillars.SopPillarsPlugin;
import org.bukkit.command.CommandSender;

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
        sender.sendMessage(resolve(withPrefix(get(path)), replacements));
    }

    public String get(String path) {
        return plugin.getConfig().getString("messages." + path, "&cMissing message: " + path);
    }

    public String resolve(String input, Map<String, String> replacements) {
        String result = input == null ? "" : input;
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                result = result.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String withPrefix(String message) {
        return plugin.getConfig().getString("messages.prefix", "") + message;
    }
}
