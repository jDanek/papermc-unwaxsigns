package eu.jdanek.unwaxsigns.hook;

import org.bukkit.entity.Player;

public class NoopEconomyHook implements EconomyHook {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean hasEnough(Player player, double amount) {
        return true;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        return true;
    }

    @Override
    public String format(double amount) {
        return String.valueOf(amount);
    }
}
