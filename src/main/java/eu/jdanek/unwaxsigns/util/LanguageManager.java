package eu.jdanek.unwaxsigns.util;

import eu.jdanek.unwaxsigns.UnwaxSigns;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class LanguageManager {

    private static final String FALLBACK_LANG = "en";

    private final UnwaxSigns plugin;
    private final MiniMessage miniMessage;

    private String langCode;
    private final Map<String, FileConfiguration> languageCache = new HashMap<>();

    public LanguageManager(UnwaxSigns plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.langCode = plugin.getConfigManager().getLanguage();

        ensureFallbackResource();
    }

    private void ensureFallbackResource() {
        File fallbackFile = new File(plugin.getDataFolder(), "translations/messages_" + FALLBACK_LANG + ".yml");
        if (!fallbackFile.exists()) {
            plugin.saveResource("translations/messages_" + FALLBACK_LANG + ".yml", false);
        }
    }

    private FileConfiguration getLanguageConfig(String lang) {
        // is it cached?
        if (languageCache.containsKey(lang)) {
            return languageCache.get(lang);
        }

        // search translation
        File langFile = new File(plugin.getDataFolder(), "translations/messages_" + lang + ".yml");

        if (!langFile.exists()) {
            if (plugin.getResource("translations/messages_" + lang + ".yml") != null) {
                plugin.saveResource("translations/messages_" + lang + ".yml", false);
            } else {
                return getLanguageConfig(FALLBACK_LANG);
            }
        }

        // save to cache
        FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
        languageCache.put(lang, config);
        return config;
    }

    public Component getMessage(String path, CommandSender sender) {
        return getMessage(path, sender, null);
    }

    public Component getMessage(String path, CommandSender sender, Map<String, String> placeholders) {
        // retrieve two-letter language code (e.g., "en", "de", "cs")
        String langCode = FALLBACK_LANG;
        if (sender instanceof Player player) {
            langCode = player.locale().getLanguage();
        }

        FileConfiguration config = getLanguageConfig(langCode);
        String rawMessage = config.getString(path);

        // add the missing translation from the fallback
        if (rawMessage == null && !langCode.equals(FALLBACK_LANG)) {
            FileConfiguration fallbackConfig = getLanguageConfig(FALLBACK_LANG);
            rawMessage = fallbackConfig.getString(path, "<red>Missing message: " + path + "</red>");
        } else if (rawMessage == null) {
            rawMessage = "<red>Missing message: " + path + "</red>";
        }

        // replace placeholders
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rawMessage = rawMessage.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        return miniMessage.deserialize(rawMessage);
    }

    public void reloadLanguages() {
        languageCache.clear();
        this.langCode = plugin.getConfigManager().getLanguage();
        ensureFallbackResource();
    }

    public String getDefaultLanguage() {
        return langCode;
    }
}
