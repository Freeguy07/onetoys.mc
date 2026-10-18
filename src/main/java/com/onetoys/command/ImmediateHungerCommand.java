package com.onetoys.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.onetoys.Onetoys;
import com.onetoys.api.PerestroykaHungerAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Collections;

/**
 * Command to control and query the "immediate hunger" mode and manually trigger
 * the immediate hunger rule on players.
 * <p>
 * Rule: Automatically drops the player's hunger timer to its lowest value (0) immediately
 * if they haven't eaten any food. Returns true if modified, false if active food is present.
 */
@EventBusSubscriber(modid = Onetoys.MODID)
public class ImmediateHungerCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /onetoys immediate_hunger ...
        dispatcher.register(Commands.literal("onetoys").then(buildCommandTree("immediate_hunger")));

        // Register top-level alias /immediatehunger ...
        dispatcher.register(buildCommandTree("immediatehunger"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandTree(String rootLiteral) {
        return Commands.literal(rootLiteral)
                .executes(ctx -> sendStatus(ctx.getSource()))
                .then(Commands.literal("query")
                        .executes(ctx -> sendStatus(ctx.getSource())))
                .then(Commands.literal("on")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> setImmediateHunger(ctx.getSource(), true)))
                .then(Commands.literal("enable")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> setImmediateHunger(ctx.getSource(), true)))
                .then(Commands.literal("off")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> setImmediateHunger(ctx.getSource(), false)))
                .then(Commands.literal("disable")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> setImmediateHunger(ctx.getSource(), false)))
                .then(Commands.literal("set")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setImmediateHunger(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> setImmediateHunger(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled"))))
                .then(Commands.literal("apply")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return applyToPlayers(ctx.getSource(), Collections.singleton(player));
                        })
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> applyToPlayers(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))));
    }

    private static int sendStatus(CommandSourceStack source) {
        boolean active = PerestroykaHungerAPI.isImmediateHungerActive();
        Component statusComponent = Component.literal("Immediate hunger is currently ")
                .append(Component.literal(active ? "ENABLED" : "DISABLED")
                        .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));
        source.sendSuccess(() -> statusComponent, false);
        return active ? 1 : 0;
    }

    private static int setImmediateHunger(CommandSourceStack source, boolean enabled) {
        PerestroykaHungerAPI.setImmediateHungerActive(enabled, source.getServer());
        if (enabled) {
            source.sendSuccess(() -> Component.literal(
                    "Immediate hunger has been ENABLED. Players without active food will drop to lowest hunger (0) immediately.")
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Immediate hunger has been DISABLED.")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
        return 1;
    }

    private static int applyToPlayers(CommandSourceStack source, Collection<ServerPlayer> players) {
        int modifiedCount = 0;
        for (ServerPlayer player : players) {
            // Rule: Automatically drops the player's hunger timer to its lowest value (0) immediately
            // if they haven't eaten any food. Returns true if modified, false if active food is present.
            boolean modified = PerestroykaHungerAPI.setHungerToLowest(player);
            if (modified) {
                modifiedCount++;
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + ": Hunger dropped to lowest (0) [modified=true]")
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                boolean hasFood = PerestroykaHungerAPI.hasActiveFoods(player);
                String reason = player.isCreative() ? "creative mode" : (hasFood ? "active food is present" : "timer already 0 or canceled");
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + ": Not modified (" + reason + ") [modified=false]")
                        .withStyle(ChatFormatting.GRAY), false);
            }
        }
        return modifiedCount;
    }
}
