package io.github.valajar.cryosat.space;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.valajar.cryosat.CryoSat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

/**
 * Server side datapack registry for one space data type, keyed by file id and rebuilt on every reload.
 */
public class SpaceData<T> extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();

    public static final SpaceData<Destination> DESTINATIONS = new SpaceData<>("destination", Destination.CODEC);
    public static final SpaceData<AsteroidClass> ASTEROIDS = new SpaceData<>("asteroid", AsteroidClass.CODEC);
    public static final SpaceData<SurveyTarget> SURVEY_TARGETS = new SpaceData<>("survey_target", SurveyTarget.CODEC);

    private final String name;
    private final Codec<T> codec;
    private Map<ResourceLocation, T> entries = Map.of();

    private SpaceData(String name, Codec<T> codec) {
        super(GSON, CryoSat.MODID + "/" + name);
        this.name = name;
        this.codec = codec;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, T> loaded = new HashMap<>();
        files.forEach((id, json) -> codec.parse(JsonOps.INSTANCE, json)
              .resultOrPartial(error -> CryoSat.LOGGER.error("Invalid {} {}: {}", name, id, error))
              .ifPresent(entry -> loaded.put(id, entry)));
        entries = Map.copyOf(loaded);
        CryoSat.LOGGER.info("Loaded {} {} definitions", entries.size(), name);
    }

    @Nullable
    public T get(ResourceLocation id) {
        return entries.get(id);
    }

    public Map<ResourceLocation, T> all() {
        return entries;
    }
}
