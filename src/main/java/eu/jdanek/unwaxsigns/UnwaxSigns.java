package eu.jdanek.unwaxsigns;

import eu.jdanek.unwaxsigns.bstats.MetricsCounters;
import eu.jdanek.unwaxsigns.bstats.PluginMetrics;
import eu.jdanek.unwaxsigns.command.UnwaxSignsCommand;
import eu.jdanek.unwaxsigns.config.ConfigManager;
import eu.jdanek.unwaxsigns.config.EffectsConfigManager;
import eu.jdanek.unwaxsigns.hook.EconomyHook;
import eu.jdanek.unwaxsigns.hook.NoopEconomyHook;
import eu.jdanek.unwaxsigns.hook.VaultEconomyHook;
import eu.jdanek.unwaxsigns.listener.InteractListener;
import eu.jdanek.unwaxsigns.util.LanguageManager;
import eu.jdanek.unwaxsigns.util.NotificationService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class UnwaxSigns extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private EffectsConfigManager effectsConfigManager;
    private LanguageManager languageManager;
    private NotificationService notificationService;
    private InteractListener interactListener;
    private EconomyHook economyHook;
    private MetricsCounters metricsCounters;

    @Override
    public void onEnable() {
        getComponentLogger().info(MiniMessage.miniMessage().deserialize(

                "\n\n<gold>Anti-edit sign wax layer:</gold> <red>DETECTED!</red>\n"
                        + "<yellow>Initializing tools... Removal temperature reached.</yellow>\n"
                        + "<green>Done.</green>\n"
                        + "<aqua>Honeycomb security protocol bypassed!</aqua>\n"
        ));

        this.configManager = new ConfigManager(this);
        this.effectsConfigManager = new EffectsConfigManager(this);
        this.languageManager = new LanguageManager(this);
        this.notificationService = new NotificationService(this);

        // Lazy loading economy - VaultEconomyHook is loaded only when Vault is on the server
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            this.economyHook = new VaultEconomyHook(this);
            if (this.economyHook.isEnabled()) {
                getLogger().info("Vault hooked successfully. Economy features are active.");
            } else {
                getLogger().warning("Vault found, but no economy provider registered! Economy features disabled.");
            }
        } else {
            this.economyHook = new NoopEconomyHook();
        }

        saveDefaultConfig();
        reloadConfiguration();

        this.metricsCounters = new MetricsCounters();

        this.interactListener = new InteractListener(this, metricsCounters);
        getServer().getPluginManager().registerEvents(interactListener, this);
        interactListener.startCleanupTask();

        new PluginMetrics(this, metricsCounters);

        // register command
        var command = getCommand("unwaxsigns");
        if (command == null) {
            throw new IllegalStateException("Command 'unwaxsigns' is not registered in plugin.yml!");
        }
        command.setExecutor(new UnwaxSignsCommand(this));
    }

    /**
     * Plugin shutdown logic
     */
    @Override
    public void onDisable() {
        if (interactListener != null) {
            interactListener.shutdown();
        }
    }

    public void reloadConfiguration() {
        configManager.load();
        effectsConfigManager.load();

        if (this.languageManager != null) {
            this.languageManager.reloadLanguages();
        }
    }

    public boolean checkPermission(CommandSender sender, String permission) {
        // if permission checking is disabled in the config, we return true (for everyone)
        if (!configManager.isPermissionsEnabled()) {
            return true;
        }
        // otherwise, we fall back to the standard permission checks (LuckPerms, PEX, etc.)
        return sender.hasPermission(permission);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EffectsConfigManager getEffectsConfigManager() {
        return effectsConfigManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public MetricsCounters getMetricsCounters() {
        return metricsCounters;
    }
}
