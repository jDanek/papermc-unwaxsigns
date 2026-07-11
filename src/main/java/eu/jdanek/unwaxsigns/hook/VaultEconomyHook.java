package eu.jdanek.unwaxsigns.hook;

import eu.jdanek.unwaxsigns.UnwaxSigns;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomyHook implements EconomyHook {

    private final UnwaxSigns plugin;
    private final Economy economy;

    public VaultEconomyHook(UnwaxSigns plugin) {
        this.plugin = plugin;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        this.economy = rsp != null ? rsp.getProvider() : null;
    }

    @Override
    public boolean isEnabled() {
        return economy != null;
    }

    @Override
    public boolean hasEnough(Player player, double amount) {
        if (economy == null || amount <= 0) return true;
        return economy.has(player, amount);
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (economy == null || amount <= 0) return true;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    @Override
    public String format(double amount) {
        if (economy == null) return String.valueOf(amount);
        return economy.format(amount);
    }
}
