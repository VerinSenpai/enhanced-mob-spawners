package com.branders.spawnermod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue CAN_REMOVE_SPAWNER_EGGS = BUILDER
            .define("can_remove_spawner_eggs", true);

    public static final ModConfigSpec.IntValue EGG_DROP_CHANCE = BUILDER
            .defineInRange("egg_drop_chance", 4, 0, 100);

    public static final ModConfigSpec.BooleanValue EGG_DROPS_REQUIRE_PLAYER = BUILDER
            .define("egg_drops_require_player", false);

    public static final ModConfigSpec.BooleanValue DISABLE_SILK_TOUCH = BUILDER
            .define("disable_silk_touch", false);

    public static final ModConfigSpec.IntValue SPAWNER_HARDNESS = BUILDER
            .defineInRange("spawner_hardness", 5, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue LIMIT_SPAWNER_SPAWNS = BUILDER
            .define("limit_spawner_spawns", false);

    public static final ModConfigSpec.IntValue SPAWNER_SPAWNS_LIMIT = BUILDER
            .defineInRange("spawner_spawns_limit", 32, 0, Integer.MAX_VALUE);

//    public static final ModConfigSpec.BooleanValue LIMIT_EGG_SPAWNS = BUILDER
//            .define("limit_egg_spawns", false);
//
//    public static final ModConfigSpec.IntValue EGG_SPAWNS_LIMIT = BUILDER
//            .defineInRange("egg_spawns_limit", 32, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue ALLOW_SPAWNER_CONFIG = BUILDER
            .define("allow_spawner_config", true);

    public static final ModConfigSpec.BooleanValue DISABLE_COUNT_CONFIG = BUILDER
            .define("disable_count_config", false);

    public static final ModConfigSpec.BooleanValue DISABLE_SPEED_CONFIG = BUILDER
            .define("disable_speed_config", false);

    public static final ModConfigSpec.BooleanValue DISABLE_RANGE_CONFIG = BUILDER
            .define("disable_range_config", false);

    public static final ModConfigSpec.IntValue DEFAULT_RANGE = BUILDER
            .defineInRange("default_range", 16, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_EGGS = BUILDER
            .defineListAllowEmpty("excluded_eggs", List.of(), () -> "", Config::validateItemName);

    public static final ModConfigSpec.BooleanValue LOG_ITEM_ID_ON_RIGHT_CLICK = BUILDER
            .define("log_item_id_on_right_click", false);

    private static boolean validateItemName(final Object obj) { return obj instanceof String; }

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void addExclusion(String id) {
        List<String> exclusions = new ArrayList<>(EXCLUDED_EGGS.get());

        if (exclusions.contains(id))
            return;

        exclusions.add(id);
        EXCLUDED_EGGS.set(exclusions);
        SPEC.save();
    }

    public static void removeExclusion(String id) {
        List<String> exclusions = new ArrayList<>(EXCLUDED_EGGS.get());

        if (!exclusions.contains(id))
            return;

        exclusions.remove(id);
        EXCLUDED_EGGS.set(exclusions);
        SPEC.save();
    }

    public static void clearExclusions() {
        EXCLUDED_EGGS.set(EXCLUDED_EGGS.getDefault());
        SPEC.save();
    }

    public static void resetDefaults() {
        EGG_DROP_CHANCE.set(EGG_DROP_CHANCE.getDefault());
        DISABLE_SILK_TOUCH.set(DISABLE_SILK_TOUCH.getDefault());
        ALLOW_SPAWNER_CONFIG.set(ALLOW_SPAWNER_CONFIG.getDefault());
        DISABLE_COUNT_CONFIG.set(DISABLE_COUNT_CONFIG.getDefault());
        DISABLE_SPEED_CONFIG.set(DISABLE_SPEED_CONFIG.getDefault());
        DISABLE_RANGE_CONFIG.set(DISABLE_RANGE_CONFIG.getDefault());
        LIMIT_SPAWNER_SPAWNS.set(LIMIT_SPAWNER_SPAWNS.getDefault());
        SPAWNER_SPAWNS_LIMIT.set(SPAWNER_SPAWNS_LIMIT.getDefault());
        CAN_REMOVE_SPAWNER_EGGS.set(CAN_REMOVE_SPAWNER_EGGS.getDefault());
        EGG_DROPS_REQUIRE_PLAYER.set(EGG_DROPS_REQUIRE_PLAYER.getDefault());
        DEFAULT_RANGE.set(DEFAULT_RANGE.getDefault());
        SPAWNER_HARDNESS.set(SPAWNER_HARDNESS.getDefault());
        EXCLUDED_EGGS.set(EXCLUDED_EGGS.getDefault());
        LOG_ITEM_ID_ON_RIGHT_CLICK.set(LOG_ITEM_ID_ON_RIGHT_CLICK.getDefault());
        SPEC.save();
    }
}

