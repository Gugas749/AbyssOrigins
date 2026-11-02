package com.gugas749.abyssorigins.command;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.event.XpEventHandler;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.power.Assignments;
import com.gugas749.abyssorigins.skilltree.SkillService;
import com.gugas749.abyssorigins.skilltree.data.PlayerSkillData;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("origin", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                AbyssOriginsAPI.getAllOrigins().stream().filter(o -> !o.isHidden())
                                    .forEach(o -> builder.suggest(o.getId().getPath()));
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
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 50))
                            .executes(OriginCommand::setLevel))))
                .then(Commands.literal("addxp")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(OriginCommand::addXp))))
                .then(Commands.literal("addskillpoints")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(OriginCommand::addSkillPoints))))
                .then(Commands.literal("respec")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(OriginCommand::respec)))
                // ---- Specialization subcommand ----
                .then(SpecializationCommand.build())
                // ---- Abilities: grant / revoke access (hidden Eminence powers included) ----
                .then(AbilityCommand.build())
                // ---- Staff profile GUI (AbyssCore ADMIN) ----
                .then(ProfileCommand.build())
        );
    }

    private static int setOrigin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            String originArg    = StringArgumentType.getString(ctx, "origin");
            ResourceLocation id = originArg.contains(":")
                ? ResourceLocation.parse(originArg)
                : ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, originArg);
            Origin origin = AbyssOriginsAPI.getOrigin(id);
            if (origin == null) { ctx.getSource().sendFailure(Component.literal("Unknown origin: " + originArg)); return 0; }
            Assignments.setOrigin(player, origin);
            ctx.getSource().sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s origin to " + origin.getDisplayName().getString()).withStyle(ChatFormatting.GREEN), true);
            player.sendSystemMessage(Component.translatable("abyssorigins.message.origin_set", origin.getDisplayName()));
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage())); return 0; }
    }

    private static int resetOrigin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            Assignments.setOrigin(player, null);
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + player.getName().getString() + "'s origin.").withStyle(ChatFormatting.YELLOW), true);
            player.sendSystemMessage(Component.translatable("abyssorigins.message.origin_cleared"));
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage())); return 0; }
    }

    private static int infoOrigin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player   = EntityArgument.getPlayer(ctx, "player");
            Origin origin         = AbyssOriginsAPI.getPlayerOrigin(player);
            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
            PlayerSkillData skills= player.getData(SkillAttachments.PLAYER_SKILLS.get());
            ctx.getSource().sendSuccess(() -> Component.literal("=== " + player.getName().getString() + " ===").withStyle(ChatFormatting.GOLD), false);
            if (origin == null) ctx.getSource().sendSuccess(() -> Component.literal("No origin assigned.").withStyle(ChatFormatting.GRAY), false);
            else {
                ctx.getSource().sendSuccess(() -> Component.literal("Origin: " + origin.getDisplayName().getString() + " | Level: " + data.getOriginLevel() + " | XP: " + data.getOriginXp() + " | SP: " + skills.getSkillPoints()).withStyle(ChatFormatting.YELLOW), false);
                if (origin.getResourceType() != null) ctx.getSource().sendSuccess(() -> Component.literal("Resource: " + String.format("%.1f", data.getResource()) + "/100").withStyle(ChatFormatting.RED), false);
            }
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage())); return 0; }
    }

    private static int listOrigins(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("=== Available Origins ===").withStyle(ChatFormatting.GOLD), false);
        AbyssOriginsAPI.getAllOrigins().stream().filter(o -> !o.isHidden()).forEach(o -> ctx.getSource().sendSuccess(() ->
            Component.literal("  - " + o.getDisplayName().getString() + " \u00a77(" + o.getId().getPath() + ") [" + o.getImpactLevel() + "]").withStyle(ChatFormatting.YELLOW), false));
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int level = IntegerArgumentType.getInteger(ctx, "level");
            XpEventHandler.setOriginLevel(player, level);
            ctx.getSource().sendSuccess(() -> Component.literal("Set " + player.getName().getString() + "'s level to " + level), true);
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage())); return 0; }
    }

    private static int addXp(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int amount = IntegerArgumentType.getInteger(ctx, "amount");
            XpEventHandler.grantXp(player, amount);
            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
            ctx.getSource().sendSuccess(() -> Component.literal("Added " + amount + " XP to " + player.getName().getString() + " (now level " + data.getOriginLevel() + ")"), true);
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage())); return 0; }
    }

    private static int respec(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int refunded = SkillService.respecOrigin(player);
            player.sendSystemMessage(Component.translatable("abyssorigins.message.respec", String.valueOf(refunded)));
            ctx.getSource().sendSuccess(() -> Component.literal("Refunded " + refunded + " origin SP to " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage())); return 0; }
    }

    private static int addSkillPoints(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int amount = IntegerArgumentType.getInteger(ctx, "amount");
            PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
            skills.addSkillPoints(amount);
            ModPackets.syncSkills(player);
            player.sendSystemMessage(Component.translatable("abyssorigins.message.skill_points_granted", String.valueOf(amount)));
            ctx.getSource().sendSuccess(() -> Component.literal("Added " + amount + " SP to " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage())); return 0; }
    }
}
