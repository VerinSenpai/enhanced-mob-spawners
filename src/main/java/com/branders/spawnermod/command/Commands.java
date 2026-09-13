package com.branders.spawnermod.command;

import com.branders.spawnermod.config.Config;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber
public class Commands {
    @SubscribeEvent
    private static void onRegisterCommands(RegisterCommandsEvent event) {
        var root = literal("ems");
        root.then(newCommand(Config.EGG_DROP_CHANCE, IntegerArgumentType.integer(0, 100), Integer.class));
        root.then(newCommand(Config.DISABLE_SILK_TOUCH, BoolArgumentType.bool(), Boolean.class));
        root.then(newCommand(Config.ALLOW_SPAWNER_CONFIG, BoolArgumentType.bool(), Boolean.class));
        root.then(newCommand(Config.DISABLE_COUNT_CONFIG, BoolArgumentType.bool(), Boolean.class));
        root.then(newCommand(Config.DISABLE_SPEED_CONFIG, BoolArgumentType.bool(), Boolean.class));
        root.then(newCommand(Config.DISABLE_RANGE_CONFIG, BoolArgumentType.bool(), Boolean.class));
        root.then(newCommand(Config.LIMIT_SPAWNER_SPAWNS, BoolArgumentType.bool(), Boolean.class));
        root.then(newCommand(Config.SPAWNER_SPAWNS_LIMIT, IntegerArgumentType.integer(0), Integer.class));
        root.then(newCommand(Config.CAN_REMOVE_SPAWNER_EGGS, BoolArgumentType.bool(), Boolean.class));
        root.then(newCommand(Config.EGG_DROPS_REQUIRE_PLAYER, BoolArgumentType.bool(), Boolean.class));
        root.then(newCommand(Config.DEFAULT_RANGE, IntegerArgumentType.integer(0), Integer.class));
        root.then(newCommand(Config.SPAWNER_HARDNESS, IntegerArgumentType.integer(0), Integer.class));
        root.then(newCommand(Config.LOG_ITEM_ID_ON_RIGHT_CLICK, BoolArgumentType.bool(), Boolean.class));

        root.then(literal("reset")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {Config.resetDefaults(); return Command.SINGLE_SUCCESS;}));

        root.then(literal("excluded_eggs")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    var exclusions = Config.EXCLUDED_EGGS.get();

                    ctx.getSource().sendSystemMessage(
                            Component.literal("Excluded Eggs: " + exclusions)
                    );

                    return Command.SINGLE_SUCCESS;
                })

                .then(literal("add").then(argument("egg", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                BuiltInRegistries.ITEM.stream()
                                        .filter(SpawnEggItem.class::isInstance)
                                        .map(BuiltInRegistries.ITEM::getKey),
                                builder))
                        .executes(ctx -> {
                            Config.addExclusion(StringArgumentType.getString(ctx, "egg"));
                            return Command.SINGLE_SUCCESS;
                        })
                ))

                .then(literal("remove").then(argument("egg", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                new ArrayList<>(Config.EXCLUDED_EGGS.get()),
                                builder))
                        .executes(ctx -> {
                            Config.removeExclusion(StringArgumentType.getString(ctx, "egg"));
                            return Command.SINGLE_SUCCESS;
                        })

                ))

                .then(literal("clear")
                        .executes(ctx -> {Config.clearExclusions(); return Command.SINGLE_SUCCESS;})
                ));


        event.getDispatcher().register(root);
    }

    private static <T> LiteralArgumentBuilder<CommandSourceStack> newCommand(
            ConfigValue<T> config,
            ArgumentType<T> argumentType,
            Class<T> type
    ) {
        String name = config.getPath().getFirst();

        var command = literal(name)
                .executes(ctx -> {
                    var value = config.get();
                    ctx.getSource().sendSystemMessage(Component.literal("%s is currently set to %s".formatted(name, value)));
                    return Command.SINGLE_SUCCESS;
                });

        command.then(argument("value", argumentType)
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    T value = ctx.getArgument("value", type);
                    config.set(value);
                    ctx.getSource().sendSystemMessage(Component.literal("%s updated to %s".formatted(name, value)));
                    config.save();
                    return Command.SINGLE_SUCCESS;
                }));

        return command;
    }
}