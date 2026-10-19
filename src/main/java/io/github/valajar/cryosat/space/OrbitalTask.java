package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

/**
 * A piece of work a satellite does for a ground block, such as one imaging pass for an Imaging Terminal. When it completes,
 * the satellite hands it to the requesting block while that block's chunk is loaded; until then it waits, never lost.
 *
 * @param requester   player who asked
 * @param terminal    block the result goes to
 * @param target      what to do, for example a survey target id
 * @param completesAt overworld game time the work is done
 */
public record OrbitalTask(UUID id, UUID requester, GlobalPos terminal, ResourceLocation target, long completesAt) {
    public static final Codec<OrbitalTask> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          UUIDUtil.CODEC.fieldOf("id").forGetter(OrbitalTask::id),
          UUIDUtil.CODEC.fieldOf("requester").forGetter(OrbitalTask::requester),
          GlobalPos.CODEC.fieldOf("terminal").forGetter(OrbitalTask::terminal),
          ResourceLocation.CODEC.fieldOf("target").forGetter(OrbitalTask::target),
          Codec.LONG.fieldOf("completes_at").forGetter(OrbitalTask::completesAt)
    ).apply(instance, OrbitalTask::new));
}
