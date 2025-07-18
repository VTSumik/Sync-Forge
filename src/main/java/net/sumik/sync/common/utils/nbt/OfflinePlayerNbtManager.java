package net.sumik.sync.common.utils.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public final class OfflinePlayerNbtManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String DATA_FILE_EXTENSION = ".dat";

    public static void editPlayerNbt(MinecraftServer server, UUID uuid, Consumer<CompoundTag> nbtEditor) {
        editPlayerNbt(server, uuid, nbt -> { nbtEditor.accept(nbt); return nbt; });
    }

    public static void editPlayerNbt(MinecraftServer server, UUID uuid, Function<CompoundTag, CompoundTag> nbtEditor) {
        File nbtPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(uuid.toString() + DATA_FILE_EXTENSION).toFile();
        try {
            if (!nbtPath.isFile()) {
                return;
            }

            CompoundTag nbt = NbtIo.readCompressed(nbtPath);
            nbt = nbtEditor.apply(nbt);
            NbtIo.writeCompressed(nbt, nbtPath);
        } catch (Throwable exception) {
            LOGGER.log(Level.ERROR, "Failed to edit player's nbt.", exception);
        }
    }

    private OfflinePlayerNbtManager() {
    }
}