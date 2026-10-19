package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Survey results stored on a Survey Locator, and which one it points to. Lives on the item, so locators can be handed over.
 *
 * @param targets  stored results, at most {@link #MAX_TARGETS}
 * @param selected index of the target the needle and beam show
 */
public record LocatorData(List<Entry> targets, int selected) {
    public static final int MAX_TARGETS = 8;
    public static final LocatorData EMPTY = new LocatorData(List.of(), 0);

    public static final Codec<LocatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Entry.CODEC.listOf(0, MAX_TARGETS).fieldOf("targets").forGetter(LocatorData::targets),
          Codec.INT.optionalFieldOf("selected", 0).forGetter(LocatorData::selected)
    ).apply(instance, LocatorData::new));
    public static final StreamCodec<ByteBuf, LocatorData> STREAM_CODEC = StreamCodec.composite(
          Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TARGETS)), LocatorData::targets,
          ByteBufCodecs.VAR_INT, LocatorData::selected,
          LocatorData::new
    );

    @Nullable
    public Entry current() {
        return targets.isEmpty() ? null : targets.get(Math.floorMod(selected, targets.size()));
    }

    /**
     * One stored result.
     *
     * @param pos         reported location (exact, or the centre of the search circle)
     * @param uncertainty radius of the search circle in blocks, 0 when exact
     * @param target      survey target id
     */
    public record Entry(GlobalPos pos, int uncertainty, ResourceLocation target) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
              GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
              Codec.INT.optionalFieldOf("uncertainty", 0).forGetter(Entry::uncertainty),
              ResourceLocation.CODEC.fieldOf("target").forGetter(Entry::target)
        ).apply(instance, Entry::new));
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
              GlobalPos.STREAM_CODEC, Entry::pos,
              ByteBufCodecs.VAR_INT, Entry::uncertainty,
              ResourceLocation.STREAM_CODEC, Entry::target,
              Entry::new
        );
    }
}
