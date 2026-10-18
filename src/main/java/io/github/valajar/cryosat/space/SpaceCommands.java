package io.github.valajar.cryosat.space;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Operator commands for testing missions: {@code /cryosat orbit skip} makes every orbital asset reach its next arrival
 * or batch right away.
 */
public final class SpaceCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cryosat")
              .then(Commands.literal("orbit")
                    .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("skip").executes(SpaceCommands::skip))));
    }

    private static int skip(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        OrbitalRegistry registry = OrbitalRegistry.get(server);
        long now = server.overworld().getGameTime();
        int skipped = 0;
        for (OrbitalAsset asset : registry.all()) {
            if (asset.state != OrbitalAsset.State.RETIRED) {
                asset.nextEventAt = now;
                skipped++;
            }
        }
        registry.tick(server);
        int count = skipped;
        context.getSource().sendSuccess(() -> Component.literal("Skipped ahead " + count + " orbital assets"), true);
        return skipped;
    }

    private SpaceCommands() {
    }
}
