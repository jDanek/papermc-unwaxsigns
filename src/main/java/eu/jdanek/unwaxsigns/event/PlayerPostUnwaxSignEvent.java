package eu.jdanek.unwaxsigns.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerPostUnwaxSignEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private final ItemStack tool;
    private final double economyCost;
    private final int durabilityCost;
    private final boolean dropHandled;

    public PlayerPostUnwaxSignEvent(@NotNull Player player, @NotNull Block block, @NotNull ItemStack tool, double economyCost, int durabilityCost, boolean dropHandled) {
        this.player = player;
        this.block = block;
        this.tool = tool.clone();
        this.economyCost = economyCost;
        this.durabilityCost = durabilityCost;
        this.dropHandled = dropHandled;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Block getBlock() {
        return block;
    }

    public @NotNull ItemStack getTool() {
        return tool.clone();
    }

    public double getEconomyCost() {
        return economyCost;
    }

    public int getDurabilityCost() {
        return durabilityCost;
    }

    public boolean isDropHandled() {
        return dropHandled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
