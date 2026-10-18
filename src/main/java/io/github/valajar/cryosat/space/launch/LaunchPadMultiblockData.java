package io.github.valajar.cryosat.space.launch;

import com.mojang.datafixers.util.Either;
import io.github.valajar.cryosat.CryoSat;
import io.github.valajar.cryosat.client.rocket.RocketRenderer;
import io.github.valajar.cryosat.registries.CryoDataComponents;
import io.github.valajar.cryosat.space.AsteroidClass;
import io.github.valajar.cryosat.space.MissionProblem;
import io.github.valajar.cryosat.space.OrbitalAsset;
import io.github.valajar.cryosat.space.OrbitalRegistry;
import io.github.valajar.cryosat.space.RocketDesign;
import io.github.valajar.cryosat.space.RocketPartItem;
import io.github.valajar.cryosat.space.RocketPartItem.Kind;
import io.github.valajar.cryosat.space.RocketTier;
import io.github.valajar.cryosat.space.RocketTier.Fuel;
import io.github.valajar.cryosat.space.SpaceData;
import io.github.valajar.cryosat.space.SpaceLang;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.inventory.slot.BasicInventorySlot;
import mekanism.common.lib.multiblock.MultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Shared state of a formed Launch Pad: the Rocket Assembly slots, the fuel the pad holds, the assembled rocket and the
 * launch in progress. Contents survive the pad being broken and rebuilt through {@link LaunchPadCache}, like any
 * Mekanism multiblock.
 * <p>
 * The assembled {@link RocketDesign}, the destination and the launch start time go to clients in the controller's update
 * tag (the controller is the only part that can be the multiblock master), so every player sees the rocket.
 */
public class LaunchPadMultiblockData extends MultiblockData {
    private static final List<Fuel> FUELS = Arrays.stream(RocketTier.values()).flatMap(tier -> tier.fuel().stream()).toList();
    private static final Set<ResourceLocation> SOLID_FUELS = fuelIds(Fuel::solidItem);
    private static final Set<ResourceLocation> OXIDISERS = fuelIds(Fuel::oxidiser);
    private static final Set<ResourceLocation> PROPELLANTS = fuelIds(Fuel::propellant);
    /** Enough for the hungriest tier's launch. */
    public static final long OXIDISER_CAPACITY = FUELS.stream().mapToLong(Fuel::oxidiserAmount).max().orElse(0);
    public static final long PROPELLANT_CAPACITY = FUELS.stream().mapToLong(Fuel::propellantAmount).max().orElse(0);
    /** The launch column is rechecked this often, and right before every launch. */
    public static final int COLUMN_CHECK_INTERVAL = 20;

    private static Set<ResourceLocation> fuelIds(Function<Fuel, Optional<ResourceLocation>> getter) {
        return FUELS.stream().flatMap(fuel -> getter.apply(fuel).stream()).collect(Collectors.toUnmodifiableSet());
    }

    // GUI positions are the item positions (the slot background starts one pixel up and left)
    public final IInventorySlot engineSlot;
    public final IInventorySlot fuelTankSlot;
    public final IInventorySlot guidanceSlot;
    public final IInventorySlot payloadSlot;
    public final IInventorySlot uplinkSlot;
    public final IInventorySlot solidFuelSlot;

    @ContainerSync
    public IChemicalTank oxidiserTank;
    @ContainerSync
    public IChemicalTank propellantTank;
    @ContainerSync
    public LaunchStatus status = LaunchStatus.ASSEMBLY_PROBLEM;
    /** Ordinal of the {@link MissionProblem} behind the status, or -1. */
    @ContainerSync
    public int problem = MissionProblem.MISSING_ENGINE.ordinal();

    @Nullable
    private RocketDesign design;
    /** The parts the rocket was assembled from, given back on disassembly. */
    private List<ItemStack> parts = List.of();
    private ResourceLocation destination = AsteroidClass.ASTEROID_BELT;
    /** Game time the current launch started, or -1. */
    private long launchStartedAt = -1;

    /** Set by the validator; server only. */
    @Nullable
    public BlockPos controller;
    public int fuelPorts;
    private boolean columnClear = true;
    private long lastColumnCheck = Long.MIN_VALUE;
    private boolean needsSync;

