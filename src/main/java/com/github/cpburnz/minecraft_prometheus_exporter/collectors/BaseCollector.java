package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import net.minecraft.server.MinecraftServer;

/**
 * Base class for the interval-sampled collectors. Adds Minecraft server access
 * on top of {@link Sampler}'s snapshot caching. The {@link #sample()} body runs
 * on the server thread, so touching world state is safe.
 */
public abstract class BaseCollector extends Sampler {

    final MinecraftServer mc_server;

    protected BaseCollector(MinecraftServer mc_server, String name, int intervalTicks) {
        super(name, intervalTicks);
        this.mc_server = mc_server;
    }
}
