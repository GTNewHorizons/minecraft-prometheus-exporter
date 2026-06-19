package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.prometheus.client.GaugeMetricFamily;

class CollectorSchedulerTest {

    private static final class CountingSampler extends Sampler {

        final AtomicInteger samples = new AtomicInteger();

        CountingSampler(int intervalTicks) {
            super("counting", intervalTicks);
        }

        @Override
        protected List<MetricFamilySamples> sample() {
            this.samples.incrementAndGet();
            return Collections.singletonList(new GaugeMetricFamily("m", "h", 1.0));
        }
    }

    @Test
    void tickRefreshesSamplerOnlyOnItsInterval() {
        CollectorScheduler scheduler = new CollectorScheduler();
        CountingSampler every2 = new CountingSampler(2);
        scheduler.register(every2);

        for (int i = 0; i < 6; i++) {
            scheduler.tick();
        }

        // Ticks 2, 4, 6 are due -> 3 refreshes.
        assertEquals(3, every2.samples.get());
    }

    @Test
    void tickRespectsIndependentIntervals() {
        CollectorScheduler scheduler = new CollectorScheduler();
        CountingSampler every2 = new CountingSampler(2);
        CountingSampler every3 = new CountingSampler(3);
        scheduler.register(every2);
        scheduler.register(every3);

        for (int i = 0; i < 6; i++) {
            scheduler.tick();
        }

        assertEquals(3, every2.samples.get(), "due at 2,4,6");
        assertEquals(2, every3.samples.get(), "due at 3,6");
    }

    @Test
    void refreshAllRefreshesEverySamplerOnce() {
        CollectorScheduler scheduler = new CollectorScheduler();
        CountingSampler a = new CountingSampler(1000);
        CountingSampler b = new CountingSampler(1000);
        scheduler.register(a);
        scheduler.register(b);

        scheduler.refreshAll();

        assertEquals(1, a.samples.get());
        assertEquals(1, b.samples.get());
    }

    @Test
    void clearRemovesAllSamplers() {
        CollectorScheduler scheduler = new CollectorScheduler();
        CountingSampler a = new CountingSampler(1);
        scheduler.register(a);
        scheduler.clear();

        scheduler.tick();

        assertEquals(0, a.samples.get(), "cleared sampler must not refresh");
    }
}
