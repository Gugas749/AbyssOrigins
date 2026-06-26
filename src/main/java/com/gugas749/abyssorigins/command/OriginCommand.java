package com.gugas749.abyssorigins.command;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.event.XpEventHandler;
import com.gugas749.abyssorigins.network.ModPackets;
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
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 50))
                            .executes(OriginCommand::setLevel))))

                // addxp now goes through XpEventHandler so level-ups fire correctly
                .then(Commands.literal("addxp")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(OriginCommand::addXp))))

                .then(Commands.literal("addskillpoints")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                            .executes(OriginCommand::addSkillPoints))))
        );
    }

    // ---- set ------------------------------------------------------------

    private static int setOrigin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            String originArg    = StringArgumentType.getString(ctx, "origin");

            ResourceLocation id = originArg.contains(":")
                ? ResourceLocation.parse(originArg)
                : ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, originArg);

            Origin origin = AbyssOriginsAPI.getOrigin(id);
            if (origin == null) {
                ctx.getSource().sendFailure(Component.literal(
                    "Unknown origin: " + originArg + ". Use /abyssorigins list"));
                return 0;
            }

            AbyssOriginsAPI.setPlayerOrigin(player, origin);
            player.getData(OriginAttachments.PLAYER_ORIGIN.get()).setOriginId(origin.getId());

            // Grant starting points if never had them
            XpEventHandler.grantStartingPoints(player);

            ModPackets.syncPlayer(player);
            ModPackets.syncSkills(player);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getName().getString() + "'s origin to "
                + origin.getDisplayName()).withStyle(ChatFormatting.GREEN), true);

            player.sendSystemMessage(Component.literal(
                "§6[AbyssOrigins] §eYour origin has been set to §b"
                + origin.getDisplayName() + "§e."));

            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // ---- reset ----------------------------------------------------------

    private static int resetOrigin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");

            AbyssOriginsAPI.setPlayerOrigin(player, null);
            player.getData(OriginAttachments.PLAYER_ORIGIN.get()).setOriginId(null);
            ModPackets.syncPlayer(player);
            ModPackets.syncSkills(player);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "Cleared " + player.getName().getString() + "'s origin.")
                .withStyle(ChatFormatting.YELLOW), true);

            player.sendSystemMessage(Component.literal(
                "§6[AbyssOrigins] §eYour origin has been cleared."));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // ---- info -----------------------------------------------------------

    private static int infoOrigin(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player   = EntityArgument.getPlayer(ctx, "player");
            Origin origin         = AbyssOriginsAPI.getPlayerOrigin(player);
            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
            PlayerSkillData skills= player.getData(SkillAttachments.PLAYER_SKILLS.get());

            ctx.getSource().sendSuccess(() -> Component.literal(
                "=== " + player.getName().getString() + " ===")
                .withStyle(ChatFormatting.GOLD), false);

            if (origin == null) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "No origin assigned.").withStyle(ChatFormatting.GRAY), false);
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Origin: " + origin.getDisplayName() + " (" + origin.getId() + ")")
                    .withStyle(ChatFormatting.YELLOW), false);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Level: " + data.getOriginLevel()
                    + " | XP: " + data.getOriginXp()
                    + " | Skill Points: " + skills.getSkillPoints())
                    .withStyle(ChatFormatting.GREEN), false);
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Skills unlocked: " + skills.getUnlockedSkills().size())
                    .withStyle(ChatFormatting.AQUA), false);
                if (origin.getResourceType() != null) {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        "Resource: " + String.format("%.1f", data.getResource()) + " / "
                        + origin.getResourceType().getMaxAmount())
                        .withStyle(ChatFormatting.RED), false);
                }
            }
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // ---- list -----------------------------------------------------------

    private static int listOrigins(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() ->
            Component.literal("=== Available Origins ===").withStyle(ChatFormatting.GOLD), false);
        AbyssOriginsAPI.getAllOrigins().forEach(origin ->
            ctx.getSource().sendSuccess(() ->
                Component.literal("  - " + origin.getDisplayName()
                    + " §7(" + origin.getId().getPath() + ")"
                    + " [" + origin.getImpactLevel() + "]")
                .withStyle(ChatFormatting.YELLOW), false));
        return 1;
    }

    // ---- setlevel -------------------------------------------------------

    private static int setLevel(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int level = IntegerArgumentType.getInteger(ctx, "level");

            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
            // Recalculate XP floor so level display is consistent
            data.setOriginLevel(level);
            data.setOriginXp(XpEventHandler.xpRequiredToReach(level));

            ModPackets.syncPlayer(player);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "Set " + player.getName().getString() + "'s level to " + level), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // ---- addxp ----------------------------------------------------------

    private static int addXp(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int amount = IntegerArgumentType.getInteger(ctx, "amount");

            // Use XpEventHandler so level-up logic and skill point grants fire correctly
            XpEventHandler.grantXp(player, amount);

            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());
            ctx.getSource().sendSuccess(() -> Component.literal(
                "Added " + amount + " XP to " + player.getName().getString()
                + " (now " + data.getOriginXp() + " XP, level " + data.getOriginLevel() + ")"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    // ---- addskillpoints -------------------------------------------------

    private static int addSkillPoints(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
            int amount = IntegerArgumentType.getInteger(ctx, "amount");

            PlayerSkillData skills = player.getData(SkillAttachments.PLAYER_SKILLS.get());
            skills.addSkillPoints(amount);
            ModPackets.syncSkills(player);

            ctx.getSource().sendSuccess(() -> Component.literal(
                "Added " + amount + " skill points to " + player.getName().getString()
                + " (now " + skills.getSkillPoints() + ")"), true);

            player.sendSystemMessage(Component.literal(
                "§6[AbyssOrigins] §a+" + amount + " skill point"
                + (amount > 1 ? "s" : "") + "§e granted. Press §bK§e to open the skill tree."));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
