package com.gugas749.abyssorigins.hiddenpower;

import com.gugas749.abyssorigins.hiddenpower.realm.PunishRealm;
import com.gugas749.abyssorigins.hiddenpower.realm.RealmData;
import com.gugas749.abyssorigins.hiddenpower.realm.Realms;
import com.gugas749.abyssorigins.api.HiddenAbility;
import com.gugas749.abyssorigins.power.Targeting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;

/**
 * Punish Dimension (hidden power). Aimed at another player within {@link #RANGE} blocks in line of sight:
 * the target (never the caster) is teleported to a cell of the punishment realm and bound to the cross for
 * 60 seconds. While the punishment lasts, using the power again steps the caster into the cell to
 * watch (and strike) or back out. One punishment per caster and per target at a time.
 */
public final class PunishAbility extends HiddenAbility {

    public static final double RANGE = 32;

    PunishAbility() {
        super("hidden_punish", "abyssorigins.ability.hidden_punish.name", "abyssorigins.ability.hidden_punish.desc", 90);
        icon("✝");
        color(0xFF8B0000);
    }

    @Override
    public boolean recast(ServerPlayer player) {
        RealmData data = RealmData.get(player.server);
        Map.Entry<UUID, RealmData.Session> mine = data.sessionBy(player.getUUID());
        if (mine == null) {
            // Left over from a punishment that ended while offline: just bring them back
            if (data.isObserving(player.getUUID()) && player.level().dimension() == Realms.PUNISHMENT) {
                Realms.travel(player, data.takeObserverReturn(player.getUUID()));
                return true;
            }
            return false;
        }
        ServerLevel realm = Realms.level(player, Realms.PUNISHMENT);
        if (realm != null) PunishRealm.toggleObserve(player, mine.getValue(), realm);
        return true;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        LivingEntity hit = Targeting.lookTarget(player, RANGE).orElse(null);
        if (!(hit instanceof ServerPlayer target) || target == player) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.punish_no_target"), true);
            return false;
        }
        if (!target.isAlive() || target.isSpectator() || Realms.inRealm(target)) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.punish_invalid", target.getDisplayName()), true);
            return false;
        }
        if (PunishRealm.isPunished(target)) {
            player.displayClientMessage(Component.translatable("abyssorigins.message.punish_already", target.getDisplayName()), true);
            return false;
        }
        ServerLevel realm = Realms.level(player, Realms.PUNISHMENT);
        if (realm == null) return false;
        PunishRealm.punish(player, target, realm);
        player.displayClientMessage(Component.translatable("abyssorigins.message.punish_cast", target.getDisplayName()), true);
        return true;
    }
}
