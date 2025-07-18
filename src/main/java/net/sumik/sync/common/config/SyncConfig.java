package net.sumik.sync.common.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.sumik.sync.Sync;
import net.sumik.sync.api.shell.ShellPriority;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Sync.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SyncConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // General settings
    private static final ForgeConfigSpec.BooleanValue ENABLE_INSTANT_SHELL_CONSTRUCTION;
    private static final ForgeConfigSpec.BooleanValue WARN_PLAYER_INSTEAD_OF_KILLING;

    // Damage settings
    private static final ForgeConfigSpec.DoubleValue FINGERSTICK_DAMAGE;
    private static final ForgeConfigSpec.DoubleValue HARDCORE_FINGERSTICK_DAMAGE;

    // Energy settings
    private static final ForgeConfigSpec.LongValue SHELL_CONSTRUCTOR_CAPACITY;
    private static final ForgeConfigSpec.LongValue SHELL_STORAGE_CAPACITY;
    private static final ForgeConfigSpec.LongValue SHELL_STORAGE_CONSUMPTION;

    // Shell storage settings
    private static final ForgeConfigSpec.BooleanValue SHELL_STORAGE_ACCEPTS_REDSTONE;
    private static final ForgeConfigSpec.IntValue SHELL_STORAGE_MAX_UNPOWERED_LIFESPAN;

    // Energy map
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ENERGY_MAP;

    // Sync priority
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SYNC_PRIORITY;

    // Tool settings
    private static final ForgeConfigSpec.ConfigValue<String> WRENCH;

    // Misc settings
    private static final ForgeConfigSpec.BooleanValue UPDATE_TRANSLATIONS_AUTOMATICALLY;
    private static final ForgeConfigSpec.BooleanValue PRESERVE_ORIGINS;

    // Technoblade easter egg
    private static final ForgeConfigSpec.BooleanValue ENABLE_TECHNOBLADE_EASTER_EGG;
    private static final ForgeConfigSpec.BooleanValue RENDER_TECHNOBLADE_CAPE;
    private static final ForgeConfigSpec.BooleanValue ALLOW_TECHNOBLADE_ANNOUNCEMENTS;
    private static final ForgeConfigSpec.BooleanValue ALLOW_TECHNOBLADE_QUOTES;
    private static final ForgeConfigSpec.IntValue TECHNOBLADE_QUOTE_DELAY;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> TECHNOBLADE_UUIDS;

    // Cached values
    private static List<EnergyMapEntry> cachedEnergyMap = null;
    private static List<ShellPriorityEntry> cachedSyncPriority = null;
    private static Set<UUID> cachedTechnobladeUuids = null;

    static {
        BUILDER.comment("General settings").push("general");

        ENABLE_INSTANT_SHELL_CONSTRUCTION = BUILDER
                .comment("Enable instant shell construction (creative mode)")
                .define("enableInstantShellConstruction", false);

        WARN_PLAYER_INSTEAD_OF_KILLING = BUILDER
                .comment("Warn player instead of killing them when they don't have enough health")
                .define("warnPlayerInsteadOfKilling", false);

        BUILDER.pop();

        BUILDER.comment("Damage settings").push("damage");

        FINGERSTICK_DAMAGE = BUILDER
                .comment("Damage dealt when creating a shell (in half hearts)")
                .defineInRange("fingerstickDamage", 20.0, 0.0, 40.0);

        HARDCORE_FINGERSTICK_DAMAGE = BUILDER
                .comment("Damage dealt when creating a shell in hardcore mode")
                .defineInRange("hardcoreFingerstickDamage", 40.0, 0.0, 80.0);

        BUILDER.pop();

        BUILDER.comment("Energy settings").push("energy");

        SHELL_CONSTRUCTOR_CAPACITY = BUILDER
                .comment("Energy capacity/requirement for shell constructor")
                .defineInRange("shellConstructorCapacity", 256000L, 1000L, Long.MAX_VALUE);

        SHELL_STORAGE_CAPACITY = BUILDER
                .comment("Energy capacity of shell storage")
                .defineInRange("shellStorageCapacity", 320L, 0L, Long.MAX_VALUE);

        SHELL_STORAGE_CONSUMPTION = BUILDER
                .comment("Energy consumption per tick for shell storage")
                .defineInRange("shellStorageConsumption", 16L, 0L, 1000L);

        BUILDER.pop();

        BUILDER.comment("Shell Storage settings").push("shellStorage");

        SHELL_STORAGE_ACCEPTS_REDSTONE = BUILDER
                .comment("Whether shell storage accepts redstone power")
                .define("acceptsRedstone", true);

        SHELL_STORAGE_MAX_UNPOWERED_LIFESPAN = BUILDER
                .comment("Maximum time (in ticks) shell storage can run without power")
                .defineInRange("maxUnpoweredLifespan", 20, 0, 6000);

        BUILDER.pop();

        BUILDER.comment("Energy production settings").push("energyProduction");

        ENERGY_MAP = BUILDER
                .comment("Entity energy production map (format: 'entity_id:energy_per_tick')")
                .defineList("energyMap",
                        Arrays.asList(
                                "minecraft:chicken:2",
                                "minecraft:pig:16",
                                "minecraft:player:20",
                                "minecraft:villager:25",
                                "minecraft:wolf:22",
                                "minecraft:creeper:80",
                                "minecraft:enderman:160"
                        ),
                        obj -> obj instanceof String && ((String)obj).contains(":")
                );

        BUILDER.pop();

        BUILDER.comment("Synchronization settings").push("sync");

        SYNC_PRIORITY = BUILDER
                .comment("Shell priority order for automatic synchronization",
                        "Valid values: WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, GRAY,",
                        "LIGHT_GRAY, CYAN, PURPLE, BLUE, BROWN, GREEN, RED, BLACK, NEAREST, NATURAL")
                .defineList("syncPriority",
                        Collections.singletonList("NATURAL"),
                        obj -> obj instanceof String && isValidPriority((String)obj)
                );

        PRESERVE_ORIGINS = BUILDER
                .comment("If enabled, all shells share the same origins")
                .define("preserveOrigins", false);

        BUILDER.pop();

        BUILDER.comment("Tool settings").push("tools");

        WRENCH = BUILDER
                .comment("Item ID to use as wrench")
                .define("wrench", "minecraft:stick");

        BUILDER.pop();

        BUILDER.comment("Miscellaneous settings").push("misc");

        UPDATE_TRANSLATIONS_AUTOMATICALLY = BUILDER
                .comment("Update translations automatically on game launch")
                .define("updateTranslationsAutomatically", false);

        BUILDER.pop();

        BUILDER.comment("Easter egg settings").push("easterEggs");

        ENABLE_TECHNOBLADE_EASTER_EGG = BUILDER
                .comment("Enable Technoblade easter egg")
                .define("enableTechnobladeEasterEgg", true);

        RENDER_TECHNOBLADE_CAPE = BUILDER
                .comment("Render Technoblade's cape")
                .define("renderTechnobladeCape", false);

        ALLOW_TECHNOBLADE_ANNOUNCEMENTS = BUILDER
                .comment("Allow Technoblade announcements")
                .define("allowTechnobladeAnnouncements", true);

        ALLOW_TECHNOBLADE_QUOTES = BUILDER
                .comment("Allow Technoblade quotes")
                .define("allowTechnobladeQuotes", true);

        TECHNOBLADE_QUOTE_DELAY = BUILDER
                .comment("Delay between Technoblade quotes (in ticks)")
                .defineInRange("technobladeQuoteDelay", 1800, 200, 72000);

        TECHNOBLADE_UUIDS = BUILDER
                .comment("UUIDs of players to treat as Technoblade")
                .defineList("technobladeUuids",
                        Collections.emptyList(),
                        obj -> obj instanceof String && isValidUuid((String)obj)
                );

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static boolean isValidPriority(String priority) {
        try {
            ShellPriority.valueOf(priority.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isValidUuid(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        cachedEnergyMap = null;
        cachedSyncPriority = null;
        cachedTechnobladeUuids = null;
    }

    private static final SyncConfig INSTANCE = new SyncConfig();

    public static SyncConfig getInstance() {
        return INSTANCE;
    }

    public boolean enableInstantShellConstruction() {
        return ENABLE_INSTANT_SHELL_CONSTRUCTION.get();
    }

    public boolean warnPlayerInsteadOfKilling() {
        return WARN_PLAYER_INSTEAD_OF_KILLING.get();
    }

    public float fingerstickDamage() {
        return FINGERSTICK_DAMAGE.get().floatValue();
    }

    public float hardcoreFingerstickDamage() {
        return HARDCORE_FINGERSTICK_DAMAGE.get().floatValue();
    }

    public long shellConstructorCapacity() {
        return SHELL_CONSTRUCTOR_CAPACITY.get();
    }

    public long shellStorageCapacity() {
        return SHELL_STORAGE_CAPACITY.get();
    }

    public long shellStorageConsumption() {
        return SHELL_STORAGE_CONSUMPTION.get();
    }

    public boolean shellStorageAcceptsRedstone() {
        return SHELL_STORAGE_ACCEPTS_REDSTONE.get();
    }

    public int shellStorageMaxUnpoweredLifespan() {
        return SHELL_STORAGE_MAX_UNPOWERED_LIFESPAN.get();
    }

    public List<EnergyMapEntry> energyMap() {
        if (cachedEnergyMap == null) {
            cachedEnergyMap = ENERGY_MAP.get().stream()
                    .map(str -> {
                        String[] parts = str.split(":");
                        if (parts.length >= 3) {
                            String entityId = parts[0] + ":" + parts[1];
                            try {
                                long energy = Long.parseLong(parts[2]);
                                return EnergyMapEntry.of(entityId, energy);
                            } catch (NumberFormatException e) {
                                // Invalid format
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return cachedEnergyMap;
    }

    public List<ShellPriorityEntry> syncPriority() {
        if (cachedSyncPriority == null) {
            cachedSyncPriority = SYNC_PRIORITY.get().stream()
                    .map(str -> {
                        try {
                            ShellPriority priority = ShellPriority.valueOf(str.toUpperCase());
                            return new ShellPriorityEntry() {
                                @Override
                                public ShellPriority priority() {
                                    return priority;
                                }
                            };
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return cachedSyncPriority;
    }

    public String wrench() {
        return WRENCH.get();
    }

    public boolean updateTranslationsAutomatically() {
        return UPDATE_TRANSLATIONS_AUTOMATICALLY.get();
    }

    public boolean preserveOrigins() {
        return PRESERVE_ORIGINS.get();
    }

    public boolean enableTechnobladeEasterEgg() {
        return ENABLE_TECHNOBLADE_EASTER_EGG.get();
    }

    public boolean renderTechnobladeCape() {
        return RENDER_TECHNOBLADE_CAPE.get();
    }

    public boolean allowTechnobladeAnnouncements() {
        return ALLOW_TECHNOBLADE_ANNOUNCEMENTS.get();
    }

    public boolean allowTechnobladeQuotes() {
        return ALLOW_TECHNOBLADE_QUOTES.get();
    }

    public int TechnobladeQuoteDelay() {
        return TECHNOBLADE_QUOTE_DELAY.get();
    }

    public boolean isTechnoblade(UUID uuid) {
        if (cachedTechnobladeUuids == null) {
            cachedTechnobladeUuids = TECHNOBLADE_UUIDS.get().stream()
                    .map(str -> {
                        try {
                            return UUID.fromString(str);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        return cachedTechnobladeUuids.contains(uuid);
    }

    public void addTechnoblade(UUID uuid) {
        List<String> current = new ArrayList<>(TECHNOBLADE_UUIDS.get());
        current.add(uuid.toString());
        TECHNOBLADE_UUIDS.set(current);
        TECHNOBLADE_UUIDS.save();
        cachedTechnobladeUuids = null;
    }

    public void removeTechnoblade(UUID uuid) {
        List<String> current = new ArrayList<>(TECHNOBLADE_UUIDS.get());
        current.remove(uuid.toString());
        TECHNOBLADE_UUIDS.set(current);
        TECHNOBLADE_UUIDS.save();
        cachedTechnobladeUuids = null;
    }

    public void clearTechnobladeCache() {
        cachedTechnobladeUuids = null;
    }

    public interface EnergyMapEntry {
        default String entityId() {
            return "minecraft:pig";
        }

        default long outputEnergyQuantity() {
            return 16;
        }

        default EntityType<?> getEntityType() {
            ResourceLocation id = ResourceLocation.tryParse(this.entityId());
            return id == null ? EntityType.PIG : ForgeRegistries.ENTITY_TYPES.getValue(id);
        }

        static EnergyMapEntry of(EntityType<?> entityType, long outputEnergyQuantity) {
            return of(ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString(), outputEnergyQuantity);
        }

        static EnergyMapEntry of(String id, long outputEnergyQuantity) {
            return new EnergyMapEntry() {
                @Override
                public String entityId() {
                    return id;
                }

                @Override
                public long outputEnergyQuantity() {
                    return outputEnergyQuantity;
                }
            };
        }
    }

    public interface ShellPriorityEntry {
        default ShellPriority priority() {
            return ShellPriority.NATURAL;
        }
    }
}