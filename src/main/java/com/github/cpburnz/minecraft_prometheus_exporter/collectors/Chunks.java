package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import io.prometheus.client.GaugeMetricFamily;

public class Chunks extends BaseCollector {

    public Chunks(MinecraftServer mc_server, int intervalTicks) {
        super(mc_server, "chunks", intervalTicks);
    }

    private static GaugeMetricFamily newMetric() {
        return new GaugeMetricFamily(
            "mc_chunks_loaded",
            "The number of loaded chunks per dimension.",
            Arrays.asList("dimension_id", "dimension_name"));
    }

    @Override
    protected List<MetricFamilySamples> sample() {
        GaugeMetricFamily metric = newMetric();

        for (WorldServer world : DimensionManager.getWorlds()) {
            int loaded = world.getChunkProvider()
                .getLoadedChunkCount();
            metric.addMetric(
                Arrays.asList(Integer.toString(world.provider.dimensionId), world.provider.getDimensionName()),
                loaded);
        }

        return Arrays.asList(metric);
    }
}
