package com.gugas749.abyssorigins.command;

import com.gugas749.abyssorigins.api.Ability;
import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Registered as a sub-literal of /abyssorigins in OriginCommand. AbyssCore ADMIN (or console).
 *
 * /abyssorigins ability grant  <player> <ability>   — any registered ability, hidden Eminence powers included
 * /abyssorigins ability revoke <player> <ability>   — removes a direct grant
 * /abyssorigins ability info   <player>             — loadouts + granted list
 * /abyssorigins ability list                        — every public ability id
 *
 * Hidden abilities are never suggested; they are granted by exact id, like hidden origins.
 */
public final class AbilityCommand {

    private AbilityCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("ability")
            .requires(src -> AbyssPermissionHandler.sourceHas(src, AbyssPermissionLevel.ADMIN) || src.hasPermission(2))
            .then(Commands.literal("grant")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("ability", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                            AbilityRegistry.visible().stream().map(AbilityRegistry.Entry::id), b))
                        .executes(AbilityCommand::grant))))
            .then(Commands.literal("revoke")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("ability", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            try { return SharedSuggestionProvider.suggest(Loadouts.data(EntityArgument.getPlayer(ctx, "player")).granted(), b); }
                            catch (CommandSyntaxException e) { return b.buildFuture(); }
                        })
                        .executes(AbilityCommand::revoke))))
            .then(Commands.literal("info")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(AbilityCommand::info)))
            .then(Commands.literal("list")
                .executes(AbilityCommand::list));
    }

    private static int grant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String id = StringArgumentType.getString(ctx, "ability");
        AbilityRegistry.Entry e = AbilityRegistry.get(id);
        if (e == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown ability: " + id + ". Use /abyssorigins ability list"));
            return 0;
        }
        boolean fresh = !Loadouts.data(player).isGranted(id);
        Loadouts.grant(player, id);
        boolean usable = Loadouts.knows(player, e.group(), id);
        ctx.getSource().sendSuccess(() -> Component.literal((fresh ? "Granted " : "Already granted: ") + id
            + " (" + e.ability().getDisplayName().getString() + ") to " + player.getName().getString()
            + (usable ? "" : " — usable once they have a " + (e.group() == SlotGroup.CLASS ? "class" : "origin")))
            .withStyle(fresh ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
        if (fresh) player.sendSystemMessage(Component.translatable("abyssorigins.message.ability_granted", e.ability().getDisplayName()));
        return 1;
    }

    private static int revoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String id = StringArgumentType.getString(ctx, "ability");
        if (!Loadouts.revoke(player, id)) {
            ctx.getSource().sendFailure(Component.literal(player.getName().getString() + " was not granted " + id
                + " (default-kit and skill powers can't be revoked)"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Revoked " + id + " from " + player.getName().getString())
            .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("=== " + player.getName().getString() + " Abilities ===").withStyle(ChatFormatting.GOLD), false);
        for (SlotGroup g : SlotGroup.values()) {
            MutableComponent line = Component.literal(g == SlotGroup.ORIGIN ? "Origin: " : "Class: ").withStyle(ChatFormatting.YELLOW);
            for (int i = 0; i < g.size(); i++) {
                String id = Loadouts.data(player).slot(g, i);
                line.append(Component.literal((i == 0 ? "" : " | ") + (i + 1) + "=" + (id.isEmpty() ? "-" : id))
                    .withStyle(ChatFormatting.WHITE));
            }
            src.sendSuccess(() -> line, false);
            Map<String, Loadouts.Source> known = Loadouts.known(player, g);
            src.sendSuccess(() -> Component.literal("  known: " + String.join(", ", known.keySet())).withStyle(ChatFormatting.GRAY), false);
        }
        MutableComponent granted = Component.literal("Granted: ").withStyle(ChatFormatting.LIGHT_PURPLE);
        if (Loadouts.data(player).granted().isEmpty()) granted.append(Component.literal("none").withStyle(ChatFormatting.GRAY));
        for (String id : Loadouts.data(player).granted()) {
            AbilityRegistry.Entry e = AbilityRegistry.get(id);
            granted.append(Component.literal(id + (e == null ? " (missing)" : e.hidden() ? " (hidden)" : "") + " ").withStyle(ChatFormatting.WHITE));
        }
        src.sendSuccess(() -> granted, false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal("=== Abilities ===").withStyle(ChatFormatting.GOLD), false);
        for (AbilityRegistry.Entry e : AbilityRegistry.visible()) {
            Ability a = e.ability();
            ctx.getSource().sendSuccess(() -> Component.literal("  " + e.id()).withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("  " + a.getDisplayName().getString() + " · " + e.ownerName().getString()
                    + " (" + e.group().name().toLowerCase() + ")").withStyle(ChatFormatting.GRAY)), false);
        }
        return 1;
    }
}
