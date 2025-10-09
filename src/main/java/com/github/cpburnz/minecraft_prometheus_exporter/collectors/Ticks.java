package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldProvider;

import com.github.cpburnz.minecraft_prometheus_exporter.ExporterConfig;
import com.github.cpburnz.minecraft_prometheus_exporter.PrometheusExporterMod;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

@EventBusSubscriber
public class Ticks extends BaseCollector {

    public static Ticks Instance;

    private final Histogram server_tick_seconds;
    private final Histogram dim_tick_seconds;
    private final Gauge server_total_ticks;

    /**
     * The active timer when timing a server tick.
     */
    @Nullable
    private Histogram.Timer server_tick_timer;

    /**
     * The active dimension id being timed.
     */
    @Nullable
    private Integer dim_tick_id;

    /**
     * The active timer when timing a dimension tick.
     */
    @Nullable
    private Histogram.Timer dim_tick_timer;

    /**
     * Contains a dimensions id if any ticks have been started during this collectors lifecycle
     */
    private final ConcurrentHashMap.KeySetView<Integer, Boolean> dims_have_ticked;

    /**
     * The histogram buckets to use for ticks.
     */
    private static final double[] TICK_BUCKETS = new double[] { 0.01, 0.025, 0.05, 0.10, 0.25, 0.5, 1.0, };

    public Ticks(MinecraftServer mc_server) {
        super(mc_server);
        this.dims_have_ticked = ConcurrentHashMap.newKeySet(3);

        // Setup server metrics.
        this.server_tick_seconds = Histogram.build()
            .buckets(TICK_BUCKETS)
            .name("mc_server_tick_seconds")
            .help("Stats on server tick times.")
            .create();

        this.dim_tick_seconds = Histogram.build()
            .buckets(TICK_BUCKETS)
            .name("mc_dimension_tick_seconds")
            .labelNames("id", "name")
            .help("Stats on dimension tick times.")
            .create();

        this.server_total_ticks = Gauge.build()
            .name("mc_server_ticks_total_counter")
            .help("DIM0's total ticks")
            .create();

        if (Instance == null) {
            Instance = this;
        }
    }

    @EventBusSubscriber.Condition
    public static boolean enabled() {
        return ExporterConfig.collector.ticks;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> server_ticks = this.server_tick_seconds.collect();
        List<MetricFamilySamples> dim_ticks = this.dim_tick_seconds.collect();

        server_total_ticks.set(
            mc_server.getEntityWorld()
                .getTotalWorldTime());

        ArrayList<MetricFamilySamples> metrics = new ArrayList<>(
            +server_ticks.size() + dim_ticks.size() + 1 /* raw tick time */
        );

        metrics.addAll(server_ticks);
        metrics.addAll(dim_ticks);
        metrics.addAll(server_total_ticks.collect());

        return metrics;
    }

    @Override
    public List<MetricFamilySamples> describe() {
        ArrayList<MetricFamilySamples> descs = new ArrayList<>();
        descs.addAll(this.server_tick_seconds.describe());
        descs.addAll(this.dim_tick_seconds.describe());
        descs.addAll(server_total_ticks.describe());
        return descs;
    }

    /**
     * Record when a dimension tick begins.
     *
     * @param dim The dimension type.
     */
    public void startDimensionTick(WorldProvider dim) {
        int id = dim.dimensionId;
        if (this.dim_tick_timer != null) {
            switch (ExporterConfig.collector.collector_mc_dimension_tick_errors) {
                case IGNORE -> {} // Ignore error.

                case LOG -> PrometheusExporterMod.LOG
                    .debug("Dimension {} tick started before stopping previous tick.", id);

                case STRICT -> throw new IllegalStateException(
                    "Dimension " + id
                        + " tick started before stopping previous tick for "
                        + "dimension "
                        + this.dim_tick_id
                        + ".");
            }

            // Stop forgotten timer.
            dim_tick_timer.close();
            dim_tick_timer = null;
        }

        String id_str = Integer.toString(id);
        String name = dim.getDimensionName();
        this.dim_tick_id = id;
        this.dim_tick_timer = this.dim_tick_seconds.labels(id_str, name)
            .startTimer();
        this.dims_have_ticked.add(id);
    }

    /**
     * Record when a dimension tick finishes.
     *
     * @param dim The dimension type.
     */
    public void stopDimensionTick(WorldProvider dim) {
        int id = dim.dimensionId;
        if (this.dim_tick_timer == null) {
            if (!this.dims_have_ticked.contains(id)) {
                // WARNING: After restarting the collector, we may start during a
                // dimension tick. Do not fail in this scenario.
                return;
            }

            switch (ExporterConfig.collector.collector_mc_dimension_tick_errors) {
                case IGNORE -> {} // Ignore error.

                case LOG -> PrometheusExporterMod.LOG.debug("Dimension {} tick stopped without an active tick.", id);

                case STRICT -> throw new IllegalStateException(
                    ("Dimension " + id + " tick stopped without an active tick."));
            }

            // No timer to stop.
            return;
        } else if (this.dim_tick_id != null && this.dim_tick_id != id) {
            throw new IllegalStateException(
                "Dimension " + id
                    + " tick stopped while in an active tick for "
                    + "dimension "
                    + this.dim_tick_id
                    + ".");
        }

        this.dim_tick_timer.observeDuration();
        this.dim_tick_timer = null;
        this.dim_tick_id = null;
    }

    /**
     * Record when a server tick begins.
     */
    public void startServerTick() {
        if (this.server_tick_timer != null) {
            throw new IllegalStateException("Server tick started before stopping previous tick.");
        }

        this.server_tick_timer = this.server_tick_seconds.startTimer();
    }

    /**
     * Record when a server tick finishes.
     */
    public void stopServerTick() {
        if (this.server_tick_timer == null) {
            throw new IllegalStateException("Server tick stopped without an active tick.");
        }

        server_tick_timer.observeDuration();
        this.server_tick_timer = null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Record server tick.
        if (event.phase == TickEvent.Phase.START) {
            Instance.startServerTick();
        } else if (event.phase == TickEvent.Phase.END) {
            Instance.stopServerTick();
        }
    }

    /**
     * Called on a dimension tick.
     *
     * @param event The event.
     */
    @SubscribeEvent
    public static void onDimensionTick(TickEvent.WorldTickEvent event) {
        // Record dimension tick.
        WorldProvider dim = event.world.provider;
        if (event.phase == TickEvent.Phase.START) {
            Instance.startDimensionTick(dim);
        } else if (event.phase == TickEvent.Phase.END) {
            Instance.stopDimensionTick(dim);
        }
    }
}
