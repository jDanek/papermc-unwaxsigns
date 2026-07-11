package eu.jdanek.unwaxsigns.listener;

import eu.jdanek.unwaxsigns.UnwaxSigns;
import eu.jdanek.unwaxsigns.bstats.MetricsCounters;
import eu.jdanek.unwaxsigns.config.ConfigManager;
import eu.jdanek.unwaxsigns.config.EffectsConfigManager;
import eu.jdanek.unwaxsigns.event.*;
import eu.jdanek.unwaxsigns.util.CooldownTracker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

public class InteractListener implements Listener {

    private final UnwaxSigns plugin;
    private final MetricsCounters metricsCounters;
    private final CooldownTracker<UUID> playerCooldowns = new CooldownTracker<>();
    private final CooldownTracker<BlockKey> blockCooldowns = new CooldownTracker<>();
    private BukkitTask cleanupTask;

    public InteractListener(UnwaxSigns plugin, MetricsCounters metricsCounters) {
        this.plugin = plugin;
        this.metricsCounters = metricsCounters;
    }

    /**
     * Starts a periodic sweep that purges expired cooldown entries so the maps don't grow unbounded.
     */
    public void startCleanupTask() {
        long periodTicks = 20L * 60 * 5; // every 5 minutes
        this.cleanupTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long maxAge = Math.max(
                    plugin.getConfigManager().getUnwaxCooldownMs(),
                    plugin.getConfigManager().getUnwaxBlockCooldownMs()
            );
            // Safety margin so we never sweep an entry that's still technically on cooldown.
            long safeMaxAge = Math.max(maxAge * 2, 60_000L);
            playerCooldowns.sweep(safeMaxAge);
            blockCooldowns.sweep(safeMaxAge);
        }, periodTicks, periodTicks);
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        playerCooldowns.clear();
        blockCooldowns.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Immediate cleanup on quit; the periodic sweep is the safety net for
        // cases without a clean PlayerQuitEvent (crashes, force-kicks, etc.).
        playerCooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        ConfigManager config = plugin.getConfigManager();
        if (!config.isWorldAllowed(clickedBlock.getWorld())) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir() || !config.getAllowedTools().contains(item.getType())) return;

        if (!(clickedBlock.getState() instanceof Sign sign) || !sign.isWaxed()) return;

        Player player = event.getPlayer();

        // Pre-event
        PlayerPreUnwaxSignEvent preEvent = new PlayerPreUnwaxSignEvent(player, clickedBlock, item);
        plugin.getServer().getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            event.setCancelled(true);
            fireDenial(player, clickedBlock, DenialReason.CANCELLED_BY_PLUGIN, null);
            return;
        }

        // Validation chain
        if (!validateSneaking(player, event, clickedBlock, config)) {
            fireDenial(player, clickedBlock, DenialReason.SNEAKING_REQUIRED, null);
            return;
        }
        if (!validatePermission(player, event, clickedBlock)) {
            fireDenial(player, clickedBlock, DenialReason.NO_PERMISSION, null);
            return;
        }
        if (!validateCooldown(player, event, config)) {
            fireDenial(player, clickedBlock, DenialReason.COOLDOWN, null);
            return;
        }
        if (!validateBlockCooldown(clickedBlock, event, config)) {
            fireDenial(player, clickedBlock, DenialReason.COOLDOWN, null);
            return;
        }
        if (!validateCustomName(player, item, event, clickedBlock, config)) {
            fireDenial(player, clickedBlock, DenialReason.WRONG_CUSTOM_NAME, Map.of("required_name", config.getRequiredCustomName()));
            return;
        }
        if (!validateEnchantment(player, item, event, clickedBlock, config)) {
            Enchantment required = config.getRequiredEnchantment();
            String requiredName = required != null
                    ? required.displayName(config.getRequiredEnchantmentLevel()).toString()
                    : "unknown";
            fireDenial(player, clickedBlock, DenialReason.WRONG_ENCHANTMENT, Map.of("required_enchantment", requiredName));
            return;
        }
        if (!validateEconomy(player, event, clickedBlock, config)) {
            fireDenial(player, clickedBlock, DenialReason.NOT_ENOUGH_MONEY, Map.of("cost", plugin.getEconomyHook().format(config.getCostPerUnwax())));
            return;
        }

        if (config.isRegionProtectionCheckEnabled() && !validateRegionProtection(player, clickedBlock, event)) {
            fireDenial(player, clickedBlock, DenialReason.REGION_PROTECTED, null);
            return;
        }

        // Cancellable event (existing)
        PlayerUnwaxedSignEvent unwaxEvent = new PlayerUnwaxedSignEvent(player, clickedBlock);
        plugin.getServer().getPluginManager().callEvent(unwaxEvent);
        if (unwaxEvent.isCancelled()) {
            event.setCancelled(true);
            fireDenial(player, clickedBlock, DenialReason.CANCELLED_BY_PLUGIN, null);
            return;
        }

        // Execute
        performUnwax(player, clickedBlock, item, event, config);
    }

    private boolean validateSneaking(Player player, PlayerInteractEvent event, Block block, ConfigManager config) {
        if (!config.isSneakingRequired() || player.isSneaking()) return true;
        event.setCancelled(true);
        plugin.getNotificationService().sendFailure(player, block.getLocation(), "messages.sneak-required");
        return false;
    }

    private boolean validatePermission(Player player, PlayerInteractEvent event, Block block) {
        if (plugin.checkPermission(player, "unwaxsigns.use")) return true;
        event.setCancelled(true);
        plugin.getNotificationService().sendDenial(player, block, "messages.no-permission");
        return false;
    }

    private boolean validateCooldown(Player player, PlayerInteractEvent event, ConfigManager config) {
        if (player.hasPermission("unwaxsigns.bypass.cooldown")) return true;
        if (!playerCooldowns.isOnCooldown(player.getUniqueId(), config.getUnwaxCooldownMs())) return true;

        event.setCancelled(true);
        return false;
    }

    private boolean validateBlockCooldown(Block block, PlayerInteractEvent event, ConfigManager config) {
        if (!blockCooldowns.isOnCooldown(BlockKey.of(block), config.getUnwaxBlockCooldownMs())) return true;

        event.setCancelled(true);
        return false;
    }

    private boolean validateCustomName(Player player, ItemStack item, PlayerInteractEvent event, Block block, ConfigManager config) {
        if (!config.isCustomNameRequired() || player.hasPermission("unwaxsigns.bypass.name")) return true;

        String required = config.getRequiredCustomName();
        boolean fail = true;

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                Component name = meta.displayName();
                if (name != null) {
                    String plain = PlainTextComponentSerializer.plainText().serialize(name);
                    if (plain.equalsIgnoreCase(required)) {
                        fail = false;
                    }
                }
            }
        }

        if (!fail) return true;

        event.setCancelled(true);
        plugin.getNotificationService().sendFailure(
                player,
                block.getLocation(),
                "messages.wrong-custom-name",
                Map.of("required_name", required)
        );
        return false;
    }

    private boolean validateEnchantment(Player player, ItemStack item, PlayerInteractEvent event, Block block, ConfigManager config) {
        if (!config.isEnchantmentRequired() || player.hasPermission("unwaxsigns.bypass.enchantment")) return true;
        if (config.getRequiredEnchantment() == null) return true;
        if (item.getEnchantmentLevel(config.getRequiredEnchantment()) >= config.getRequiredEnchantmentLevel()) return true;

        Enchantment required = config.getRequiredEnchantment();
        String requiredName = required.displayName(config.getRequiredEnchantmentLevel()).toString();

        event.setCancelled(true);
        plugin.getNotificationService().sendFailure(
                player,
                block.getLocation(),
                "messages.wrong-enchantment",
                Map.of("required_enchantment", requiredName)
        );
        return false;
    }

    private boolean validateEconomy(Player player, PlayerInteractEvent event, Block block, ConfigManager config) {
        double cost = config.getCostPerUnwax();
        if (cost <= 0 || player.hasPermission("unwaxsigns.bypass.cost")) return true;
        if (plugin.getEconomyHook().hasEnough(player, cost)) return true;

        event.setCancelled(true);
        plugin.getNotificationService().sendFailure(
                player,
                block.getLocation(),
                "messages.not-enough-money",
                Map.of("cost", plugin.getEconomyHook().format(cost))
        );
        return false;
    }

    private boolean validateRegionProtection(Player player, Block block, PlayerInteractEvent event) {
        if (player.isOp()) return true;

        try {
            BlockBreakEvent mockEvent = new BlockBreakEvent(block, player);
            plugin.getServer().getPluginManager().callEvent(mockEvent);
            if (mockEvent.isCancelled()) {
                event.setCancelled(true);
                plugin.getNotificationService().sendDenial(player, block, "messages.no-permission");
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Protection compatibility check failed for "
                    + player.getName() + " (" + e.getClass().getSimpleName() + "): " + e.getMessage()
            );
            event.setCancelled(true);
            return false;
        }

        return true;
    }

    private void performUnwax(Player player, Block block, ItemStack item, PlayerInteractEvent event, ConfigManager config) {
        Sign sign = (Sign) block.getState();
        EffectsConfigManager effects = plugin.getEffectsConfigManager();

        double cost = config.getCostPerUnwax();
        if (cost > 0) {
            plugin.getEconomyHook().withdraw(player, cost);
        }

        sign.setWaxed(false);
        sign.update();

        effects.playSuccess(block.getLocation(), player, true);
        if (event.getHand() != null) {
            player.swingHand(event.getHand());
        }

        // Drop via event
        boolean dropHandled = false;
        if (config.isDropEnabled() && config.getDropChance() > 0) {
            if (ThreadLocalRandom.current().nextDouble() < config.getDropChance()) {
                int amount = config.getDropMinAmount();
                if (config.getDropMaxAmount() > config.getDropMinAmount()) {
                    amount = ThreadLocalRandom.current().nextInt(config.getDropMinAmount(), config.getDropMaxAmount() + 1);
                }
                amount = Math.min(amount, config.getDropMaterial().getMaxStackSize());
                ItemStack drop = new ItemStack(config.getDropMaterial(), amount);

                SignUnwaxedDropEvent dropEvent = new SignUnwaxedDropEvent(player, block, drop, block.getLocation());
                plugin.getServer().getPluginManager().callEvent(dropEvent);

                if (!dropEvent.isCancelled() && dropEvent.getDropItem() != null) {
                    block.getWorld().dropItemNaturally(dropEvent.getDropLocation(), dropEvent.getDropItem());
                    dropHandled = true;
                }
            }
        }

        if (config.isConsumeDurabilityEnabled()
                && player.getGameMode() != GameMode.CREATIVE
                && player.getGameMode() != GameMode.SPECTATOR
                && item.getType().getMaxDurability() > 0
        ) {
            item.damage(config.getDurabilityCost(), player);
        }

        // Update cooldowns
        config.logUnwax(player, block.getLocation(), false);
        if (config.getUnwaxCooldownMs() > 0) {
            playerCooldowns.record(player.getUniqueId());
        }
        if (config.getUnwaxBlockCooldownMs() > 0) {
            blockCooldowns.record(BlockKey.of(block));
        }

        // Cancel vanilla actions
        event.setCancelled(true);

        metricsCounters.recordUnwax();

        // Post event
        PlayerPostUnwaxSignEvent postEvent = new PlayerPostUnwaxSignEvent(player, block, item, cost, config.getDurabilityCost(), dropHandled);
        plugin.getServer().getPluginManager().callEvent(postEvent);
    }

    private void fireDenial(Player player, Block block, DenialReason reason, Map<String, String> context) {
        metricsCounters.recordDenial(reason.name());
        PlayerUnwaxDenialEvent denialEvent = new PlayerUnwaxDenialEvent(player, block, reason, context);
        plugin.getServer().getPluginManager().callEvent(denialEvent);
    }

    private record BlockKey(String world, int x, int y, int z) {
        static BlockKey of(Block block) {
            return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }
}
