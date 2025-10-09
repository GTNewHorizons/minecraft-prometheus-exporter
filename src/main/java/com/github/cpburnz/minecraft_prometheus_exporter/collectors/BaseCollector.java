package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import net.minecraft.server.MinecraftServer;

import io.prometheus.client.Collector;

public abstract class BaseCollector extends Collector implements Collector.Describable {

    final MinecraftServer mc_server;

    public BaseCollector(MinecraftServer mc_server) {
        this.mc_server = mc_server;
    }
}
