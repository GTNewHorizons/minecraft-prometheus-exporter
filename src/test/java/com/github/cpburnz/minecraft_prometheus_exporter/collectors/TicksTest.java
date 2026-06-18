package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.prometheus.client.Histogram;

class TicksTest {

    @Test
    void dimTickChildCachedPerDimensionAndNameResolvedOnlyOnMiss() {
        Ticks ticks = new Ticks(null);
        AtomicInteger nameCalls = new AtomicInteger();

        Histogram.Child first = ticks.dimTickChild(0, () -> {
            nameCalls.incrementAndGet();
            return "Overworld";
        });
        Histogram.Child second = ticks.dimTickChild(0, () -> {
            nameCalls.incrementAndGet();
            return "Overworld";
        });

        assertSame(first, second, "same dimension reuses the cached child");
        assertEquals(1, nameCalls.get(), "dimension name resolved only on cache miss");
    }

    @Test
    void dimTickChildDistinctPerDimension() {
        Ticks ticks = new Ticks(null);
        Histogram.Child overworld = ticks.dimTickChild(0, () -> "Overworld");
        Histogram.Child nether = ticks.dimTickChild(-1, () -> "Nether");
        assertNotSame(overworld, nether);
    }
}
