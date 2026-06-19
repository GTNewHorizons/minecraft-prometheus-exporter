package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import org.apache.commons.lang3.ObjectUtils;

import com.mojang.authlib.GameProfile;

import io.prometheus.client.GaugeMetricFamily;

public class Players extends BaseCollector {

    public Players(MinecraftServer mc_server, int intervalTicks) {
        super(mc_server, "players", intervalTicks);
    }

    private static GaugeMetricFamily newMetric() {
        return new GaugeMetricFamily(
            "mc_player_info",
            "The players connected to the server.",
            Arrays.asList("player_id", "player_name", "dimension_name", "dimension_id"));
    }

    private static GaugeMetricFamily newPositionMetric() {
        return new GaugeMetricFamily(
            "mc_player_position",
            "The position of connected players, one series per axis.",
            Arrays.asList("player_id", "player_name", "axis"));
    }

    @Override
    protected List<MetricFamilySamples> sample() {
        GaugeMetricFamily metric = newMetric();
        GaugeMetricFamily position = newPositionMetric();

        for (Object playerObj : this.mc_server.getConfigurationManager().playerEntityList) {
            // Get player profile.
            EntityPlayerMP player = (EntityPlayerMP) playerObj;
            GameProfile profile = player.getGameProfile();

            // Get player info.
            // - WARNING: Either "id" or "name" can be null.
            String id_str = "";
            UUID id = profile.getId();
            if (id != null) {
                id_str = id.toString();
            }

            String name = ObjectUtils.defaultIfNull(profile.getName(), "");
            World world = player.worldObj;
            String dimName = "Unknown";
            int dimID = 0;
            if (world != null) {
                dimName = world.provider.getDimensionName();
                dimID = world.provider.dimensionId;
            }
            metric.addMetric(Arrays.asList(id_str, name, dimName, Integer.toString(dimID)), 1);

            // Position is exposed as gauge values (not labels) to keep cardinality
            // bounded, one series per axis. Reading the entity here is safe:
            // sample() runs on the server thread.
            position.addMetric(Arrays.asList(id_str, name, "x"), player.posX);
            position.addMetric(Arrays.asList(id_str, name, "y"), player.posY);
            position.addMetric(Arrays.asList(id_str, name, "z"), player.posZ);
        }

        return Arrays.asList(metric, position);
    }
}
