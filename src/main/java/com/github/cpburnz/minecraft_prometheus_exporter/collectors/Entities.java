package com.github.cpburnz.minecraft_prometheus_exporter.collectors;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import gnu.trove.map.hash.TObjectIntHashMap;
import io.prometheus.client.GaugeMetricFamily;

public class Entities extends BaseCollector {

    private static GaugeMetricFamily newMetric() {
        return new GaugeMetricFamily(
            "mc_entities_total",
            "The number of entities in each dimension by type.",
            Arrays.asList("dim", "dim_id", "id", "type"));
    }

    public Entities(MinecraftServer mc_server) {
        super(mc_server);
    }

    @Override
    public List<MetricFamilySamples> collect() {
        TObjectIntHashMap<EntityKey> entity_totals = new TObjectIntHashMap<>();
        for (WorldServer world : mc_server.worldServers) {
            // Get world info.
            int dim_id = world.provider.dimensionId;
            String dim = world.provider.getDimensionName();

            // Get entity info.
            List loaded_entities = world.loadedEntityList;
            for (int i = loaded_entities.size(); i-- > 0;) {
                Object entityObj = loaded_entities.get(i);
                if (entityObj instanceof Entity entity && !(entityObj instanceof EntityPlayer)) {
                    // Get entity type.
                    String entity_type = EntityList.getEntityString(entity);
                    if (entity_type == null && entity instanceof IMob) {
                        entity_type = entity.getClass()
                            .getName();
                    }

                    if (entity_type != null) {
                        int entity_id = EntityList.getEntityID(entity);
                        EntityKey entity_key = new EntityKey(dim, dim_id, entity_id, entity_type);
                        entity_totals.adjustOrPutValue(entity_key, 1, 1);
                    }
                }
            }
        }

        // Record metrics.
        GaugeMetricFamily metric = newMetric();
        for (EntityKey entity_key : entity_totals.keySet()) {
            double total = entity_totals.get(entity_key);
            String dim_id_str = Integer.toString(entity_key.dim_id);
            String id_str = Integer.toString(entity_key.id);
            metric.addMetric(Arrays.asList(entity_key.dim, dim_id_str, id_str, entity_key.type), total);
        }

        return Arrays.asList(metric);
    }

    static final List<MetricFamilySamples> desc = Collections.singletonList(newMetric());

    @Override
    public List<MetricFamilySamples> describe() {
        return desc;
    }

    private static class EntityKey {

        public final String dim;
        public final int dim_id;
        public final int id;
        public final String type;

        /**
         * Construct the instance.
         *
         * @param dim    The dimension name.
         * @param dim_id The dimension id.
         * @param id     The entity id.
         * @param type   The entity type.
         */
        public EntityKey(String dim, int dim_id, int id, String type) {
            this.dim = dim;
            this.dim_id = dim_id;
            this.id = id;
            this.type = type;
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
                    && this.id == other.id
                    && Objects.equals(this.type, other.type));
            }
            return false;
        }

        /**
         * Get a hash code value for the object.
         */
        @Override
        public int hashCode() {
            return Objects.hash(this.dim, this.dim_id, this.id, this.type);
        }
    }
}
