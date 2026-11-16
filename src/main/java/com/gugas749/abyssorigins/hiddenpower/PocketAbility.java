package com.gugas749.abyssorigins.hiddenpower;

import com.gugas749.abyssorigins.hiddenpower.realm.PocketRealm;
import com.gugas749.abyssorigins.hiddenpower.realm.Realms;
import com.gugas749.abyssorigins.api.HiddenAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pocket Dimension (hidden power). Teleports the caster (only the caster) to their
 * own private plot of the pocket realm: an always-night void with a starter platform. Use again
 * inside to return to where they left from.
 */
public final class PocketAbility extends HiddenAbility {

    PocketAbility() {
        super("hidden_pocket", "abyssorigins.ability.hidden_pocket.name", "abyssorigins.ability.hidden_pocket.desc", 10);
        icon("◈");
        color(0xFF6A5ACD);
    }

    @Override
    public boolean recast(ServerPlayer player) {
        if (player.level().dimension() != Realms.POCKET) return false;
        PocketRealm.leave(player);
        return true;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        if (player.level().dimension() == Realms.PUNISHMENT) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.pocket_blocked"), true);
            return false;
        }
        ServerLevel pocket = Realms.level(player, Realms.POCKET);
        if (pocket == null) return false;
        PocketRealm.enter(player, pocket);
        player.displayClientMessage(Component.translatable("abyssorigins.message.pocket_entered"), true);
        return true;
    }
}
