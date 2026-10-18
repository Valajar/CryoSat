package io.github.valajar.cryosat.space.mission;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.common.inventory.container.sync.list.SyncableList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Mekanism container sync for the Mission Control list. The container polls the getter every tick and sends the whole
 * list when its hash changes, plus once when the screen opens.
 */
public class SyncableMissionList extends SyncableList<MissionSummary> {

    public static SyncableMissionList create(Supplier<@NotNull List<MissionSummary>> getter, Consumer<@NotNull List<MissionSummary>> setter) {
        return new SyncableMissionList(getter, setter);
    }

    private SyncableMissionList(Supplier<@NotNull List<MissionSummary>> getter, Consumer<@NotNull List<MissionSummary>> setter) {
        super(getter, setter);
    }

    @Override
    protected List<MissionSummary> deserializeList(RegistryFriendlyByteBuf buffer) {
        return buffer.readList(buf -> MissionSummary.STREAM_CODEC.decode(buffer));
    }

    @Override
    protected void serializeListElement(RegistryFriendlyByteBuf buffer, MissionSummary element) {
        MissionSummary.STREAM_CODEC.encode(buffer, element);
    }
}
