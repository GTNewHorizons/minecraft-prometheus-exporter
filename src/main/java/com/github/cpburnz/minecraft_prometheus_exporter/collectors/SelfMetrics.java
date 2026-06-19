package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

/**
 * Self-monitoring metrics about the collectors themselves: how long each
 * sampler's last refresh took and how stale its cached snapshot is. Reads only
 * volatile fields, so it is safe to collect on the HTTP thread.
 */
public class SelfMetrics extends Collector implements Collector.Describable {

    private final CollectorScheduler scheduler;

    public SelfMetrics(CollectorScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily duration = new GaugeMetricFamily(
            "mc_collector_refresh_duration_seconds",
            "Duration of each collector's last successful refresh, in seconds.",
            Collections.singletonList("collector"));
        GaugeMetricFamily staleness = new GaugeMetricFamily(
            "mc_collector_staleness_seconds",
            "Seconds since each collector's last successful refresh.",
            Collections.singletonList("collector"));

        long now = System.currentTimeMillis();
        for (Sampler sampler : this.scheduler.samplers()) {
            List<String> label = Collections.singletonList(sampler.name);
            duration.addMetric(label, sampler.lastRefreshSeconds());
            staleness.addMetric(label, sampler.stalenessSecondsAt(now));
        }

        return Arrays.asList(duration, staleness);
    }

    @Override
    public List<MetricFamilySamples> describe() {
        return Collections.emptyList();
    }
}
