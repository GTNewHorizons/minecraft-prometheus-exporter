package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import io.prometheus.client.GaugeMetricFamily;

public class Chunks extends BaseCollector {

    public Chunks(MinecraftServer mc_server) {
        super(mc_server);
    }

    private static GaugeMetricFamily newMetric() {
        return new GaugeMetricFamily(
            "mc_dimension_chunks_loaded",
            "The number of loaded dimension chunks.",
            Arrays.asList("id", "name"));
    }

    @Override
    public List<MetricFamilySamples> collect() {
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

    static final List<MetricFamilySamples> desc = Collections.singletonList(newMetric());

    @Override
    public List<MetricFamilySamples> describe() {
        return desc;
    }
}
