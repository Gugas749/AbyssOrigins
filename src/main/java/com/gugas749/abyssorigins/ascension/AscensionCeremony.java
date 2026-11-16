package com.gugas749.abyssorigins.ascension;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Ascension;
import com.gugas749.abyssorigins.api.Ascension.Motif;
import com.gugas749.abyssorigins.api.Ascension.Timeline;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abysscore.api.cinematic.AbyssCinematics;
import com.gugas749.abyssorigins.origin.eminence.AtomicBlast;
import com.gugas749.abyssorigins.power.Fx;
import com.gugas749.abyssorigins.power.Hold;
import com.gugas749.abyssorigins.power.Targeting;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import com.gugas749.abyssorigins.skilltree.registry.SkillEffectHandler;
import com.gugas749.abysscore.api.vfx.AbyssVfx;
import com.gugas749.abyssorigins.vfx.OriginVfx;
import com.gugas749.abysscore.api.vfx.VfxType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server side of the ascension ceremony ({@link Ascension}), played when a player's origin level reaches a new
 * stage. On the stage's {@link Timeline}:
 *
 *  - 0..rise: the player floats up (Levitation), then is held in mid-air ({@link Hold}) until release.
 *    They take no damage (Resistance V) and can't use powers for the whole ceremony.
 *  - 0..burst: energy gathers around them: the ASCENSION effect plus the motif's own effects and sounds.
 *  - burst: an energy explosion that throws nearby enemies back without hurting them or any block, then
 *    heals the player fully and fills their resource.
 *  - release: they drift down with Slow Falling.
 *
 * The bonus skill points and stage stats are given at once (in {@link #onLevelChanged}); the ceremony waits
 * while the player is casting I Am Atomic, sleeping or dead.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID)
public final class AscensionCeremony {

    private static final ResourceLocation HOLD = Hold.id("ascension_hold");
    private static final double VIEW_RANGE = 96;
    private static final int SLOW_FALL_TICKS = 100;

    private static final class Ceremony {
        /** Where the player is kept: x/z from the start while rising, the whole position once held. */
        final UUID player;
        final ServerLevel level;
        final Origin origin;
        final int stage;
        final Timeline tl;
        final Vec3 ground;
        int age = -1;
        int nextBeat;
        Vec3 anchor;

        Ceremony(ServerPlayer p, Origin origin, int stage) {
            this.player = p.getUUID();
            this.level = p.serverLevel();
            this.origin = origin;
            this.stage = stage;
            this.tl = Ascension.timeline(stage);
            this.ground = p.position();
        }

        boolean lord() { return stage >= Ascension.LORD; }
        float k()      { return lord() ? 1.45f : 1f; }
        int c1()       { return origin.getAscensionColor(); }
        int c2()       { return origin.getAscensionColor2(); }
    }

    private static final List<Ceremony> ACTIVE = new ArrayList<>();
    /** Players owed a ceremony (player -> stage), started as soon as they are free. */
    private static final Map<UUID, Integer> PENDING = new LinkedHashMap<>();

    private AscensionCeremony() {}

    // ---- Entry points -------------------------------------------------------------------------

    /**
     * Call after the origin level changed. Re-applies the stage stats; when a new stage was reached, gives
     * its bonus skill points and queues the ceremony.
     */
    public static void onLevelChanged(ServerPlayer p, int oldLevel, int newLevel) {
        int from = Ascension.stageAt(oldLevel), to = Ascension.stageAt(newLevel);
        if (from == to) return;
        SkillEffectHandler.refresh(p);
        if (to < from) return;
        int bonus = 0;
        for (int s = from + 1; s <= to; s++) bonus += Ascension.bonusPoints(s);
        if (bonus > 0) {
            p.getData(SkillAttachments.PLAYER_SKILLS.get()).addSkillPoints(bonus);
            ModPackets.syncSkills(p);
        }
        PENDING.merge(p.getUUID(), to, Math::max);
    }

    /** Plays the ceremony for {@code stage} now (staff preview); nothing else changes. */
    public static boolean play(ServerPlayer p, int stage) {
        Origin o = AbyssOriginsAPI.getPlayerOrigin(p);
        if (o == null || active(p) || AtomicBlast.casting(p)) return false;
        start(p, o, Math.max(Ascension.NOBLE, Math.min(Ascension.LORD, stage)));
        return true;
    }

    /** True while {@code p} is in a ceremony (powers are blocked). */
    public static boolean active(Player p) {
        for (Ceremony c : ACTIVE) if (c.player.equals(p.getUUID())) return true;
        return false;
    }

    // ---- Tick ---------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!PENDING.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> it = PENDING.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> e = it.next();
                ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getKey());
                Origin o = p == null ? null : AbyssOriginsAPI.getPlayerOrigin(p);
                if (p == null || o == null) { it.remove(); continue; }          // logged out: the stage is kept, the show is skipped
                if (!p.isAlive() || p.isSleeping() || active(p) || AtomicBlast.casting(p)) continue;
                it.remove();
                start(p, o, e.getValue());
            }
        }
        if (ACTIVE.isEmpty()) return;
        Iterator<Ceremony> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Ceremony c = it.next();
            c.age++;
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(c.player);
            if (p == null || !p.isAlive() || p.level() != c.level) {
                if (p != null) Hold.set(p, HOLD, false);
                it.remove();
                continue;
            }
            try { run(c, p); }
            catch (Exception ex) { Abyssorigins.LOGGER.error("[AbyssOrigins] Ascension ceremony failed", ex); c.age = c.tl.total(); }
            if (c.age >= c.tl.release() && c.anchor != null) release(c, p);
            if (c.age >= c.tl.total()) { Hold.set(p, HOLD, false); it.remove(); }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.clear();
        PENDING.clear();
    }

    // ---- Ceremony -----------------------------------------------------------------------------

    private static void start(ServerPlayer p, Origin o, int stage) {
        Ceremony c = new Ceremony(p, o, stage);
        c.anchor = p.position();
        ACTIVE.add(c);
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, c.tl.rise(), 2, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, c.tl.total() + 20, 4, false, false, false));

        AscensionCinematic.play(p, o, stage, VIEW_RANGE);
        AbyssVfx.of(OriginVfx.ASCENSION).follow(p).at(p.position()).scale(stage).extra(o.getMotif().ordinal())
            .colors(c.c1(), c.c2()).duration(c.tl.total()).send(c.level, VIEW_RANGE);
    }

    private static void run(Ceremony c, ServerPlayer p) {
        Timeline tl = c.tl;
        if (c.age == tl.rise()) {
            p.removeEffect(MobEffects.LEVITATION);
            c.anchor = p.position();
            Hold.set(p, HOLD, true);
        }
        if (c.anchor != null && c.age < tl.rise()) {
            // Rising: free to go up, but nothing pushes them sideways
            double dx = p.getX() - c.anchor.x, dz = p.getZ() - c.anchor.z;
            if (dx * dx + dz * dz > 0.04) p.teleportTo(c.anchor.x, p.getY(), c.anchor.z);
        } else if (c.anchor != null && c.age < tl.release()) {
            p.setDeltaMovement(Vec3.ZERO);
            if (p.position().distanceToSqr(c.anchor) > 0.04) p.teleportTo(c.anchor.x, c.anchor.y, c.anchor.z);
        }
        p.fallDistance = 0;
        choreograph(c, p);
        if (c.age == tl.burst()) burst(c, p);
    }

    private static void release(Ceremony c, ServerPlayer p) {
        Hold.set(p, HOLD, false);
        c.anchor = null;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, false, false, true));
        p.fallDistance = 0;
    }

    /** The explosion: knockback, quake, heal, and the motif's blast effects. */
    private static void burst(Ceremony c, ServerPlayer p) {
        Vec3 chest = p.position().add(0, 1.0, 0);
        double reach = 8 * c.k();
        for (LivingEntity e : Targeting.enemiesAround(p, chest, reach)) {
            Vec3 d = e.position().subtract(p.position());
            Vec3 h = new Vec3(d.x, 0, d.z);
            h = h.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : h.normalize();
            double push = 1.6 * (1 - Math.min(1, d.length() / reach)) + 0.6;
            e.push(h.x * push, 0.45 + 0.2 * c.k(), h.z * push);
            e.hurtMarked = true;
        }
        AbyssCinematics.quake(c.level, chest, (float) (18 * c.k()), c.lord() ? 26 : 18, c.lord() ? 1f : 0.65f);

        p.setHealth(p.getMaxHealth());
        p.getData(OriginAttachments.PLAYER_ORIGIN.get()).setResource(100);
        p.getFoodData().eat(20, 1f);
        ModPackets.syncPlayer(p);

        AbyssVfx.of(VfxType.SHOCKWAVE).at(c.ground).scale(9 * c.k()).colors(c.c1(), c.c2()).duration(26).send(c.level, VIEW_RANGE);
        AbyssVfx.of(VfxType.BURST).at(chest).scale(1.6f * c.k()).colors(c.c1(), c.c2()).duration(16).send(c.level, VIEW_RANGE);
        sound(c, SoundEvents.GENERIC_EXPLODE, 1f, c.lord() ? 0.55f : 0.75f);

        int left = c.tl.total() - c.age;
        switch (c.origin.getMotif()) {
            case RADIANT -> {
                AbyssVfx.of(VfxType.PILLAR).at(c.ground).scale(28 * c.k()).extra(1.4f * c.k()).colors(c.c1(), c.c2()).duration(50).send(c.level, VIEW_RANGE);
                sound(c, SoundEvents.TOTEM_USE, 1f, 1f);
                sound(c, SoundEvents.PLAYER_LEVELUP, 1f, 0.6f);
            }
            case BLOOD -> {
                AbyssVfx.of(VfxType.DOME).at(c.ground).scale(6 * c.k()).colors(c.c1(), c.c2()).flags(AbyssVfx.HALF).duration(34).send(c.level, VIEW_RANGE);
                for (int i = 0; i < 3; i++) sound(c, SoundEvents.BAT_TAKEOFF, 1f, 0.6f + i * 0.2f);
                sound(c, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 1.5f);
            }
            case HELLFIRE -> {
                AbyssVfx.of(VfxType.PILLAR).at(c.ground).scale(22 * c.k()).extra(1.9f * c.k()).colors(c.c1(), c.c2()).duration(46).send(c.level, VIEW_RANGE);
                sound(c, SoundEvents.BLAZE_SHOOT, 1f, 0.5f);
                sound(c, c.lord() ? SoundEvents.WITHER_SPAWN : SoundEvents.RAVAGER_ROAR, 0.7f, 1.1f);
            }
            case DIVINE -> {
                AbyssVfx.of(VfxType.PILLAR).at(c.ground).scale(44 * c.k()).extra(1.2f * c.k()).colors(c.c2(), c.c1()).duration(56).send(c.level, VIEW_RANGE);
                sound(c, SoundEvents.BELL_BLOCK, 1f, 0.5f);
                sound(c, SoundEvents.BELL_RESONATE, 1f, 1f);
                sound(c, SoundEvents.BEACON_POWER_SELECT, 1f, 1.2f);
            }
            case VOID -> {
                AbyssVfx.of(VfxType.DOME).at(c.ground).scale(7 * c.k()).colors(c.c1(), c.c2()).flags(AbyssVfx.HALF | AbyssVfx.VOID).duration(40).send(c.level, VIEW_RANGE);
                sound(c, SoundEvents.WARDEN_SONIC_BOOM, 1f, 0.8f);
                sound(c, SoundEvents.END_PORTAL_SPAWN, 0.6f, 1.2f);
            }
            case SHADOW -> {
                AbyssVfx.of(VfxType.DOME).at(c.ground).scale(6 * c.k()).colors(c.c1(), c.c2()).flags(AbyssVfx.HALF | AbyssVfx.VOID).duration(36).send(c.level, VIEW_RANGE);
                AbyssVfx.of(VfxType.TENDRILS).at(c.ground).scale(5 * c.k()).extra(c.lord() ? 14 : 10).colors(c.c1(), c.c2()).duration(40).send(c.level, VIEW_RANGE);
                sound(c, SoundEvents.SCULK_SHRIEKER_SHRIEK, 1f, 0.8f);
                sound(c, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 0.6f);
            }
            case SOVEREIGN -> {
                for (int i = 0; i < 2; i++)
                    AbyssVfx.of(VfxType.SLASH).at(chest).dir(Vec3.directionFromRotation(0, p.getYRot() + 90 * i)).scale(3.2f * c.k()).extra(320)
                        .colors(c.c1(), c.c2()).flags(AbyssVfx.VERTICAL).duration(14).send(c.level, VIEW_RANGE);
                AbyssVfx.of(VfxType.PILLAR).at(c.ground).scale(30 * c.k()).extra(1.6f * c.k()).colors(c.c1(), c.c2()).duration(50).send(c.level, VIEW_RANGE);
                sound(c, SoundEvents.TRIDENT_THUNDER.value(), 1f, 0.8f);
                sound(c, SoundEvents.BEACON_POWER_SELECT, 1f, 0.6f);
            }
        }
        AbyssVfx.of(VfxType.AURA).follow(p).scale(1.3f * c.k()).colors(c.c1(), c.c2()).duration(Math.max(20, left))
            .flags(c.origin.getMotif() == Motif.SHADOW || c.origin.getMotif() == Motif.VOID ? AbyssVfx.VOID : 0).send(c.level, VIEW_RANGE);
    }

    /** What each motif adds while the energy gathers (the shared effect is OriginVfx.ASCENSION). */
    private static void choreograph(Ceremony c, ServerPlayer p) {
        int a = c.age, burst = c.tl.burst();
        if (a > burst) return;
        float k = c.k();
        Vec3 chest = p.position().add(0, 1.0, 0);
        if (a == 0) {
            AbyssVfx.of(VfxType.SIGIL).at(c.ground.add(0, 0.05, 0)).scale(3.4f * k).extra(c.lord() ? 8 : 6)
                .colors(c.c1(), c.c2()).duration(burst + 12).send(c.level, VIEW_RANGE);
            sound(c, SoundEvents.BEACON_ACTIVATE, 1f, 0.6f);
        }
        if (a == burst - 14) sound(c, SoundEvents.BEACON_POWER_SELECT, 0.8f, 0.5f);

        switch (c.origin.getMotif()) {
            case RADIANT -> {
                if (a == 6) AbyssVfx.of(VfxType.HELIX).follow(p).scale(2.8f * k).colors(c.c1(), c.c2()).duration(burst - 6).send(c.level, VIEW_RANGE);
                if (a == 24 && c.lord()) AbyssVfx.of(VfxType.HELIX).follow(p).scale(3.4f).colors(c.c2(), c.c1()).flags(AbyssVfx.REVERSE).duration(burst - 24).send(c.level, VIEW_RANGE);
                if (a % 16 == 8) sound(c, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.6f + 0.8f * a / burst);
            }
            case BLOOD -> {
                if (a == 24) AbyssVfx.of(VfxType.WINGS).follow(p).scale(1.5f * k).colors(c.c1(), c.c2()).flags(AbyssVfx.VOID).duration(c.tl.total() - 24).send(c.level, VIEW_RANGE);
                // A heartbeat that quickens toward the burst
                if (a >= c.nextBeat) {
                    sound(c, SoundEvents.WARDEN_HEARTBEAT, 1.2f, 0.9f + 0.3f * a / burst);
                    c.nextBeat = a + Math.max(5, Math.round(22 - 17f * a / burst));
                }
                if (a == c.tl.rise()) sound(c, SoundEvents.BAT_AMBIENT, 1f, 0.6f);
            }
            case HELLFIRE -> {
                if (a == 12) AbyssVfx.of(VfxType.SIGIL).at(c.ground.add(0, 0.08, 0)).scale(5.2f * k).extra(5)
                    .colors(c.c1(), c.c2()).duration(burst - 4).send(c.level, VIEW_RANGE);
                if (a % 10 == 0) sound(c, SoundEvents.BLAZE_BURN, 0.9f, 0.6f + 0.5f * a / burst);
                if (a % 24 == 12) sound(c, SoundEvents.FIRECHARGE_USE, 0.8f, 0.7f);
            }
            case DIVINE -> {
                if (a == 20) AbyssVfx.of(VfxType.WINGS).follow(p).scale(1.6f * k).colors(c.c1(), c.c2()).duration(c.tl.total() - 20).send(c.level, VIEW_RANGE);
                if (a == c.tl.rise() + 10 || (c.lord() && a == c.tl.rise() + 40)) {
                    for (int i = 0; i < 4; i++) {
                        double ang = Math.toRadians(p.getYRot() + 45 + 90 * i);
                        Vec3 sky = c.ground.add(Math.cos(ang) * 6, 36, Math.sin(ang) * 6);
                        AbyssVfx.beam(c.level, sky, chest, 0.35f * k, c.c2(), c.c1(), 34);
                    }
                }
                if (a % 14 == 0) sound(c, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.8f + 0.8f * a / burst);
                if (a == 0) sound(c, SoundEvents.BELL_BLOCK, 0.8f, 1.2f);
            }
            case VOID -> {
                if (a == 14) AbyssVfx.of(VfxType.VORTEX).at(p.position().add(0, 3.6 * k, 0)).scale(2.2f * k).colors(c.c1(), c.c2()).duration(burst - 14).send(c.level, VIEW_RANGE);
                if (a == 30 && c.lord()) {
                    Vec3 back = Vec3.directionFromRotation(0, p.getYRot()).scale(-2.4);
                    AbyssVfx.of(VfxType.RIFT).at(c.ground.add(back)).dir(Vec3.directionFromRotation(0, p.getYRot())).scale(5)
                        .colors(c.c1(), c.c2()).duration(c.tl.total() - 30).send(c.level, VIEW_RANGE);
                }
                if (a % 30 == 0) sound(c, SoundEvents.PORTAL_AMBIENT, 0.9f, 0.6f + 0.6f * a / burst);
            }
            case SHADOW -> {
                if (a == 8 || a == burst / 2) AbyssVfx.of(VfxType.TENDRILS).at(c.ground).scale(3.4f * k).extra(8)
                    .colors(c.c1(), c.c2()).duration(56).send(c.level, VIEW_RANGE);
                if (a == 10) AbyssVfx.of(VfxType.AURA).follow(p).scale(1.6f * k).colors(c.c1(), c.c2()).flags(AbyssVfx.VOID).duration(burst - 10).send(c.level, VIEW_RANGE);
                if (a % 14 == 0) sound(c, SoundEvents.SOUL_ESCAPE.value(), 1f, 0.5f + 0.5f * a / burst);
                if (a == 0) sound(c, SoundEvents.AMBIENT_CAVE.value(), 1f, 0.8f);
            }
            case SOVEREIGN -> {
                if (a == 10) AbyssVfx.of(VfxType.SIGIL).at(c.ground.add(0, 0.08, 0)).scale(6f * k).extra(12)
                    .colors(c.c1(), c.c2()).duration(burst - 6).send(c.level, VIEW_RANGE);
                if (a == 16) AbyssVfx.of(VfxType.HELIX).follow(p).scale(3f * k).colors(c.c2(), c.c1()).duration(burst - 16).send(c.level, VIEW_RANGE);
                if (a % 20 == 0) sound(c, SoundEvents.BEACON_AMBIENT, 1f, 0.6f + 0.6f * a / burst);
            }
        }
    }

    private static void sound(Ceremony c, SoundEvent s, float volume, float pitch) {
        ServerPlayer p = c.level.getServer().getPlayerList().getPlayer(c.player);
        Vec3 at = p != null ? p.position() : c.ground;
        Fx.sound(c.level, at, s, volume * 2.5f, pitch);
    }

    private static void sound(Ceremony c, Holder<SoundEvent> s, float volume, float pitch) {
        sound(c, s.value(), volume, pitch);
    }
}
