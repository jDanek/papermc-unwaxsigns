package eu.jdanek.unwaxsigns.bstats;

import eu.jdanek.unwaxsigns.UnwaxSigns;
import eu.jdanek.unwaxsigns.config.ConfigManager;

import java.util.Map;
import java.util.concurrent.Callable;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.jetbrains.annotations.NotNull;

public class PluginMetrics {

    private static final int PLUGIN_ID = 32556;

    private final Metrics metrics;

    public PluginMetrics(UnwaxSigns plugin, MetricsCounters counters) {
        this.metrics = new Metrics(plugin, PLUGIN_ID);
        registerCharts(plugin, counters);
    }

    private void addChart(CustomChart chart) {
        metrics.addCustomChart(chart);
    }

    private void simplePie(@NotNull String chartId, Callable<String> callback) {
        metrics.addCustomChart(new SimplePie(chartId, callback));
    }

    private void advancedPie(String chartId, Callable<Map<String, Integer>> callback) {
        metrics.addCustomChart(new AdvancedPie(chartId, callback));
    }

    private void registerCharts(UnwaxSigns plugin, MetricsCounters counters) {
        ConfigManager config = plugin.getConfigManager();

        // Usage over time
        addChart(new SingleLineChart("signs_unwaxed", counters::getAndResetUnwaxCount));
        addChart(new SingleLineChart("denials_total", counters::getAndResetDenialCount));
        advancedPie("denial_reasons", counters::getAndResetDenialsByReason);

        // Configuration / adoption
        simplePie("language_used", () -> plugin.getLanguageManager().getDefaultLanguage());
        simplePie("config_version", () -> String.valueOf(config.getConfigVersion()));
        simplePie("notification_type", () -> config.getNotificationType().name().toLowerCase());
        simplePie("tool_mode", () -> config.getModeType().name().toLowerCase());
        simplePie("permissions_enabled", () -> String.valueOf(config.isPermissionsEnabled()));
        simplePie("economy_enabled", () -> String.valueOf(plugin.getEconomyHook().isEnabled()));
        simplePie("custom_name_required", () -> String.valueOf(config.isCustomNameRequired()));
        simplePie("enchantment_required", () -> String.valueOf(config.isEnchantmentRequired()));
        simplePie("region_protection_check", () -> String.valueOf(config.isRegionProtectionCheckEnabled()));
        simplePie("world_restriction_mode", () -> config.getWorldRestrictionMode().toLowerCase());
        simplePie("drop_on_unwax", () -> String.valueOf(config.isDropEnabled()));
        simplePie("block_cooldown_configured", () -> String.valueOf(config.getUnwaxBlockCooldownMs() > 0));

        // Distinct from vault_present: tells you whether admins who HAVE Vault
        // actually configured a non-zero cost, or just left it at the default 0.0.
        // Returns null (excluded from the chart) when economy isn't active, so
        // servers without Vault don't skew the result toward "false".
        simplePie("economy_cost_configured", () -> {
            if (!plugin.getEconomyHook().isEnabled()) return null;
            return String.valueOf(config.getCostPerUnwax() > 0.0);
        });

        // Compatibility
        simplePie("vault_present", () -> String.valueOf(plugin.getServer().getPluginManager().getPlugin("Vault") != null));
        simplePie("worldguard_present", () -> String.valueOf(plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null));
    }
}
