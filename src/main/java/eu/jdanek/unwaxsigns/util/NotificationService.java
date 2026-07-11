package eu.jdanek.unwaxsigns.util;

import eu.jdanek.unwaxsigns.UnwaxSigns;
import eu.jdanek.unwaxsigns.config.ConfigManager;

import java.util.Map;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NotificationService {

    private final UnwaxSigns plugin;

    public NotificationService(UnwaxSigns plugin) {
        this.plugin = plugin;
    }

    public void send(CommandSender sender, String messagePath) {
        send(sender, messagePath, null, false);
    }

    public void send(CommandSender sender, String messagePath, Map<String, String> placeholders) {
        send(sender, messagePath, placeholders, false);
    }

    public void send(CommandSender sender, String messagePath, boolean isAlert) {
        send(sender, messagePath, null, isAlert);
    }

    public void send(CommandSender sender, String messagePath, Map<String, String> placeholders, boolean isAlert) {
        Component message = plugin.getLanguageManager().getMessage(messagePath, sender, placeholders);
        send(sender, message, isAlert);
    }

    public void send(CommandSender sender, Component message) {
        send(sender, message, false);
    }

    public void send(CommandSender sender, Component message, boolean isAlert) {
        if (sender instanceof Player player && isAlert) {
            ConfigManager config = plugin.getConfigManager();
            switch (config.getNotificationType()) {
                case ACTIONBAR -> {
                    player.sendActionBar(message);
                    return;
                }
                case NONE -> {
                    return;
                }
                case CHAT -> {
                    // fall-through to chat
                }
            }
        }

        Component prefix = plugin.getLanguageManager().getMessage("prefix", sender);
        sender.sendMessage(prefix.append(message));
    }

    /**
     * Failure (wrong tool, sneak, enchant…) – sound + message, no logging.
     */
    public void sendFailure(Player player, Location location, String messagePath) {
        sendFailure(player, location, messagePath, null);
    }

    public void sendFailure(Player player, Location location, String messagePath, Map<String, String> placeholders) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.shouldNotifyOnDenial()) return;

        send(player, messagePath, placeholders, true);
        plugin.getEffectsConfigManager().playFailure(location, player, true);
    }

    /**
     * Rejection (permission, region) – sound + message + console log.
     */
    public void sendDenial(Player player, Block block, String messagePath) {
        sendDenial(player, block, messagePath, null);
    }

    public void sendDenial(Player player, Block block, String messagePath, Map<String, String> placeholders) {
        ConfigManager config = plugin.getConfigManager();
        if (config.shouldNotifyOnDenial()) {
            send(player, messagePath, placeholders, true);
            plugin.getEffectsConfigManager().playFailure(block.getLocation(), player, true);
        }
        plugin.getConfigManager().logUnwax(player, block.getLocation(), true);
    }
}
