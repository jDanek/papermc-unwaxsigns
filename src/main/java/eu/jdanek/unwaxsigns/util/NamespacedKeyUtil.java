package eu.jdanek.unwaxsigns.util;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

public final class NamespacedKeyUtil {

    private NamespacedKeyUtil() {
    }

    /**
     * Converts a string to a NamespacedKey. If it contains a colon, it is used as the full key;
     * otherwise, the namespace {@code minecraft:} is appended.
     */
    public static @Nullable NamespacedKey fromString(@Nullable String name) {
        if (name == null) return null;

        String clean = name.toLowerCase().trim();

        if (clean.isEmpty()) return null;

        if (clean.contains(":")) {
            return NamespacedKey.fromString(clean);
        }
        return NamespacedKey.minecraft(clean);
    }
}
