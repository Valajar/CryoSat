package io.github.valajar.cryosat.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ExtraCodecs.TagOrElementLocation;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Something an Imaging Terminal can search for. Loaded from {@code data/<namespace>/cryosat/survey_target/*.json}; the file id
 * is the target id and names it through the lang key {@code survey_target.<namespace>.<path>}.
 *
 * @param structures a structure tag ({@code #minecraft:village}) or a single structure id, what {@code /locate} takes
 * @param deepScan   underground target: only searchable with a SQUID Array next to the terminal
 * @param icon       item shown for it in the terminal
 */
public record SurveyTarget(TagOrElementLocation structures, boolean deepScan, ResourceLocation icon) {
    public static final Codec<SurveyTarget> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("structures").forGetter(SurveyTarget::structures),
          Codec.BOOL.optionalFieldOf("deep_scan", false).forGetter(SurveyTarget::deepScan),
          ResourceLocation.CODEC.fieldOf("icon").forGetter(SurveyTarget::icon)
    ).apply(instance, SurveyTarget::new));

    /** @return the structures to search for in this world, empty if the tag or structure does not exist here */
    public Optional<HolderSet<Structure>> resolve(RegistryAccess access) {
        var registry = access.registryOrThrow(Registries.STRUCTURE);
        if (structures.tag()) {
            return registry.getTag(TagKey.create(Registries.STRUCTURE, structures.id())).map(tag -> tag);
        }
        return registry.getHolder(ResourceKey.create(Registries.STRUCTURE, structures.id())).map(holder -> HolderSet.direct((Holder<Structure>) holder));
    }
}
