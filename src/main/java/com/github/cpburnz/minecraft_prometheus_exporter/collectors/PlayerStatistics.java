package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.stats.StatisticsFile;

import com.mojang.authlib.GameProfile;

import gnu.trove.map.hash.THashMap;
import io.prometheus.client.GaugeMetricFamily;

public class PlayerStatistics extends BaseCollector {

    /**
     * Maps each player id to his name and stats. This is used to persist player
     * stats after sign-out.
     */
    private final THashMap<UUID, PlayerInfo> players;

    /**
     * Maps each stat to its name. This is used to cache stat names.
     */
    private final THashMap<StatBase, String> stat_names;

    /**
     * The initial capacity for the players map. This is arbitrary.
     */
    protected static final int PLAYERS_INIT = 20;

    /**
     * The initial capacity to use for the stat names map. This was counted from
     * StatList.
     */
    private static final int STATS_INIT = 23;

    public PlayerStatistics(MinecraftServer mc_server) {
        super(mc_server);
        this.players = new THashMap<>(PLAYERS_INIT);
        this.stat_names = new THashMap<>(STATS_INIT);
    }

    private static GaugeMetricFamily newMetric() {
        return new GaugeMetricFamily(
            "mc_player_stat_total",
            "The general stats about players.",
            Arrays.asList("code", "name", "player_id", "player_name"));
    }

    @Override
    public List<MetricFamilySamples> collect() {
        // Cache player list and stats.
        for (EntityPlayerMP player : this.mc_server.getConfigurationManager().playerEntityList) {
            // Get player profile.
            GameProfile profile = player.getGameProfile();

            // Get player info.
            // - WARNING: Either "id" or "name" can be null in Minecraft 1.19 and
            // earlier.
            @Nullable
            UUID player_id = profile.getId();
            @Nullable
            String player_name = profile.getName();

            if (player_id != null && player_name != null) {
                StatisticsFile stats = player.func_147099_x();
                @Nullable
                PlayerInfo player_info = this.players.get(player_id);
                if (player_info != null) {
                    player_info.name = player_name;
                    player_info.stats = stats;
                } else {
                    player_info = new PlayerInfo(player_id, player_name, stats);
                    this.players.put(player_id, player_info);
                }
            }
        }

        // Collect player stats.
        GaugeMetricFamily metric = newMetric();
        for (PlayerInfo player_info : this.players.values()) {
            String player_id_str = player_info.id.toString();
            String player_name = player_info.name;
            StatisticsFile stats = player_info.stats;

            for (StatBase stat : StatList.generalStats) {
                // Get stat value.
                // - NOTICE: Despite its name, this reads the value.
                int stat_val = stats.writeStat(stat);
                String stat_code = stat.statId;
                @Nullable
                String stat_name = this.stat_names.get(stat);
                if (stat_name == null) {
                    // Cache stat name.
                    stat_name = stat.func_150951_e()
                        .getUnformattedText();
                    this.stat_names.put(stat, stat_name);
                }

                // Record score.
                metric.addMetric(Arrays.asList(stat_code, stat_name, player_id_str, player_name), stat_val);
            }
        }
        return Arrays.asList(metric);
    }

    static final List<MetricFamilySamples> desc = Collections.singletonList(newMetric());

    @Override
    public List<MetricFamilySamples> describe() {
        return desc;
    }

    /**
     * The PlayerInfo class contains the player name and stats.
     */
    private static class PlayerInfo {

        /**
         * The player id.
         */
        final UUID id;

        /**
         * The player name,
         */
        String name;

        /**
         * The statistics file.
         */
        StatisticsFile stats;

        /**
         * Constructs the PlayerInfo instance.
         *
         * @param id    The player id.
         * @param name  The player name.
         * @param stats The statistics file.
         */
        PlayerInfo(UUID id, String name, StatisticsFile stats) {
            this.id = id;
            this.name = name;
            this.stats = stats;
        }
    }
}
