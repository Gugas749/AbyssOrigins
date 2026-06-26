package com.gugas749.abyssorigins.command;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registered as a sub-literal of /abyssorigins in OriginCommand.
 * All subcommands are OP-level 2.
 *
 * /abyssorigins specialization set <player> <id>
 * /abyssorigins specialization remove <player>
 * /abyssorigins specialization addlevel <player> <amount>      <- called by FTB Quests
 * /abyssorigins specialization addskillpoints <player> <amount>
 * /abyssorigins specialization info <player>
 * /abyssorigins specialization list
 */
public class SpecializationCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("specialization")
            .then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("spec", StringArgumentType.string())
                        .suggests((ctx, builder) -> {
                            SpecializationRegistry.getAll().forEach(s ->
                                builder.suggest(s.getId().getPath()));
                            return builder.buildFuture();
                        })
                        .executes(SpecializationCommand::setSpec))))
            .then(Commands.literal("remove")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(SpecializationCommand::removeSpec)))
            .then(Commands.literal("addlevel")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(SpecializationCommand::addLevel))))
            .then(Commands.literal("addskillpoints")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(SpecializationCommand::addSkillPoints))))
            .then(Commands.literal("info")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(SpecializationCommand::info)))
            .then(Commands.literal("list")
                .executes(SpecializationCommand::list));
    }

    private static int setSpec(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            String specArg      = StringArgumentType.getString(ctx, "spec");

            ResourceLocation id = specArg.contains(":")
                ? ResourceLocation.parse(specArg)
                : ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, specArg);

            Specialization spec = SpecializationRegistry.get(id).orElse(null);
            if (spec == null) {
                ctx.getSource().sendFailure(Component.literal(
                    "Unknown specialization: " + specArg + ". Use /abyssorigins specialization list"));
                return 0;
            }

            PlayerSpecializationData data =
                player.getData(SpecializationAttachments.PLAYER_SPEC.get());

            // Remove old spec passives if any
            if (data.hasSpec()) {
                SpecializationRegistry.get(data.getSpecId()).ifPresent(old ->
                    old.getPassives().forEach(p -> p.onRemove(player)));
            }

            data.setSpecId(id);
            if (data.getSpecLevel() == 0) data.setSpecLevel(1);

            // Apply new spec passives
            spec.getPassives().forEach(p -> p.onEquip(player));

            // Grant starting skill points if none
            if (data.getSkillPoints() == 0 && data.getUnlockedSkills().isEmpty()) {
                data.addSkillPoints(2);
            }

            ModPackets.syncSpecialization(player);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getName().getString() + "'s specialization to "
                + spec.getDisplayName().getString()).withStyle(ChatFormatting.GREEN), true);

            player.sendSystemMessage(Component.translatable(
                "abyssorigins.message.spec_set", spec.getDisplayName()));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int removeSpec(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            PlayerSpecializationData data =
                player.getData(SpecializationAttachments.PLAYER_SPEC.get());

            if (data.hasSpec()) {
                SpecializationRegistry.get(data.getSpecId()).ifPresent(spec ->
                    spec.getPassives().forEach(p -> p.onRemove(player)));
            }

            data.setSpecId(null);
            ModPackets.syncSpecialization(player);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "Cleared " + player.getName().getString() + "'s specialization.")
                .withStyle(ChatFormatting.YELLOW), true);

            player.sendSystemMessage(Component.translatable("abyssorigins.message.spec_removed"));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int addLevel(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int amount = IntegerArgumentType.getInteger(ctx, "amount");

            PlayerSpecializationData data =
                player.getData(SpecializationAttachments.PLAYER_SPEC.get());

            int oldLevel = data.getSpecLevel();
            data.addSpecLevel(amount);
            // Each level grants 1 skill point
            data.addSkillPoints(amount);

            ModPackets.syncSpecialization(player);

            int newLevel = data.getSpecLevel();
            ctx.getSource().sendSuccess(() -> Component.literal(
                player.getName().getString() + "'s spec level: " + oldLevel + " → " + newLevel
                + " (+" + amount + " skill points)"), true);

            player.sendSystemMessage(Component.translatable(
                "abyssorigins.message.spec_level_up",
                String.valueOf(newLevel),
                String.valueOf(amount)));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int addSkillPoints(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int amount = IntegerArgumentType.getInteger(ctx, "amount");

            PlayerSpecializationData data =
                player.getData(SpecializationAttachments.PLAYER_SPEC.get());
            data.addSkillPoints(amount);
            ModPackets.syncSpecialization(player);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "Added " + amount + " spec skill points to " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int info(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            PlayerSpecializationData data =
                player.getData(SpecializationAttachments.PLAYER_SPEC.get());

            ctx.getSource().sendSuccess(() -> Component.literal(
                "=== " + player.getName().getString() + " Specialization ===")
                .withStyle(ChatFormatting.GOLD), false);

            if (!data.hasSpec()) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "No specialization assigned.").withStyle(ChatFormatting.GRAY), false);
            } else {
                SpecializationRegistry.get(data.getSpecId()).ifPresent(spec -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "Spec: " + spec.getDisplayName().getString()
                        + " | Level: " + data.getSpecLevel()
                        + " | Points: " + data.getSkillPoints())
                        .withStyle(ChatFormatting.YELLOW), false);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "Skills unlocked: " + data.getUnlockedSkills().size())
                        .withStyle(ChatFormatting.AQUA), false);
                });
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
            Component.literal("=== Available Specializations ===")
                .withStyle(ChatFormatting.GOLD), false);
        SpecializationRegistry.getAll().forEach(spec ->
            ctx.getSource().sendSuccess(() ->
                Component.literal("  - " + spec.getDisplayName().getString()
                    + " \u00a77(" + spec.getId().getPath() + ")")
                .withStyle(ChatFormatting.YELLOW), false));
        return 1;
    }
}
