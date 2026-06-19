package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.prometheus.client.Collector;

/**
 * A Sampler gathers metrics on the Minecraft server thread and caches an
 * immutable snapshot for the Prometheus HTTP thread to read.
 *
 * <p>
 * {@link #sample()} runs only on the server thread (driven by
 * {@link CollectorScheduler}), where touching Minecraft world state is safe.
 * {@link #collect()} runs on the HTTP thread and only returns the cached
 * snapshot, so a scrape never races the tick loop nor blocks the server.
 */
public abstract class Sampler extends Collector implements Collector.Describable {

    private static final Logger LOG = LogManager.getLogger("prometheus_exporter");

    private static final List<MetricFamilySamples> EMPTY = Collections.emptyList();

    /**
     * The collector name, used as a label in self-monitoring metrics.
     */
    public final String name;

    /**
     * How often this sampler should be refreshed, in server ticks.
     */
    public final int intervalTicks;

    /**
     * The latest immutable snapshot read by {@link #collect()}.
     */
    private final AtomicReference<List<MetricFamilySamples>> cache = new AtomicReference<>(EMPTY);

    /**
     * Epoch millis of the last successful refresh, or 0 if never refreshed.
     */
    private volatile long lastRefreshMillis;

    /**
     * Duration of the last successful {@link #sample()} call, in seconds.
     */
    private volatile double lastRefreshSeconds;

    protected Sampler(String name, int intervalTicks) {
        this.name = name;
        this.intervalTicks = intervalTicks;
    }

    /**
     * Gather the current metrics. Runs only on the Minecraft server thread.
     *
     * @return The collected metric families.
     */
    protected abstract List<MetricFamilySamples> sample();

    /**
     * Whether this sampler is due to refresh on the given tick.
     *
     * @param tick The current tick counter.
     *
     * @return Whether to refresh.
     */
    public boolean isDue(long tick) {
        return tick % this.intervalTicks == 0;
    }

    /**
     * Refresh the cached snapshot using the current wall clock.
     */
    public void refresh() {
        this.refreshAt(System.currentTimeMillis());
    }

    /**
     * Refresh the cached snapshot, recording the given time as the refresh
     * time. A failed {@link #sample()} keeps the previous snapshot and does not
     * advance the refresh time.
     *
     * @param nowMillis The current epoch millis.
     */
    public void refreshAt(long nowMillis) {
        long start = System.nanoTime();
        List<MetricFamilySamples> samples;
        try {
            samples = this.sample();
        } catch (Exception e) {
            LOG.warn("Collector '{}' failed to refresh; keeping previous snapshot.", this.name, e);
            return;
        }
        this.lastRefreshSeconds = (System.nanoTime() - start) / 1e9;
        this.lastRefreshMillis = nowMillis;
        this.cache.set(samples != null ? samples : EMPTY);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return this.cache.get();
    }

    @Override
    public List<MetricFamilySamples> describe() {
        return EMPTY;
    }

    /**
     * @return The duration of the last successful refresh, in seconds.
     */
    public double lastRefreshSeconds() {
        return this.lastRefreshSeconds;
    }

    /**
     * @return Seconds since the last successful refresh, using the current clock.
     */
    public double stalenessSeconds() {
        return this.stalenessSecondsAt(System.currentTimeMillis());
    }

    /**
     * @param nowMillis The current epoch millis.
     *
     * @return Seconds since the last successful refresh.
     */
    public double stalenessSecondsAt(long nowMillis) {
        return (nowMillis - this.lastRefreshMillis) / 1000.0;
    }
}
