package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import io.prometheus.client.GaugeMetricFamily;

/**
 * Collects per-dimension world time and weather. In vanilla these are shared from
 * the overworld via DerivedWorldInfo, but they are emitted per dimension so modpack
 * dimensions that override their own world info are captured.
 */
public class Environment extends BaseCollector {

    public Environment(MinecraftServer mc_server, int intervalTicks) {
        super(mc_server, "environment", intervalTicks);
    }

    @Override
    protected List<MetricFamilySamples> sample() {
        GaugeMetricFamily time = new GaugeMetricFamily(
            "mc_world_time",
            "The day-cycle time of the world in ticks (time of day = value % 24000).",
            Arrays.asList("dimension_id", "dimension_name"));
        GaugeMetricFamily raining = new GaugeMetricFamily(
            "mc_world_raining",
            "Whether it is raining in the dimension (1) or not (0).",
            Arrays.asList("dimension_id", "dimension_name"));
        GaugeMetricFamily thundering = new GaugeMetricFamily(
            "mc_world_thundering",
            "Whether it is thundering in the dimension (1) or not (0).",
            Arrays.asList("dimension_id", "dimension_name"));

        for (WorldServer world : DimensionManager.getWorlds()) {
            List<String> labels = Arrays
                .asList(Integer.toString(world.provider.dimensionId), world.provider.getDimensionName());
            time.addMetric(labels, world.getWorldTime());
            raining.addMetric(labels, world.isRaining() ? 1 : 0);
            thundering.addMetric(labels, world.isThundering() ? 1 : 0);
        }

        return Arrays.asList(time, raining, thundering);
    }
}