    public LaunchPadMultiblockData(BlockEntity tile) {
        super(tile);
        inventorySlots.add(engineSlot = partSlot(Kind.ENGINE, 49, 17));
        inventorySlots.add(fuelTankSlot = partSlot(Kind.FUEL_TANK, 49, 35));
        inventorySlots.add(guidanceSlot = partSlot(Kind.GUIDANCE_COMPUTER, 49, 53));
        inventorySlots.add(payloadSlot = partSlot(Kind.PAYLOAD, 49, 71));
        inventorySlots.add(uplinkSlot = BasicInventorySlot.at(LaunchPadMultiblockData::notExternal, LaunchPadMultiblockData::notExternal,
              stack -> stack.has(CryoDataComponents.UPLINK_TARGET), this, 49, 97));
        // Pipes may fill the grain slot through the fuel ports, but never empty it
        inventorySlots.add(solidFuelSlot = BasicInventorySlot.at(LaunchPadMultiblockData::notExternal, (stack, automation) -> true,
              stack -> SOLID_FUELS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem())), this, 17, 83));
        chemicalTanks.add(oxidiserTank = BasicChemicalTank.createModern(OXIDISER_CAPACITY, (stack, automation) -> true, formedBiPred(),
              stack -> acceptsChemical(stack, OXIDISERS, Fuel::oxidiser), this));
        chemicalTanks.add(propellantTank = BasicChemicalTank.createModern(PROPELLANT_CAPACITY, (stack, automation) -> true, formedBiPred(),
              stack -> acceptsChemical(stack, PROPELLANTS, Fuel::propellant), this));
    }

    private BasicInventorySlot partSlot(Kind kind, int x, int y) {
        return BasicInventorySlot.at(LaunchPadMultiblockData::notExternal, LaunchPadMultiblockData::notExternal, stack -> RocketPartItem.is(stack, kind), this, x, y);
    }

    private static boolean notExternal(ItemStack stack, AutomationType automation) {
        return automation != AutomationType.EXTERNAL;
    }

    /** Any tier's chemical while no rocket is assembled, otherwise only what the assembled rocket burns. */
    private boolean acceptsChemical(ChemicalStack stack, Set<ResourceLocation> any, Function<Fuel, Optional<ResourceLocation>> getter) {
        Fuel fuel = design == null ? null : design.tier().fuel().orElse(null);
        if (fuel != null && !fuel.isSolid()) {
            return getter.apply(fuel).map(id -> stack.getChemicalHolder().is(id)).orElse(false);
        }
        return any.stream().anyMatch(id -> stack.getChemicalHolder().is(id));
    }

    // --- queries ------------------------------------------------------------------------------

    @Nullable
    public RocketDesign getDesign() {
        return design;
    }

    public List<ItemStack> getParts() {
        return parts;
    }

    public ResourceLocation getDestination() {
        return destination;
    }

    public boolean isLaunching() {
        return launchStartedAt >= 0;
    }

    public long getLaunchStartedAt() {
        return launchStartedAt;
    }

    @Nullable
    public MissionProblem getProblem() {
        return problem < 0 || problem >= MissionProblem.values().length ? null : MissionProblem.values()[problem];
    }

    /** Top centre of the pad, where the rocket's nozzle sits. */
    public BlockPos getCentre() {
        return getMinPos().offset(LaunchPadValidator.SIZE / 2, 0, LaunchPadValidator.SIZE / 2);
    }

    public boolean hasFuel() {
        if (design == null) {
            return false;
        }
        Fuel fuel = design.tier().fuel().orElse(null);
        if (fuel == null) {
            return false;
        } else if (fuel.isSolid()) {
            ItemStack grains = solidFuelSlot.getStack();
            return fuel.solidItem().map(id -> BuiltInRegistries.ITEM.getKey(grains.getItem()).equals(id)).orElse(false) && grains.getCount() >= fuel.solidCount();
        }
        return hasChemical(oxidiserTank, fuel.oxidiser(), fuel.oxidiserAmount()) && hasChemical(propellantTank, fuel.propellant(), fuel.propellantAmount());
    }

    private static boolean hasChemical(IChemicalTank tank, Optional<ResourceLocation> id, long amount) {
        return id.isPresent() && !tank.isEmpty() && tank.getStack().getChemicalHolder().is(id.get()) && tank.getStored() >= amount;
    }

    /** Whether the column the rocket stands in and lifts off through is free of blocks. */
    public boolean isColumnClear(Level level) {
        BlockPos centre = getCentre();
        // Compile time constants, inlined: this never loads the client only renderer class on a server
        int radius = (int) RocketRenderer.RADIUS;
        int height = (int) Math.ceil(RocketRenderer.HEIGHT);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 1; y <= height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    pos.setWithOffset(centre, x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // --- ticking ------------------------------------------------------------------------------

    @Override
    public boolean tick(Level world) {
        boolean needsPacket = super.tick(world);
        long now = world.getGameTime();
        if (isLaunching() && now - launchStartedAt >= LaunchSequence.DURATION_TICKS) {
            // The rocket is out of sight: clear the pad
            design = null;
            parts = List.of();
            launchStartedAt = -1;
            changed();
        }
        if (now - lastColumnCheck >= COLUMN_CHECK_INTERVAL) {
            columnClear = isColumnClear(world);
            lastColumnCheck = now;
        }
        updateStatus();
        if (needsSync) {
            needsSync = false;
            needsPacket = true;
        }
        return needsPacket;
    }

    private void changed() {
        needsSync = true;
        markDirty();
    }

    private void updateStatus() {
        MissionProblem found = null;
        if (isLaunching()) {
            status = LaunchStatus.LAUNCHING;
        } else if (design == null) {
            found = RocketDesign.assemble(engineSlot.getStack(), fuelTankSlot.getStack(), guidanceSlot.getStack(), payloadSlot.getStack()).right().orElse(null);
            status = found == null ? LaunchStatus.READY_TO_ASSEMBLE : LaunchStatus.ASSEMBLY_PROBLEM;
        } else if ((found = OrbitalRegistry.check(design, destination)) != null) {
            status = LaunchStatus.MISSION_PROBLEM;
        } else if (!hasFuel()) {
            status = LaunchStatus.NEEDS_FUEL;
        } else if (!columnClear) {
            status = LaunchStatus.OBSTRUCTED;
        } else {
            status = LaunchStatus.READY;
        }
        problem = found == null ? -1 : found.ordinal();
    }

    // --- actions (server) ----------------------------------------------------------------------

    /**
     * Consumes one of each part into a rocket.
     *
     * @return the problem that stopped it, or {@code null} once assembled
     */
    @Nullable
    public MissionProblem assemble() {
        if (design != null || isLaunching()) {
            return null;
        }
        Either<RocketDesign, MissionProblem> result = RocketDesign.assemble(engineSlot.getStack(), fuelTankSlot.getStack(), guidanceSlot.getStack(),
              payloadSlot.getStack());
        if (result.right().isPresent()) {
            return result.right().get();
        }
        List<ItemStack> used = new ArrayList<>();
        for (IInventorySlot slot : List.of(engineSlot, fuelTankSlot, guidanceSlot, payloadSlot)) {
            used.add(slot.extractItem(1, Action.EXECUTE, AutomationType.INTERNAL));
        }
        design = result.left().orElseThrow();
        parts = List.copyOf(used);
        if (OrbitalRegistry.check(design, destination) != null) {
            usableDestinations().stream().findFirst().ifPresent(id -> destination = id);
        }
        updateStatus();
        changed();
        return null;
    }

    /**
     * Takes the rocket apart again. Parts go back into their slots, then into the player's inventory, then onto the pad.
     *
     * @return whether there was a rocket to take apart
     */
    public boolean disassemble(@Nullable Player player) {
        if (design == null || isLaunching()) {
            return false;
        }
        List<IInventorySlot> slots = List.of(engineSlot, fuelTankSlot, guidanceSlot, payloadSlot);
        for (int i = 0; i < parts.size(); i++) {
            ItemStack rest = i < slots.size() ? slots.get(i).insertItem(parts.get(i).copy(), Action.EXECUTE, AutomationType.INTERNAL) : parts.get(i).copy();
            if (!rest.isEmpty()) {
                if (player != null) {
                    player.getInventory().placeItemBackInInventory(rest);
                } else {
                    Block.popResource(getLevel(), getCentre().above(), rest);
                }
            }
        }
        design = null;
        parts = List.of();
        updateStatus();
        changed();
        return true;
    }

    /** Destinations the assembled rocket can go to (all of them while none is assembled), by id. */
    public List<ResourceLocation> usableDestinations() {
        return SpaceData.DESTINATIONS.all().keySet().stream()
              .filter(id -> design == null || OrbitalRegistry.check(design, id) == null)
              .sorted()
              .toList();
    }

    public void cycleDestination(int step) {
        List<ResourceLocation> ids = usableDestinations();
        if (ids.isEmpty()) {
            return;
        }
        int index = ids.indexOf(destination);
        ResourceLocation next = index < 0 ? ids.getFirst() : ids.get(Math.floorMod(index + step, ids.size()));
        if (!next.equals(destination)) {
            destination = next;
            updateStatus();
            changed();
        }
    }

    /**
     * Launches the assembled rocket: burns the fuel, puts the payload into orbit and starts the launch sequence.
     *
     * @param owner    who the payload belongs to
     * @param launcher name for the announcement, only asked for once the rocket is launched
     *
     * @return {@link LaunchStatus#LAUNCHING} once launched, otherwise why it did not launch
     */
    public LaunchStatus launch(ServerLevel level, @Nullable UUID owner, Supplier<Component> launcher) {
        if (isLaunching()) {
            return LaunchStatus.LAUNCHING;
        }
        columnClear = isColumnClear(level);
        lastColumnCheck = level.getGameTime();
        updateStatus();
        if (status != LaunchStatus.READY) {
            return status;
        } else if (owner == null) {
            return LaunchStatus.NO_OWNER;
        }
        RocketDesign rocket = design;
        GlobalPos receiver = uplinkSlot.getStack().get(CryoDataComponents.UPLINK_TARGET);
        Either<OrbitalAsset, MissionProblem> result = OrbitalRegistry.get(level.getServer()).launch(level.getServer(), owner, rocket, destination, receiver);
        if (result.right().isPresent()) {
            problem = result.right().get().ordinal();
            status = LaunchStatus.MISSION_PROBLEM;
            return status;
        }
        burnFuel(rocket.tier().fuel().orElseThrow());
        launchStartedAt = level.getGameTime();
        updateStatus();
        changed();

        BlockPos centre = getCentre();
        level.playSound(null, centre.getX() + 0.5, centre.getY() + 1, centre.getZ() + 0.5, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4, 0.5F);
        level.playSound(null, centre.getX() + 0.5, centre.getY() + 1, centre.getZ() + 0.5, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 6, 0.5F);
        OrbitalAsset asset = result.left().orElseThrow();
        level.getServer().getPlayerList().broadcastSystemMessage(SpaceLang.LAUNCH_ANNOUNCEMENT.translate(launcher.get(), asset.displayName()), false);
        CryoSat.LOGGER.debug("Launched {} from {} to {}", asset.id, centre, destination);
        return LaunchStatus.LAUNCHING;
    }

    private void burnFuel(Fuel fuel) {
        if (fuel.isSolid()) {
            solidFuelSlot.shrinkStack(fuel.solidCount(), Action.EXECUTE);
        } else {
            oxidiserTank.shrinkStack(fuel.oxidiserAmount(), Action.EXECUTE);
            propellantTank.shrinkStack(fuel.propellantAmount(), Action.EXECUTE);
        }
    }

    // --- persistence and sync -----------------------------------------------------------------

    void loadLaunchState(@Nullable RocketDesign design, List<ItemStack> parts, ResourceLocation destination, long launchStartedAt) {
        this.design = design;
        this.parts = List.copyOf(parts);
        this.destination = destination;
        this.launchStartedAt = launchStartedAt;
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.writeUpdateTag(tag, provider);
        if (design != null) {
            RocketDesign.CODEC.encodeStart(NbtOps.INSTANCE, design).ifSuccess(encoded -> tag.put("design", encoded));
        }
        tag.putString("destination", destination.toString());
        tag.putLong("launch_started_at", launchStartedAt);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        super.readUpdateTag(tag, provider);
        design = tag.contains("design") ? RocketDesign.CODEC.parse(NbtOps.INSTANCE, tag.get("design")).result().orElse(null) : null;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("destination"));
        if (id != null) {
            destination = id;
        }
        launchStartedAt = tag.contains("launch_started_at") ? tag.getLong("launch_started_at") : -1;
    }
}
