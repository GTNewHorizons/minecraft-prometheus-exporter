package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.prometheus.client.GaugeMetricFamily;

class SamplerTest {

    /**
     * A test sampler whose output and failure behaviour can be driven by the test.
     */
    private static final class FakeSampler extends Sampler {

        final AtomicInteger sampleCount = new AtomicInteger();
        double value = 1.0;
        RuntimeException toThrow;

        FakeSampler(int intervalTicks) {
            super("fake", intervalTicks);
        }

        @Override
        protected List<MetricFamilySamples> sample() {
            this.sampleCount.incrementAndGet();
            if (this.toThrow != null) {
                throw this.toThrow;
            }
            return Collections.singletonList(new GaugeMetricFamily("fake_metric", "help", this.value));
        }
    }

    private static double singleValue(List<Sampler.MetricFamilySamples> families) {
        return families.get(0).samples.get(0).value;
    }

    @Test
    void collectReturnsEmptyListBeforeFirstRefresh() {
        FakeSampler s = new FakeSampler(5);
        List<Sampler.MetricFamilySamples> families = s.collect();
        assertNotNull(families, "collect() must never return null");
        assertTrue(families.isEmpty(), "no snapshot yet -> empty");
    }

    @Test
    void isDueOnlyOnIntervalMultiples() {
        FakeSampler s = new FakeSampler(5);
        assertTrue(s.isDue(0));
        assertFalse(s.isDue(1));
        assertFalse(s.isDue(4));
        assertTrue(s.isDue(5));
        assertTrue(s.isDue(10));
    }

    @Test
    void refreshPublishesSampleOutput() {
        FakeSampler s = new FakeSampler(5);
        s.value = 42.0;
        s.refresh();
        assertEquals(1, s.sampleCount.get());
        assertEquals(42.0, singleValue(s.collect()));
    }

    @Test
    void refreshFailureKeepsPreviousSnapshot() {
        FakeSampler s = new FakeSampler(5);
        s.value = 7.0;
        s.refresh();

        s.toThrow = new RuntimeException("boom");
        s.refresh();

        // Old snapshot survives a failed refresh.
        assertEquals(7.0, singleValue(s.collect()));
    }

    @Test
    void lastRefreshDurationRecordedOnSuccess() {
        FakeSampler s = new FakeSampler(5);
        s.refresh();
        assertTrue(s.lastRefreshSeconds() >= 0.0);
    }

    @Test
    void stalenessGrowsFromLastSuccessfulRefresh() {
        FakeSampler s = new FakeSampler(5);
        s.refreshAt(1_000L);
        assertEquals(2.0, s.stalenessSecondsAt(3_000L), 1e-9);

        // A failed refresh must not reset staleness.
        s.toThrow = new RuntimeException("boom");
        s.refreshAt(5_000L);
        assertEquals(4.0, s.stalenessSecondsAt(5_000L), 1e-9);
    }
}
