package eu.jdanek.unwaxsigns.event;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SignUnwaxedDropEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private ItemStack dropItem;
    private Location dropLocation;
    private boolean cancelled;

    public SignUnwaxedDropEvent(@NotNull Player player, @NotNull Block block, @NotNull ItemStack dropItem, @NotNull Location dropLocation) {
        this.player = player;
        this.block = block;
        this.dropItem = dropItem.clone();
        this.dropLocation = dropLocation.clone();
        this.cancelled = false;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Block getBlock() {
        return block;
    }

    public @Nullable ItemStack getDropItem() {
        return dropItem != null ? dropItem.clone() : null;
    }

    public void setDropItem(@Nullable ItemStack dropItem) {
        this.dropItem = dropItem;
    }

    public @NotNull Location getDropLocation() {
        return dropLocation.clone();
    }

    public void setDropLocation(@NotNull Location dropLocation) {
        this.dropLocation = dropLocation;
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
