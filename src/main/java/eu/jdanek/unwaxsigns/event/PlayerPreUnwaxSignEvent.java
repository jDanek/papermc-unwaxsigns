package eu.jdanek.unwaxsigns.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerPreUnwaxSignEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private final ItemStack tool;
    private boolean cancelled;

    public PlayerPreUnwaxSignEvent(@NotNull Player player, @NotNull Block block, @NotNull ItemStack tool) {
        this.player = player;
        this.block = block;
        this.tool = tool.clone();
        this.cancelled = false;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
