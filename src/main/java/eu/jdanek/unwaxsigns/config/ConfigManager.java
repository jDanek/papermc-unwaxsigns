package eu.jdanek.unwaxsigns.config;

import eu.jdanek.unwaxsigns.util.NamespacedKeyUtil;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Logger logger;

    // settings
    private int configVersion;
    private String language;
    private boolean permissionsEnabled;
    private boolean notifyOnDenial;
    private NotificationType notificationType;
    private boolean requireSneaking;
    private boolean regionProtectionCheckEnabled;
    private long unwaxCooldownMs;
    private long unwaxBlockCooldownMs;
    private boolean dropEnabled;
    private Material dropMaterial;
    private int dropMinAmount;
    private int dropMaxAmount;
    private double dropChance;
    private boolean logUnwaxActions;
    private boolean logDeniedAttempts;
    private String worldRestrictionMode;
    private Set<String> restrictedWorlds;

    // economy
    private double costPerUnwax;

    // tool settings
    private final Set<Material> allowedToolsResolved = new HashSet<>();
    private final Set<Material> allowedTools = new HashSet<>();
    private final Set<Material> excludedTools = new HashSet<>();
    private boolean consumeDurabilityEnabled;
    private int durabilityCost;
    private ToolMode modeType;
    private boolean axesTagFallbackEnabled;

    // tool restrictions
    private boolean customNameRequired;
    private String requiredCustomName;
    private boolean enchantmentRequired;
    private Enchantment requiredEnchantment;
    private int requiredEnchantmentLevel;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        loadSettings(config);
        loadToolSettings(config);
        loadToolRestrictions(config);
        resolveAllowedTools();
    }

    private void resolveAllowedTools() {
        // clear
        allowedToolsResolved.clear();
        // resolve
        allowedToolsResolved.addAll(allowedTools);
        allowedToolsResolved.removeAll(excludedTools);
    }

    private void loadSettings(FileConfiguration config) {
        this.configVersion = config.getInt("config-version", 1);
        this.language = config.getString("language", "en");
        this.permissionsEnabled = config.getBoolean("settings.use-permissions", true);
        this.notifyOnDenial = config.getBoolean("settings.notify-on-denial", true);
        this.notificationType = NotificationType.fromString(config.getString("settings.notification-type", "actionbar"));
        this.requireSneaking = config.getBoolean("settings.require-sneaking", false);
        this.regionProtectionCheckEnabled = config.getBoolean("settings.region-protection-check", true);
        this.unwaxCooldownMs = config.getLong("settings.unwax-cooldown-ms", 500);
        this.unwaxBlockCooldownMs = config.getLong("settings.unwax-block-cooldown-ms", 0);
        this.dropEnabled = config.getBoolean("settings.drop-on-unwax", false);
        this.dropMaterial = loadDropMaterial(config);

        // validate drop + log
        int rawMin = config.getInt("settings.drop.min-amount", 1);
        int rawMax = config.getInt("settings.drop.max-amount", 1);
        double rawChance = config.getDouble("settings.drop.chance", 1.0);

        this.dropMinAmount = Math.max(1, rawMin);
        this.dropMaxAmount = Math.max(this.dropMinAmount, rawMax);
        this.dropChance = Math.clamp(rawChance, 0.0, 1.0);

        if (rawMin != this.dropMinAmount || rawMax != this.dropMaxAmount) {
            logger.warning("settings.drop min/max-amount was invalid (min=" + rawMin + ", max=" + rawMax + "). "
                    + "Corrected to min=" + this.dropMinAmount + ", max=" + this.dropMaxAmount + ".");
        }
        if (rawChance != this.dropChance) {
            logger.warning("settings.drop.chance must be between 0.0 and 1.0 (was " + rawChance + "). "
                    + "Clamped to " + this.dropChance + ".");
        }

        this.logUnwaxActions = config.getBoolean("settings.log-unwax-actions", true);
        this.logDeniedAttempts = config.getBoolean("settings.log-denied-attempts", false);

        this.worldRestrictionMode = config.getString("settings.world-restriction-mode", "disabled").toLowerCase();
        this.restrictedWorlds = new HashSet<>();
        for (String world : config.getStringList("settings.restricted-worlds")) {
            this.restrictedWorlds.add(world.toLowerCase());
        }

        this.costPerUnwax = config.getDouble("settings.cost-per-unwax", 0.0);
    }

    private Material loadDropMaterial(FileConfiguration config) {
        String materialName = config.getString("settings.drop.material", "honeycomb").toLowerCase();

        NamespacedKey key = NamespacedKeyUtil.fromString(materialName);
        if (key == null) {
            logger.warning("Invalid drop material format '" + materialName + "'. Using honeycomb.");
            return Material.HONEYCOMB;
        }
        Material mat = Registry.MATERIAL.get(key);
        if (mat == null) {
            logger.warning("Unknown drop material '" + materialName + "'. Using honeycomb.");
            return Material.HONEYCOMB;
        }
        return mat;
    }

    private void loadToolSettings(FileConfiguration config) {
        this.allowedTools.clear();
        this.consumeDurabilityEnabled = config.getBoolean("tool-settings.consume-durability", true);
        this.durabilityCost = config.getInt("tool-settings.durability-cost", 1);
        this.modeType = ToolMode.fromString(config.getString("tool-settings.mode-type", "tag"));
        this.axesTagFallbackEnabled = config.getBoolean("tool-settings.use-axes-tag-fallback", true);

        switch (getModeType()) {
            case LIST -> loadListMode(config);
            // the TAG option is the default; there is no need to duplicate the logic
            default -> loadTagMode(config);
        }

        loadExcludedItems(config);
    }

    private void loadExcludedItems(FileConfiguration config) {
        this.excludedTools.clear();
        for (String name : config.getStringList("tool-settings.excluded-items")) {
            String nameStr = name.toLowerCase().trim();

            NamespacedKey key = NamespacedKeyUtil.fromString(nameStr);
            if (key == null) continue;

            Material mat = Registry.MATERIAL.get(key);
            if (mat != null) {
                this.excludedTools.add(mat);
            }
        }
    }

    private void loadTagMode(FileConfiguration config) {
        List<String> tagNames = config.getStringList("tool-settings.tag-names");

        if (tagNames.isEmpty()) {
            tagNames.add("axes");
        }

        for (String tagName : tagNames) {
            NamespacedKey tagKey = NamespacedKeyUtil.fromString(tagName);

            if (tagKey == null) {
                continue;
            }

            Tag<Material> itemTag = plugin.getServer().getTag(Tag.REGISTRY_ITEMS, tagKey, Material.class);

            if (itemTag == null) {
                logger.warning("The Minecraft tag '#" + tagName + "' does not exist! I'm skipping it.");
            } else {
                this.allowedTools.addAll(itemTag.getValues());
            }
        }

        if (this.allowedTools.isEmpty()) {
            if (this.axesTagFallbackEnabled) {
                logger.severe("No valid tags were found! As a fallback, I'm allowing axes.");
                Tag<Material> fallback = plugin.getServer().getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft("axes"), Material.class);
                if (fallback != null) {
                    this.allowedTools.addAll(fallback.getValues());
                }
            } else {
                logger.severe("--- CRITICAL CONFIGURATION ERROR ---");
                logger.severe("No valid tool tags were found and the axe fallback is DISABLED!");
                logger.severe("The plugin will not work for any tools until the configuration is fixed.");
            }
        }
    }

    private void loadListMode(FileConfiguration config) {
        List<String> itemNames = config.getStringList("tool-settings.allowed-items");

        for (String name : itemNames) {
            NamespacedKey key = NamespacedKeyUtil.fromString(name);
            if (key == null) {
                logger.warning("The '" + name + "' tool has an invalid name format! I'm skipping it.");
                continue;
            }

            Material mat = Registry.MATERIAL.get(key);
            if (mat == null) {
                logger.warning("The '" + name + "' item (Key: " + key + ") was not found in the game! I'm skipping it.");
            } else {
                this.allowedTools.add(mat);
            }
        }
    }

    private void loadToolRestrictions(FileConfiguration config) {
        this.customNameRequired = config.getBoolean("tool-restrictions.custom-name-required", false);
        this.requiredCustomName = config.getString("tool-restrictions.required-custom-name", "");

        this.enchantmentRequired = config.getBoolean("tool-restrictions.enchantment-required", false);
        this.requiredEnchantmentLevel = config.getInt("tool-restrictions.required-enchantment-level", 1);
        this.requiredEnchantment = null;

        if (this.enchantmentRequired) {
            loadEnchantment(config);
        }
    }

    private void loadEnchantment(FileConfiguration config) {
        String enchantStr = config.getString("tool-restrictions.required-enchantment", "silk_touch").toLowerCase();

        NamespacedKey key = NamespacedKeyUtil.fromString(enchantStr);
        if (key == null) {
            logger.severe("--- CONFIGURATION ERROR ---");
            logger.severe("The specified enchantment format '" + enchantStr + "' contains invalid characters!");
            this.enchantmentRequired = false;
            return;
        }

        var enchantmentRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        Enchantment enchant = enchantmentRegistry.get(key);

        if (enchant == null) {
            logger.severe("--- CONFIGURATION ERROR ---");
            logger.severe("The enchantment '" + enchantStr + "' was not found!");
            logger.severe("Check for typos. Spell checking has been disabled until the fix is implemented.");
            this.enchantmentRequired = false;
        } else {
            this.requiredEnchantment = enchant;
        }
    }

    public boolean isWorldAllowed(World world) {
        if (world == null) return true;
        return switch (worldRestrictionMode) {
            case "blacklist" -> !restrictedWorlds.contains(world.getName().toLowerCase());
            case "whitelist" -> restrictedWorlds.contains(world.getName().toLowerCase());
            default -> true;
        };
    }

    public void logUnwax(Player player, Location loc, boolean denied) {
        if (denied && !logDeniedAttempts) return;
        if (!denied && !logUnwaxActions) return;

        String action = denied ? "denied unwax attempt" : "unwaxed sign";
        logger.info(String.format(
                "%s %s at %.0f, %.0f, %.0f in %s",
                player.getName(), action, loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName())
        );
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isPermissionsEnabled() {
        return permissionsEnabled;
    }

    public boolean shouldNotifyOnDenial() {
        return notifyOnDenial;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public boolean isSneakingRequired() {
        return requireSneaking;
    }

    public boolean isRegionProtectionCheckEnabled() {
        return regionProtectionCheckEnabled;
    }

    public long getUnwaxCooldownMs() {
        return unwaxCooldownMs;
    }

    public long getUnwaxBlockCooldownMs() {
        return unwaxBlockCooldownMs;
    }

    public boolean isDropEnabled() {
        return dropEnabled;
    }

    public Material getDropMaterial() {
        return dropMaterial;
    }

    public int getDropMinAmount() {
        return dropMinAmount;
    }

    public int getDropMaxAmount() {
        return dropMaxAmount;
    }

    public double getDropChance() {
        return dropChance;
    }

    public boolean isLogUnwaxActions() {
        return logUnwaxActions;
    }

    public boolean isLogDeniedAttempts() {
        return logDeniedAttempts;
    }

    public double getCostPerUnwax() {
        return costPerUnwax;
    }

    public Set<Material> getAllowedTools() {
        return Collections.unmodifiableSet(allowedToolsResolved);
    }

    public ToolMode getModeType() {
        return modeType;
    }

    public boolean isAxesTagFallbackEnabled() {
        return axesTagFallbackEnabled;
    }

    public boolean isConsumeDurabilityEnabled() {
        return consumeDurabilityEnabled;
    }

    public int getDurabilityCost() {
        return durabilityCost;
    }

    public boolean isCustomNameRequired() {
        return customNameRequired;
    }

    public String getRequiredCustomName() {
        return requiredCustomName;
    }

    public boolean isEnchantmentRequired() {
        return enchantmentRequired;
    }

    public Enchantment getRequiredEnchantment() {
        return requiredEnchantment;
    }

    public int getRequiredEnchantmentLevel() {
        return requiredEnchantmentLevel;
    }

    public String getWorldRestrictionMode() {
        return worldRestrictionMode;
    }
}
