package com.gugas749.abyssorigins.origin.eminence;

import com.gugas749.abysscore.api.cinematic.AbyssCinematic;
import com.gugas749.abysscore.api.cinematic.AbyssCinematics;
import com.gugas749.abysscore.api.cinematic.CinematicText;
import com.gugas749.abysscore.api.cinematic.Ease;
import com.gugas749.abysscore.api.cinematic.Keyframes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static com.gugas749.abyssorigins.origin.eminence.EminenceAbilities.Atomic.CUT;
import static com.gugas749.abyssorigins.origin.eminence.EminenceAbilities.Atomic.DETONATE;
import static com.gugas749.abyssorigins.origin.eminence.EminenceAbilities.Atomic.SWING;
import static com.gugas749.abyssorigins.origin.eminence.EminenceAbilities.Atomic.TOTAL;

/**
 * I Am Atomic's cinematic, on AbyssCore's engine. One id per caster ({@link #id}), sent to everyone near the
 * cast, so every client can time the caster's scythe pose with {@code AbyssCinematicsClient.age(id(caster))}
 * (ScytheAnimations, ScytheFirstPerson).
 *
 *  - Players within the blast radius (and the caster): the HUD is hidden (chat stays), the view darkens,
 *    letterbox bars and a vignette close in, the subtitles "I am..." / "Atomic." appear in the bottom bar, the
 *    screen flashes white at the cut and the explosion, and the camera trembles and zooms in while the atom
 *    gathers, then punches out on detonation (the explosion's shake is a quake, AbyssCinematics.quake).
 *  - The caster's camera switches to third person (front), pulled back so the raised scythe, the atom and the
 *    blast fit in view, and returns {@link #CAMERA_RESTORE} ticks before the end.
 *  - Everyone who gets it draws the caster full-bright while casting.
 */
public final class AtomicCinematic {

    /** Ticks before the end at which the caster's camera is restored. */
    private static final int CAMERA_RESTORE = 30;
    private static final int TEXT = 0xE4DAFF;

    private AtomicCinematic() {}

    public static String id(int casterId) { return "abyssorigins:atomic/" + casterId; }

    /** Players within {@code radius} of {@code c} get the whole cinematic, the rest up to {@code range} the glow. */
    public static void play(ServerPlayer caster, Vec3 c, double radius, double range) {
        List<ServerPlayer> inside = new ArrayList<>(), outside = new ArrayList<>();
        for (ServerPlayer p : caster.serverLevel().players()) {
            double d = p.position().distanceTo(c);
            if (p == caster || d <= radius) inside.add(p);
            else if (d <= range) outside.add(p);
        }
        int glow = TOTAL - CAMERA_RESTORE;
        AbyssCinematics.play(inside, cinematic(caster).build());
        if (!outside.isEmpty())
            AbyssCinematics.play(outside, AbyssCinematic.builder(id(caster.getId()), TOTAL).focus(caster).fullBright(0, glow).build());
    }

    private static AbyssCinematic.Builder cinematic(ServerPlayer caster) {
        int glow = TOTAL - CAMERA_RESTORE;
        return AbyssCinematic.builder(id(caster.getId()), TOTAL)
            .focus(caster)
            .hideHud(4, TOTAL - 20)
            .fullBright(0, glow)
            // The caster: front view, pulled back to 10 blocks, then 15 as the blast grows
            .camera(AbyssCinematic.CameraMode.THIRD_PERSON_FRONT, 0, glow)
            .pullBack(Keyframes.start(10).to(DETONATE, 10).to(DETONATE + 20, 15, Ease.SMOOTH).build(),
                Keyframes.window(0, glow, 15, 20, 1))
            // The world darkens while the atom gathers; the blast lights it again in violet
            .fill(0x0A0618, Keyframes.start(0).to(30, 0.45f, Ease.SMOOTH).to(DETONATE, 0.45f).to(DETONATE + 8, 0, Ease.SMOOTH).build())
            .fill(0x3A1E8A, Keyframes.start(0).to(DETONATE, 0).to(DETONATE, 0.22f).to(DETONATE + 60, 0, Ease.SMOOTH).build())
            .vignette(0x000000, 1 / 3f, Keyframes.window(0, TOTAL, 12, 25, 0.85f))
            .vignette(0x2A1260, 0.2f, Keyframes.window(0, TOTAL, 12, 25, 0.55f))
            // A short flash at the cut, a full white-out at the explosion
            .fill(0xF6F2FF, Keyframes.start(0).to(CUT, 0).to(CUT, 0.4f).to(CUT + 5, 0).build())
            .fill(0xF6F2FF, Keyframes.start(0).to(DETONATE, 0).to(DETONATE, 1).hold(DETONATE + 2).to(DETONATE + 18, 0, Ease.OUT).build())
            .letterbox(Keyframes.window(0, TOTAL, 12, 25, 0.12f))
            .text(CinematicText.translatable("abyssorigins.atomic.line1"), AbyssCinematic.TextAnchor.SUBTITLE, TEXT, 1.6f, 16, CUT - 3, 4)
            .text(CinematicText.translatable("abyssorigins.atomic.line2"), AbyssCinematic.TextAnchor.SUBTITLE, TEXT, 1.6f, CUT - 3, DETONATE + 40, 4)
            // Tremor while the atom gathers (stronger as it grows); zoom in, then punch out on detonation
            .shake(Keyframes.start(0.15f).to(SWING, 0.9f, Ease.SMOOTH).to(DETONATE, 0.9f).to(DETONATE, 0).build(), 1f)
            .fov(Keyframes.start(1).to(SWING, 0.9f, Ease.SMOOTH).to(DETONATE, 0.9f).to(DETONATE + 3, 1.15f)
                .to(DETONATE + 33, 1, Ease.SMOOTH).build());
    }
}
