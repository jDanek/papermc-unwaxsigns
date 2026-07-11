package eu.jdanek.unwaxsigns.event;

import java.util.Collections;
import java.util.Map;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerUnwaxDenialEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Block block;
    private final DenialReason reason;
    private final Map<String, String> context;

    public PlayerUnwaxDenialEvent(@NotNull Player player, @NotNull Block block, @NotNull DenialReason reason, @Nullable Map<String, String> context) {
        this.player = player;
        this.block = block;
        this.reason = reason;
        this.context = context != null ? Map.copyOf(context) : Collections.emptyMap();
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Block getBlock() {
        return block;
    }

    public @NotNull DenialReason getReason() {
        return reason;
    }

    public @NotNull Map<String, String> getContext() {
        return context;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
