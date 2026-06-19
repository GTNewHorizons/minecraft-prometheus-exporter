This mod provides a Prometheus exporter for Minecraft. It exports metrics
related to the Minecraft server and the JVM for consumption by the open-source
systems monitoring toolkit, [Prometheus](https://prometheus.io/). The mod is intended for server-side
use, and does not need to be installed client-side. This currently has builds
for the following versions:

- Minecraft 1.7.10 with Forge 10.13.4.

Installation
------------

The Prometheus Exporter mod only needs to be installed on the server.
Since this mod does not add anything to the Minecraft world,
it can be safely upgraded by simply replacing an older version with a newer
version.

Migrating
---------

If you are coming from the upstream repo, the old config file will transfer the following configs:
- listen_address
- listen_port
- jwm_collector

The following changes occured that may require panel adjustments from coming from upstream:
- mc_player_list: current dimension added

### Metric renames (caching rewrite)

Metric names and labels were standardized to Prometheus naming conventions
(consistent `dimension_id` / `dimension_name` labels, no `_total` suffix on
current-value gauges, singular `team` prefix). Update any dashboards accordingly:

| Old | New |
|-----|-----|
| `mc_dimension_tick_seconds{id,name}` | `mc_dimension_tick_seconds{dimension_id,dimension_name}` |
| `mc_server_ticks_total_counter` | `mc_server_ticks_total` |
| `mc_entities_total{dim,dim_id,id,type}` | `mc_entities{dimension_name,dimension_id,entity_id,entity_type}` |
| `mc_dimension_tileentities{id,name}` | `mc_tileentities{dimension_id,dimension_name}` |
| `mc_dimension_tileentities_detailed{dim_id,dim,te_class,te_name}` | `mc_tileentities_detailed{dimension_id,dimension_name,te_class,te_name}` |
| `mc_dimension_chunks_loaded{id,name}` | `mc_chunks_loaded{dimension_id,dimension_name}` |
| `mc_player_list{id,name,dim,dim_id}` | `mc_player_info{player_id,player_name,dimension_name,dimension_id}` |
| `mc_player_stat_total{code,name,...}` | `mc_player_stat_total{stat_code,stat_name,...}` |
| `mc_teams_chunk_claims{...,dim_id,dim_name}` | `mc_team_chunk_claims{...,dimension_id,dimension_name}` |
| `mc_teams_chunk_loads{...,dim_id,dim_name}` | `mc_team_chunk_loads{...,dimension_id,dimension_name}` |
| `mc_teams_players{...,player_uuid,...}` | `mc_team_members{...,player_id,...}` |

New self-monitoring metrics: `mc_collector_refresh_duration_seconds{collector}`
and `mc_collector_staleness_seconds{collector}`.

Configuration
-------------

The mod configuration is located at *config/prometheus_exporter.cfg*.
It will be automatically generated upon server start if it does not already exist.
The default configuration can be seen in the example [examples/prometheus_exporter.cfg](examples/prometheus_exporter.cfg).

### Collection model and caching

Metrics that read Minecraft world state (entities, tile entities, chunks,
players, player statistics, teams) are sampled on the **server thread** during
the server tick and cached as an immutable snapshot. Prometheus scrapes only
read the latest snapshot, so a scrape never races the tick loop nor blocks the
server. The tick-timing metrics remain event-driven.

Each cached collector refreshes on its own interval, configurable in ticks
(20 ticks = 1 second). Cheap collectors refresh often; expensive ones rarely:

| Option | Default (ticks) | Approx. |
|--------|-----------------|---------|
| `chunks_interval_ticks` | 40 | 2s |
| `players_interval_ticks` | 40 | 2s |
| `entities_interval_ticks` | 100 | 5s |
| `tileentities_interval_ticks` | 100 | 5s |
| `player_statistics_interval_ticks` | 600 | 30s |
| `teams_interval_ticks` | 600 | 30s |

Set an interval near or above your Prometheus scrape interval for the heavy
collectors. Use `mc_collector_refresh_duration_seconds` to profile cost and
`mc_collector_staleness_seconds` to confirm freshness. Self-monitoring metrics
can be disabled with `self_metrics = false`.


Exporter
--------

A sample output from the exporter can be seen in the example [examples/output.txt](examples/output.txt).


Dashboards
----------

Known compatible Grafana dashboards are listed in [dashboards.md].
