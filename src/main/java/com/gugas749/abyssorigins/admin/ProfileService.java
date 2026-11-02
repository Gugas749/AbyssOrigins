package com.gugas749.abyssorigins.admin;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.event.XpEventHandler;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.network.packet.OpenProfilePacket;
import com.gugas749.abyssorigins.power.Assignments;
import com.gugas749.abyssorigins.power.Loadouts;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import com.gugas749.abyssorigins.skilltree.SkillService;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abysscore.api.permission.AbyssPermissionHandler;
import com.gugas749.abysscore.api.permission.AbyssPermissionLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;

/**
 * Server side of the staff profile (/abyssorigins profile). Every request is re-checked
 * against AbyssCore ADMIN — the client's own permission check is only cosmetic — and every
 * value is validated and clamped here. Edits reuse the same services the commands use and
 * are written to the server log for auditing.
 */
public final class ProfileService {

    public static final AbyssPermissionLevel REQUIRED = AbyssPermissionLevel.ADMIN;

    public static final int MAX_XP = 1_000_000, MAX_POINTS = 1_000;

    public enum Action {
        REFRESH,
        SET_ORIGIN, SET_CLASS,
        ADD_ORIGIN_XP, ADD_CLASS_XP,
        SET_ORIGIN_LEVEL, SET_CLASS_LEVEL,
        ADD_ORIGIN_POINTS, ADD_CLASS_POINTS,
        RESPEC_ORIGIN, RESPEC_CLASS,
        GRANT_ABILITY, REVOKE_ABILITY
    }

    private ProfileService() {}

    public static void open(ServerPlayer admin, ServerPlayer target) {
        if (!AbyssPermissionHandler.require(admin, REQUIRED)) return;
        PacketDistributor.sendToPlayer(admin, new OpenProfilePacket(ProfileSnapshot.of(target), true));
    }

    public static void handle(ServerPlayer admin, UUID targetId, Action action, String arg, int value) {
        if (!AbyssPermissionHandler.require(admin, REQUIRED)) return;
        ServerPlayer target = admin.server.getPlayerList().getPlayer(targetId);
        if (target == null) {
            admin.sendSystemMessage(Component.translatable("abyssorigins.profile.offline").withStyle(ChatFormatting.RED));
            return;
        }

        Component result = action == Action.REFRESH ? null : apply(target, action, arg, value);
        if (action != Action.REFRESH) {
            if (result == null) {
                admin.sendSystemMessage(Component.translatable("abyssorigins.profile.invalid", action.name()).withStyle(ChatFormatting.RED));
            } else {
                Abyssorigins.LOGGER.info("[AbyssOrigins] [Staff] {} edited {}: {} {} {}",
                    admin.getGameProfile().getName(), target.getGameProfile().getName(), action, arg, value);
                admin.sendSystemMessage(Component.literal("[" + target.getGameProfile().getName() + "] ").withStyle(ChatFormatting.GOLD)
                    .append(result.copy().withStyle(ChatFormatting.GREEN)));
            }
        }
        PacketDistributor.sendToPlayer(admin, new OpenProfilePacket(ProfileSnapshot.of(target), false));
    }

