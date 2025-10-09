package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import io.prometheus.client.GaugeMetricFamily;
import serverutils.data.ClaimedChunk;
import serverutils.data.ClaimedChunks;
import serverutils.lib.data.ForgePlayer;
import serverutils.lib.data.ForgeTeam;
import serverutils.lib.data.Universe;

public class Teams extends BaseCollector {

    public Teams(MinecraftServer mc_server) {
        super(mc_server);
    }

    private static GaugeMetricFamily newClaimsMetric() {
        return new GaugeMetricFamily(
            "mc_teams_chunk_claims",
            "Number of chunk claims per team per dim.",
            Arrays.asList("team_id", "team_name", "team_type", "dim_id", "dim_name"));
    }

    private static GaugeMetricFamily newLoadsMetric() {
        return new GaugeMetricFamily(
            "mc_teams_chunk_loads",
            "Number of chunks being force loaded per team per dim.",
            Arrays.asList("team_id", "team_name", "team_type", "dim_id", "dim_name"));
    }

    private static GaugeMetricFamily newPlayersMetric() {
        return new GaugeMetricFamily(
            "mc_teams_players",
            "Players in a team",
            Arrays.asList("team_id", "team_name", "player_uuid", "player_name"));
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily claimsMetric = newClaimsMetric();
        GaugeMetricFamily loadsMetric = newLoadsMetric();
        GaugeMetricFamily playersMetric = newPlayersMetric();

        // SU not ready, bail
        if (!Universe.loaded()) return null;

        Universe universe = Universe.get();
        Collection<ForgeTeam> teams = universe.getTeams();
        if (ClaimedChunks.isActive()) {
            for (ForgeTeam team : teams) {
                for (WorldServer world : DimensionManager.getWorlds()) {
                    Set<ClaimedChunk> teamChunks = ClaimedChunks.instance
                        .getTeamChunks(team, OptionalInt.of(world.provider.dimensionId));

                    if (teamChunks.isEmpty()) continue;

                    List<String> labels = Arrays.asList(
                        team.getId(),
                        team.getTitle()
                            .getUnformattedText(),
                        team.type.getName(),
                        Integer.toString(world.provider.dimensionId),
                        world.provider.getDimensionName());
                    claimsMetric.addMetric(labels, teamChunks.size());
                    loadsMetric.addMetric(
                        labels,
                        (int) teamChunks.stream()
                            .filter(c -> c.forced != null ? c.forced : false)
                            .count());
                }
            }
        }

        for (ForgeTeam team : teams) {
            List<ForgePlayer> members = team.getMembers();
            for (ForgePlayer member : members) {
                playersMetric.addMetric(
                    Arrays.asList(
                        team.getId(),
                        team.getTitle()
                            .getUnformattedText(),
                        member.getId()
                            .toString(),
                        member.getName() // not using DisplayName as it will return getName if the player is offline
                    ),
                    1);
            }
        }

        return Arrays.asList(claimsMetric, loadsMetric, playersMetric);
    }

    static final List<MetricFamilySamples> desc = Arrays
        .asList(newClaimsMetric(), newLoadsMetric(), newPlayersMetric());

    @Override
    public List<MetricFamilySamples> describe() {
        return desc;
    }
}
