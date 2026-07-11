package eu.jdanek.unwaxsigns.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic timestamp-based cooldown tracker with periodic cleanup support. Thread-safe for the sweep
 * to be callable from any thread if ever needed, though normal usage happens on the main server
 * thread.
 */
public class CooldownTracker<K> {

    private final Map<K, Long> timestamps = new ConcurrentHashMap<>();

    /**
     * True if the key is still within the given cooldown window.
     */
    public boolean isOnCooldown(K key, long cooldownMs) {
        if (cooldownMs <= 0) return false;
        Long last = timestamps.get(key);
        return last != null && (System.currentTimeMillis() - last) < cooldownMs;
    }

    public void record(K key) {
        timestamps.put(key, System.currentTimeMillis());
    }

    public void remove(K key) {
        timestamps.remove(key);
    }

    public void clear() {
        timestamps.clear();
    }

    /**
     * Removes entries older than maxAgeMs. Call periodically to bound memory use.
     */
    public void sweep(long maxAgeMs) {
        long now = System.currentTimeMillis();
        timestamps.entrySet().removeIf(e -> (now - e.getValue()) > maxAgeMs);
    }

    public int size() {
        return timestamps.size();
    }
}
