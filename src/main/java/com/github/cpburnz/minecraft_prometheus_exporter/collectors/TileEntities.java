package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import com.github.cpburnz.minecraft_prometheus_exporter.ExporterConfig;

import gnu.trove.map.hash.TObjectIntHashMap;
import io.prometheus.client.GaugeMetricFamily;

public class TileEntities extends BaseCollector {

    public TileEntities(MinecraftServer mc_server) {
        super(mc_server);
    }

    private static GaugeMetricFamily newTEMetric() {
        return new GaugeMetricFamily(
            "mc_dimension_tileentities",
            "The number of loaded ticking tileentities in a dim.",
            Arrays.asList("id", "name"));
    }

    private static GaugeMetricFamily newTEDetailedMetric() {
        return new GaugeMetricFamily(
            "mc_dimension_tileentities_detailed",
            "The number of loaded ticking tileentities in a dim per type.",
            Arrays.asList("dim_id", "dim", "te_class", "te_name"));
    }

    @Override
    public List<MetricFamilySamples> collect() {
        GaugeMetricFamily metric;
        if (ExporterConfig.collector.tileentities_details) {
            TObjectIntHashMap<EntityKey> te_totals = new TObjectIntHashMap<>();
            metric = newTEDetailedMetric();
            for (WorldServer world : DimensionManager.getWorlds()) {
                int dim_id = world.provider.dimensionId;
                String dim = world.provider.getDimensionName();

                List<TileEntity> loaded_entities = world.loadedTileEntityList;
                if (loaded_entities.size() == 0) continue;
                for (TileEntity te : loaded_entities) {
                    Class<? extends TileEntity> teClass = te.getClass();
                    String teClassName = teClass.getName();
                    String teName = (String) TileEntity.classToNameMap.get(teClass);
                    if (teName != null) {
                        EntityKey te_key = new EntityKey(dim, dim_id, teClassName, teName);
                        te_totals.adjustOrPutValue(te_key, 1, 1);
                    }
                }
            }

            for (EntityKey entity_key : te_totals.keySet()) {
                double total = te_totals.get(entity_key);
                String dim_id_str = Integer.toString(entity_key.dim_id);
                metric.addMetric(Arrays.asList(dim_id_str, entity_key.dim, entity_key.clazz, entity_key.name), total);
            }
        } else {
            metric = newTEMetric();
            for (WorldServer world : DimensionManager.getWorlds()) {
                int loaded = world.loadedTileEntityList.size();
                if (loaded == 0) continue;
                metric.addMetric(
                    Arrays.asList(Integer.toString(world.provider.dimensionId), world.provider.getDimensionName()),
                    loaded);
            }
        }

        return Arrays.asList(metric);
    }

    static final List<MetricFamilySamples> desc = Collections
        .singletonList(ExporterConfig.collector.tileentities_details ? newTEDetailedMetric() : newTEMetric());

    @Override
    public List<MetricFamilySamples> describe() {
        return desc;
    }

    private static class EntityKey {

        public final String dim;
        public final int dim_id;
        private final String clazz;
        public final String name;

        /**
         * Construct the instance.
         *
         * @param dim    The dimension name.
         * @param dim_id The dimension id.
         * @param name   The entity type.
         */
        public EntityKey(String dim, int dim_id, String clazz, String name) {
            this.dim = dim;
            this.dim_id = dim_id;
            this.clazz = clazz;
            this.name = name;
        }

        /**
         * Determine whether the other object is equal to this one.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof EntityKey other) {
                return (Objects.equals(this.dim, other.dim) && this.dim_id == other.dim_id
                    && Objects.equals(this.clazz, other.clazz)
                    && Objects.equals(this.name, other.name));
            }
            return false;
        }

        /**
         * Get a hash code value for the object.
         */
        @Override
        public int hashCode() {
            return Objects.hash(this.dim, this.dim_id, this.clazz, this.name);
        }
    }
}
