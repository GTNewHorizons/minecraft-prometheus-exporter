package com.github.cpburnz.minecraft_prometheus_exporter;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * The Config class defines the mod config. This is used to load and generate
 * the "prometheus_exporter.cfg" config file.
 */
@Config(modid = PrometheusExporterMod.MODID, category = "")
public class ExporterConfig {

    public static Collector collector = new Collector();
    public static Web web = new Web();

    public static class Collector {

        @Config.DefaultBoolean(true)
        @Config.Comment("Whether collecting metrics about the JVM process is enabled")
        public boolean jwm_collector;

        @Config.DefaultBoolean(true)
        @Config.Comment("Whether entity metrics should be collected")
        public boolean entities;

        @Config.DefaultBoolean(true)
        @Config.Comment("Whether ticking tile entity metrics should be collected")
        public boolean tileentities;

        @Config.DefaultBoolean(false)
        @Config.Comment("Enables logging per tile entity type per dim; replaces tileentities")
        public boolean tileentities_details;

        @Config.DefaultBoolean(true)
        @Config.Comment("Whether ticks metrics should be collected")
        public boolean ticks;

        @Config.DefaultBoolean(true)
        @Config.Comment("Whether chunk metrics should be collected")
        public boolean chunks;

        @Config.DefaultBoolean(true)
        @Config.Comment("Whether player metrics should be collected")
        public boolean players;

        @Config.DefaultBoolean(true)
        @Config.Comment("Whether player statistics should be collected")
        public boolean player_statistics;

        @Config.DefaultBoolean(true)
        @Config.Comment("Whether to collect metrics on ServerUtilities teams")
        public boolean teams;

        @Config.RangeInt(min = 0, max = 4)
        @Config.DefaultInt(4)
        @Config.Comment("What permission level should be required to restart the exporter")
        public int command_permission_level;

        @Config.DefaultEnum("LOG")
        @Config.Comment("""
            Configure how to handle dimension (world) tick errors. Some mods
            handle the tick events for their custom dimensions, and may not
            reliably start and stop ticks as expected.

              IGNORE: Ignore tick errors. If a mod really botches tick
            events, it could emit up to 20 log statements per second for
            each dimension. This would cause large ballooning of the
            "logs/debug.txt" file. Use this setting, or figure out how to
            filter out DEBUG messages for
            "com.github.cpburnz.minecraft_prometheus_exporter.collectors.Ticks/"
            in "log4j2.xml".

              LOG: Log tick errors. This is the new default.

              STRICT: Raise an exception on tick error. This will crash the
            server if an error occurs.
            """)
        public TickErrorPolicy collector_mc_dimension_tick_errors;
    }

    public static class Web {

        @Config.DefaultString("0.0.0.0")
        @Config.Comment("The default address to listen on. Default is everywhere")
        public String listen_address;

        @Config.DefaultInt(19565)
        @Config.RangeInt(min = 0, max = 65535)
        @Config.Comment({ "The default TCP port ot use.",
            "It was derived from the Minecraft port (25565) and the Prometheus exporter ports (9100+)" })
        public int listen_port;
    }

    /**
     * The TickErrorPolicy enum defines how to handle dimension (world) tick event
     * errors.
     */
    public enum TickErrorPolicy {
        /**
         * When a tick error occurs, ignore the error.
         */
        IGNORE,

        /**
         * When a tick error occurs, log the error.
         */
        LOG,

        /**
         * When a tick error occurs, raise an IllegalStateException.
         */
        STRICT
    }
}