    /** @return feedback for the admin, or null when the request was invalid. */
    private static Component apply(ServerPlayer target, Action action, String arg, int value) {
        switch (action) {
            case SET_ORIGIN -> {
                if (arg.isEmpty()) {
                    Assignments.setOrigin(target, null);
                    target.sendSystemMessage(Component.translatable("abyssorigins.message.origin_cleared"));
                    return Component.literal("Origin cleared");
                }
                ResourceLocation id = ResourceLocation.tryParse(arg);
                Origin origin = id == null ? null : AbyssOriginsAPI.getOrigin(id);
                if (origin == null) return null;
                Assignments.setOrigin(target, origin);
                target.sendSystemMessage(Component.translatable("abyssorigins.message.origin_set", origin.getDisplayName()));
                return Component.literal("Origin set to ").append(origin.getDisplayName());
            }
            case SET_CLASS -> {
                if (arg.isEmpty()) {
                    Assignments.setClass(target, null);
                    target.sendSystemMessage(Component.translatable("abyssorigins.message.spec_removed"));
                    return Component.literal("Class cleared");
                }
                ResourceLocation id = ResourceLocation.tryParse(arg);
                Specialization spec = id == null ? null : SpecializationRegistry.get(id).orElse(null);
                if (spec == null) return null;
                Assignments.setClass(target, spec);
                target.sendSystemMessage(Component.translatable("abyssorigins.message.spec_set", spec.getDisplayName()));
                return Component.literal("Class set to ").append(spec.getDisplayName());
            }
            case ADD_ORIGIN_XP -> {
                if (!inRange(value, 1, MAX_XP) || AbyssOriginsAPI.getPlayerOrigin(target) == null) return null;
                XpEventHandler.addOriginXp(target, value);
                return Component.literal("+" + value + " origin XP");
            }
            case ADD_CLASS_XP -> {
                if (!inRange(value, 1, MAX_XP) || !hasClass(target)) return null;
                XpEventHandler.addClassXp(target, value);
                return Component.literal("+" + value + " class XP");
            }
            case SET_ORIGIN_LEVEL -> {
                if (!inRange(value, 1, XpEventHandler.MAX_LEVEL)) return null;
                XpEventHandler.setOriginLevel(target, value);
                return Component.literal("Origin level set to " + value);
            }
            case SET_CLASS_LEVEL -> {
                if (!inRange(value, 1, XpEventHandler.MAX_LEVEL) || !XpEventHandler.setClassLevel(target, value)) return null;
                return Component.literal("Class level set to " + value);
            }
            case ADD_ORIGIN_POINTS -> {
                if (!inRange(value, 1, MAX_POINTS)) return null;
                target.getData(SkillAttachments.PLAYER_SKILLS.get()).addSkillPoints(value);
                ModPackets.syncSkills(target);
                target.sendSystemMessage(Component.translatable("abyssorigins.message.skill_points_granted", String.valueOf(value)));
                return Component.literal("+" + value + " origin skill points");
            }
            case ADD_CLASS_POINTS -> {
                if (!inRange(value, 1, MAX_POINTS) || !hasClass(target)) return null;
                target.getData(SpecializationAttachments.PLAYER_SPEC.get()).addSkillPoints(value);
                ModPackets.syncSpecialization(target);
                return Component.literal("+" + value + " class skill points");
            }
            case RESPEC_ORIGIN -> {
                int refunded = SkillService.respecOrigin(target);
                target.sendSystemMessage(Component.translatable("abyssorigins.message.respec", String.valueOf(refunded)));
                return Component.literal("Origin tree respecced (" + refunded + " SP refunded)");
            }
            case RESPEC_CLASS -> {
                if (!hasClass(target)) return null;
                int refunded = SkillService.respecClass(target);
                target.sendSystemMessage(Component.translatable("abyssorigins.message.respec", String.valueOf(refunded)));
                return Component.literal("Class tree respecced (" + refunded + " SP refunded)");
            }
            case GRANT_ABILITY -> {
                AbilityRegistry.Entry e = AbilityRegistry.get(arg);
                if (e == null) return null;
                boolean fresh = !Loadouts.data(target).isGranted(arg);
                Loadouts.grant(target, arg);
                if (fresh) target.sendSystemMessage(Component.translatable("abyssorigins.message.ability_granted", e.ability().getDisplayName()));
                return Component.literal("Granted ").append(e.ability().getDisplayName());
            }
            case REVOKE_ABILITY -> {
                AbilityRegistry.Entry e = AbilityRegistry.get(arg);
                if (!Loadouts.revoke(target, arg)) return null;
                return Component.literal("Revoked ").append(e != null ? e.ability().getDisplayName() : Component.literal(arg));
            }
            default -> { return null; }
        }
    }

    private static boolean hasClass(ServerPlayer p) {
        PlayerSpecializationData d = p.getData(SpecializationAttachments.PLAYER_SPEC.get());
        return d.hasSpec();
    }

    private static boolean inRange(int v, int min, int max) { return v >= min && v <= max; }
}
