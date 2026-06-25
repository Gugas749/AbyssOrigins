package com.gugas749.abyssorigins.command;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Abyssorigins.MODID)
public class OriginCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("abyssorigins")
                .requires(src -> src.hasPermission(2)) // OP level 2
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("origin", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                AbyssOriginsAPI.getAllOrigins().forEach(o ->
                                    builder.suggest(o.getId().getPath()));
                                return builder.buildFuture();
                            })
                            .executes(OriginCommand::setOrigin))))
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(OriginCommand::resetOrigin)))
                .then(Commands.literal("info")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(OriginCommand::infoOrigin)))
                .then(Commands.literal("list")
                    .executes(OriginCommand::listOrigins))
                .then(Commands.literal("setlevel")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 50))
                            .executes(OriginCommand::setLevel))))
                .then(Commands.literal("addxp")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                            .executes(OriginCommand::addXp))))
        );
    }

    private static int setOrigin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            String originArg = StringArgumentType.getString(ctx, "origin");

            // Accept both "vampire" and "abyssorigins:vampire"
            ResourceLocation id = originArg.contains(":")
                ? ResourceLocation.parse(originArg)
                : ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, originArg);

            Origin origin = AbyssOriginsAPI.getOrigin(id);
            if (origin == null) {
                ctx.getSource().sendFailure(Component.literal(
                    "Unknown origin: " + originArg + ". Use /abyssorigins list to see available origins."));
                return 0;
            }

            AbyssOriginsAPI.setPlayerOrigin(player, origin);

            // Persist the ID
            player.getData(OriginAttachments.PLAYER_ORIGIN.get()).setOriginId(origin.getId());

            ModPackets.syncPlayer(player);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getName().getString() + "'s origin to "
                + origin.getDisplayName()).withStyle(ChatFormatting.GREEN), true);

            player.sendSystemMessage(Component.literal(
                "§6[AbyssOrigins] §eYour origin has been set to §b" + origin.getDisplayName() + "§e."));

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int resetOrigin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

            AbyssOriginsAPI.setPlayerOrigin(player, null);
            player.getData(OriginAttachments.PLAYER_ORIGIN.get()).setOriginId(null);
            ModPackets.syncPlayer(player);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "Cleared " + player.getName().getString() + "'s origin.").withStyle(ChatFormatting.YELLOW), true);

            player.sendSystemMessage(Component.literal("§6[AbyssOrigins] §eYour origin has been cleared."));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int infoOrigin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());

            ctx.getSource().sendSuccess(() -> Component.literal(
                "=== " + player.getName().getString() + " ===").withStyle(ChatFormatting.GOLD), false);

            if (origin == null) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "No origin assigned.").withStyle(ChatFormatting.GRAY), false);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Origin: " + origin.getDisplayName()
                    + " (" + origin.getId() + ")").withStyle(ChatFormatting.YELLOW), false);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Level: " + data.getOriginLevel() + " | XP: " + data.getOriginXp()
                    ).withStyle(ChatFormatting.GREEN), false);
                if (origin.getResourceType() != null) {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "Resource: " + String.format("%.1f", data.getResource()) + " / "
                        + origin.getResourceType().getMaxAmount()
                        ).withStyle(ChatFormatting.RED), false);
                }
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Abilities: " + origin.getAbilities().size()
                    + " | Passives: " + origin.getPassives().size()
                    ).withStyle(ChatFormatting.AQUA), false);
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int listOrigins(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
            Component.literal("=== Available Origins ===").withStyle(ChatFormatting.GOLD), false);

        AbyssOriginsAPI.getAllOrigins().forEach(origin ->
            ctx.getSource().sendSuccess(() ->
                Component.literal("  - " + origin.getDisplayName()
                    + " §7(" + origin.getId().getPath() + ")"
                    + " [" + origin.getImpactLevel() + "]"
                ).withStyle(ChatFormatting.YELLOW), false)
        );
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int level = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level");
            player.getData(OriginAttachments.PLAYER_ORIGIN.get()).setOriginLevel(level);
            ModPackets.syncPlayer(player);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getName().getString() + "'s level to " + level), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount");
            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
            data.setOriginXp(data.getOriginXp() + amount);
            ModPackets.syncPlayer(player);
            ctx.getSource().sendSuccess(() -> Component.literal(
                "Added " + amount + " XP to " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
