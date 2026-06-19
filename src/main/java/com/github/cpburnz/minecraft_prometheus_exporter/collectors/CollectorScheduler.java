package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Drives {@link Sampler} refreshes from the Minecraft server tick, so all
 * Minecraft world access happens on the server thread. The Prometheus HTTP
 * thread only ever reads the cached snapshots via {@link Sampler#collect()}.
 */
@EventBusSubscriber
public class CollectorScheduler {

    /**
     * The active scheduler, or null when the exporter is stopped.
     */
    @Nullable
    public static CollectorScheduler Instance;

    private final List<Sampler> samplers = new CopyOnWriteArrayList<>();

    private long tick;

    /**
     * Register a sampler to be refreshed on its interval.
     *
     * @param sampler The sampler.
     */
    public void register(Sampler sampler) {
        this.samplers.add(sampler);
    }

    /**
     * Remove all registered samplers and reset the tick counter.
     */
    public void clear() {
        this.samplers.clear();
        this.tick = 0;
    }

    /**
     * @return The registered samplers, for self-monitoring.
     */
    public List<Sampler> samplers() {
        return Collections.unmodifiableList(this.samplers);
    }

    /**
     * Advance one tick and refresh any samplers due on this tick. Runs on the
     * Minecraft server thread.
     */
    public void tick() {
        long t = ++this.tick;
        for (Sampler sampler : this.samplers) {
            if (sampler.isDue(t)) {
                sampler.refresh();
            }
        }
    }

    /**
     * Refresh every registered sampler immediately, regardless of interval.
     * Used on startup so the first scrape is not empty.
     */
    public void refreshAll() {
        for (Sampler sampler : this.samplers) {
            sampler.refresh();
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (Instance != null && event.phase == TickEvent.Phase.END) {
            Instance.tick();
        }
    }
}
