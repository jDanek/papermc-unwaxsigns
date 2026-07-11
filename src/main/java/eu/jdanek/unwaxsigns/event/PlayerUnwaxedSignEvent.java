package eu.jdanek.unwaxsigns.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerUnwaxedSignEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private boolean cancelled;

    public PlayerUnwaxedSignEvent(@NotNull Player player, @NotNull Block block) {
        this.player = player;
        this.block = block;
        this.cancelled = false;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Block getBlock() {
        return block;
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
