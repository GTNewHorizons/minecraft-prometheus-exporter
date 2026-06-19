package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.GaugeMetricFamily;

class SelfMetricsTest {

    private static final class FakeSampler extends Sampler {

        FakeSampler(String name) {
            super(name, 5);
        }

        @Override
        protected List<MetricFamilySamples> sample() {
            return Collections.singletonList(new GaugeMetricFamily("m", "h", 1.0));
        }
    }

    private static MetricFamilySamples familyNamed(List<MetricFamilySamples> families, String name) {
        for (MetricFamilySamples f : families) {
            if (f.name.equals(name)) {
                return f;
            }
        }
        return null;
    }

    private static Sample sampleForCollector(MetricFamilySamples family, String collector) {
        for (Sample s : family.samples) {
            if (s.labelValues.contains(collector)) {
                return s;
            }
        }
        return null;
    }

    @Test
    void exposesRefreshDurationAndStalenessPerSampler() {
        CollectorScheduler scheduler = new CollectorScheduler();
        FakeSampler a = new FakeSampler("alpha");
        FakeSampler b = new FakeSampler("beta");
        scheduler.register(a);
        scheduler.register(b);
        scheduler.refreshAll();

        SelfMetrics self = new SelfMetrics(scheduler);
        List<MetricFamilySamples> families = self.collect();

        MetricFamilySamples duration = familyNamed(families, "mc_collector_refresh_duration_seconds");
        MetricFamilySamples staleness = familyNamed(families, "mc_collector_staleness_seconds");
        assertNotNull(duration, "duration family present");
        assertNotNull(staleness, "staleness family present");

        assertEquals(2, duration.samples.size(), "one duration sample per sampler");

        Sample alphaDuration = sampleForCollector(duration, "alpha");
        assertNotNull(alphaDuration);
        assertEquals("collector", alphaDuration.labelNames.get(0));
        assertEquals(a.lastRefreshSeconds(), alphaDuration.value, 1e-9);

        Sample betaStaleness = sampleForCollector(staleness, "beta");
        assertNotNull(betaStaleness);
        assertTrue(betaStaleness.value >= 0.0);
    }
}
