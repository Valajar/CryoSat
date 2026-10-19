package io.github.valajar.cryosat.space.survey;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.common.inventory.container.sync.list.SyncableList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * Mekanism container sync for a list of records written with a stream codec. The container polls the getter every tick and
 * sends the whole list when its hash changes, plus once when the screen opens.
 */
public class SyncableCodecList<T> extends SyncableList<T> {
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> codec;

    public SyncableCodecList(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Supplier<@NotNull List<T>> getter, Consumer<@NotNull List<T>> setter) {
        super(getter, setter);
        this.codec = codec;
    }

    @Override
    protected List<T> deserializeList(RegistryFriendlyByteBuf buffer) {
        return buffer.readList(buf -> codec.decode(buffer));
    }

    @Override
    protected void serializeListElement(RegistryFriendlyByteBuf buffer, T element) {
        codec.encode(buffer, element);
    }
}
