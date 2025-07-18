package net.sumik.sync.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.sumik.sync.Sync;
import net.sumik.sync.common.utils.reflect.Activator;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Sync.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SyncCommands {
    private static final Set<Command> COMMANDS = new HashSet<>();

    static {
        register(GhostShellsCommand.class);
    }

    public static void init() {
    }

    private static <T extends Command> void register(Class<T> type) {
        COMMANDS.add(Activator.createInstance(type));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> commandBuilder = LiteralArgumentBuilder.literal(Sync.MOD_ID);
        commandBuilder.requires(source -> COMMANDS.stream().anyMatch(c -> c.hasPermissions(source)));

        for (Command commandInfo : COMMANDS) {
            LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(commandInfo.getName());
            command.requires(commandInfo::hasPermissions);
            commandInfo.build(command);
            commandBuilder.then(command);
        }

        dispatcher.register(commandBuilder);
    }
}