package eu.jdanek.unwaxsigns.bstats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsCounters {

    private final AtomicInteger unwaxCount = new AtomicInteger();
    private final AtomicInteger denialCount = new AtomicInteger();
    private final Map<String, Integer> denialsByReason = new ConcurrentHashMap<>();

    public void recordUnwax() {
        unwaxCount.incrementAndGet();
    }

    public void recordDenial(String reasonName) {
        denialCount.incrementAndGet();
        denialsByReason.merge(reasonName, 1, Integer::sum);
    }

    public synchronized Map<String, Integer> getAndResetDenialsByReason() {
        Map<String, Integer> snapshot = Map.copyOf(denialsByReason);
        denialsByReason.clear();
        return snapshot;
    }

    public int getAndResetUnwaxCount() {
        return unwaxCount.getAndSet(0);
    }

    public int getAndResetDenialCount() {
        return denialCount.getAndSet(0);
    }
}
