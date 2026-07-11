package eu.jdanek.unwaxsigns.command;

import eu.jdanek.unwaxsigns.UnwaxSigns;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnwaxSignsCommand implements CommandExecutor, TabCompleter {

    private final UnwaxSigns plugin;

    public UnwaxSignsCommand(UnwaxSigns plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("unwaxsigns")) return false;

        if (args.length == 0) {
            plugin.getNotificationService().send(sender, "messages.usage");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("unwaxsigns.admin")) {
                plugin.getNotificationService().send(sender, "messages.no-permission");
                return true;
            }

            try {
                plugin.reloadConfiguration();
                plugin.getNotificationService().send(sender, "messages.reload-success");
            } catch (Exception e) {
                plugin.getNotificationService().send(sender, "messages.reload-fail");
                plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            }
            return true;
        }

        plugin.getNotificationService().send(sender, "messages.usage");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("unwaxsigns")) {
            if (args.length == 1 && sender.hasPermission("unwaxsigns.admin")) {
                List<String> completions = new ArrayList<>();
                if ("reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
                return completions;
            }
        }
        return new ArrayList<>();
    }
}
