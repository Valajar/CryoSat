package io.github.valajar.cryosat.atmosphere;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.valajar.cryosat.CryoSat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Server side registry of {@link Atmosphere}s, rebuilt on every datapack reload.
 */
public class AtmosphereManager extends SimpleJsonResourceReloadListener {
    // Must be initialised before INSTANCE, whose constructor uses it
    private static final Gson GSON = new GsonBuilder().create();
    public static final AtmosphereManager INSTANCE = new AtmosphereManager();

    private Map<ResourceKey<Level>, Atmosphere> atmospheres = Map.of();

    private AtmosphereManager() {
        super(GSON, CryoSat.MODID + "/atmosphere");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceKey<Level>, Atmosphere> loaded = new HashMap<>();
        files.forEach((id, json) -> Atmosphere.CODEC.parse(JsonOps.INSTANCE, json)
              .resultOrPartial(error -> CryoSat.LOGGER.error("Invalid atmosphere {}: {}", id, error))
              .ifPresent(atmosphere -> {
                  Atmosphere previous = loaded.put(atmosphere.dimension(), atmosphere);
                  if (previous != null) {
                      CryoSat.LOGGER.warn("Atmosphere {} replaces another definition for {}", id, atmosphere.dimension().location());
                  }
              }));
        atmospheres = Map.copyOf(loaded);
        CryoSat.LOGGER.info("Loaded {} atmospheres", atmospheres.size());
    }

    /**
     * @return the atmosphere of a dimension, or {@code null} if it has none (or it is disabled with a rate of 0)
     */
    @Nullable
    public Atmosphere get(ResourceKey<Level> dimension) {
        Atmosphere atmosphere = atmospheres.get(dimension);
        return atmosphere == null || atmosphere.rate() <= 0 ? null : atmosphere;
    }
}
