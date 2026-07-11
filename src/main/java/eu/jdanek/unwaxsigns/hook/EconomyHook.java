package eu.jdanek.unwaxsigns.hook;

import org.bukkit.entity.Player;

public interface EconomyHook {
    boolean isEnabled();

    boolean hasEnough(Player player, double amount);

    boolean withdraw(Player player, double amount);

    String format(double amount);
}
