package com.gugas749.abyssorigins.command;

import com.gugas749.abyssorigins.admin.ProfileService;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

/**
 * /abyssorigins profile &lt;player&gt; — opens the staff profile GUI for an online player.
 * AbyssCore ADMIN only, and only from a player (the GUI has to open somewhere).
 */
public final class ProfileCommand {

    private ProfileCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("profile")
            .requires(src -> AbyssPermissionHandler.sourceHas(src, ProfileService.REQUIRED))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ProfileService.open(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"));
                    return 1;
                }));
    }
}
