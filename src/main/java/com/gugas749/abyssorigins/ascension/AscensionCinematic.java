package com.gugas749.abyssorigins.ascension;

import com.gugas749.abysscore.api.cinematic.AbyssCinematic;
import com.gugas749.abysscore.api.cinematic.AbyssCinematics;
import com.gugas749.abysscore.api.cinematic.CinematicText;
import com.gugas749.abysscore.api.cinematic.Ease;
import com.gugas749.abysscore.api.cinematic.Keyframes;
import com.gugas749.abysscore.api.sky.AbyssSky;
import com.gugas749.abysscore.api.sky.AbyssSkySpec;
import com.gugas749.abyssorigins.api.Ascension;
import com.gugas749.abyssorigins.api.Ascension.Motif;
import com.gugas749.abyssorigins.api.Ascension.Timeline;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.config.AbyssOriginsServerConfig;
import com.gugas749.abyssorigins.network.packet.AscensionPacket;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The ascension ceremony's cinematic and sky, on AbyssCore's engines (played by AscensionCeremony).
 *
 *  - The ascending player: the HUD is hidden (chat stays), letterbox bars and a vignette in the origin's
 *    colour close in, the stage's line appears in the bottom bar while the energy gathers and the screen
 *    flashes at the burst. The camera flies around them in third person on the motif's path ({@link Path})
 *    and returns to their own view at the end; their view direction is held still until release. The new
 *    title ("Noble Vampire") is drawn by the client's AscensionTitle, sent with {@link AscensionPacket}.
 *  - Everyone else nearby: the ascending player is drawn full-bright until released.
 *  - The ascending player's sky takes the origin's ascension colour (deeper at Lord; the Eminence gets a night
 *    sky full of stars) from the start of the ceremony until two minutes after it.
 */
public final class AscensionCinematic {

    public static final String ID = "abyssorigins:ascension";
    /** How long the sky stays after the ceremony ends: 2 minutes. */
    private static final int AFTERGLOW = 2 * 60 * 20;
    /** Camera blend-in at the start and back to the player's own view at the end, in ticks. */
    private static final int BLEND_IN = 18, BLEND_OUT = 18, END = 4;

    /**
     * Camera path, angles relative to the player's facing: it starts {@code yaw0} degrees around (180 = in
     * front, facing them) and travels {@code sweep} degrees by release. Pitch (positive looks down from above)
     * and distance go start -> at the burst -> at release.
     */
    private record Path(float yaw0, float sweep, float pitch0, float pitchBurst, float pitchEnd,
                        float dist0, float distBurst, float distEnd) {}

    private static Path path(Motif m) {
        return switch (m) {
            case RADIANT   -> new Path(150, 200, 12, -12, 8, 5f, 6.5f, 9f);
            case BLOOD     -> new Path(200, 320, 8, 22, 12, 4.5f, 5.5f, 8.5f);
            case HELLFIRE  -> new Path(170, 140, 16, -14, -4, 7f, 5.5f, 10f);
            case DIVINE    -> new Path(180, 180, 35, -28, -10, 5f, 7f, 10f);
            case VOID      -> new Path(180, 380, 18, 4, 12, 9f, 4f, 11f);
            case SHADOW    -> new Path(135, -200, 28, 12, 30, 6.5f, 4.5f, 9f);
            case SOVEREIGN -> new Path(180, 240, 2, -12, 15, 5.5f, 6.5f, 12f);
        };
    }

    private AscensionCinematic() {}

    /** Plays it for {@code p} ascending into {@code stage}; players within {@code range} see them glow. */
    public static void play(ServerPlayer p, Origin o, int stage, double range) {
        Timeline tl = Ascension.timeline(stage);
        AbyssCinematics.play(p, build(p, o, stage, tl));
        PacketDistributor.sendToPlayer(p, new AscensionPacket(o.getId().toString(), stage));

        double r2 = range * range;
        List<ServerPlayer> others = p.serverLevel().players().stream()
            .filter(v -> v != p && v.distanceToSqr(p) <= r2).toList();
        if (!others.isEmpty())
            AbyssCinematics.play(others, AbyssCinematic.builder(ID + "_glow/" + p.getId(), tl.total())
                .focus(p).fullBright(0, tl.release()).build());

        if (AbyssOriginsServerConfig.ASCENSION_SKY.get()) AbyssSky.set(p, sky(o, stage), tl.total() + AFTERGLOW);
    }

    private static AbyssSkySpec sky(Origin o, int stage) {
        if (Eminence.is(o)) return AbyssSkySpec.solid(0x030309, 0x06060F, 0x29263D).withStars(1).withStarField(1);
        return AbyssSkySpec.tint(o.getAscensionColor(), stage >= Ascension.LORD ? 0.95f : 0.8f);
    }

    private static AbyssCinematic build(ServerPlayer p, Origin o, int stage, Timeline tl) {
        boolean lord = stage >= Ascension.LORD;
        int total = tl.total(), burst = tl.burst(), release = tl.release();
        int color = o.getAscensionColor();
        Path path = path(o.getMotif());

        // Camera: orbit on the motif's path, a wider sweep and distance for a Lord; at the burst it jolts
        // and is thrown back a little (sampled: a quick kick that settles while the camera eases out)
        float sweep = path.sweep * (lord ? 1.35f : 1f), scale = lord ? 1.2f : 1f;
        Keyframes.Builder distance = Keyframes.start(path.dist0 * scale).to(burst, path.distBurst * scale, Ease.SMOOTH);
        float span = release - burst;
        for (int d = 2; d < span; d += 3) {
            float base = Mth.lerp(easeOut(d / span), path.distBurst, path.distEnd);
            float kick = 2.5f * (float) Math.exp(-d / 6f) * Math.min(1, d / 2f);
            distance.to(burst + d, (base + kick) * scale);
        }
        distance.to(release, path.distEnd * scale);
        float jolt = lord ? 2.2f : 1.4f;

        return AbyssCinematic.builder(ID, total)
            .focus(p)
            .hideHud(4, total - 10)
            .lockView(0, release)
            .fullBright(0, release)
            // The world darkens while the energy gathers, washed in the origin's colour; the burst lights it again
            .fill(0x07050F, Keyframes.start(0).to(30, 0.38f, Ease.SMOOTH).to(burst, 0.38f).to(burst + 10, 0, Ease.SMOOTH).build())
            .fill(color, Keyframes.window(0, total - END, 12, 18, 0.08f))
            .vignette(0x000000, 1 / 3f, Keyframes.window(0, total - END, 12, 18, 0.85f))
            .vignette(mix(color, 0x000000, 0.55f), 0.2f, Keyframes.window(0, total - END, 12, 18, 0.6f))
            .fill(mix(0xFFFFFF, color, 0.25f), Keyframes.start(0).to(burst, 0).to(burst, 0.95f).hold(burst + 2).to(burst + 16, 0, Ease.OUT).build())
            .letterbox(Keyframes.window(0, total - END, 12, 18, 0.12f))
            .text(CinematicText.translatable(Ascension.lineKey(o, stage)), AbyssCinematic.TextAnchor.SUBTITLE,
                mix(color, 0xFFFFFF, 0.55f), 1.4f, 16, burst - 4, 5)
            .orbit(Keyframes.start(path.yaw0).to(release, path.yaw0 + sweep, Ease.IN_OUT).build(),
                Keyframes.start(path.pitch0).to(burst, path.pitchBurst, Ease.SMOOTH).to(release, path.pitchEnd, Ease.SMOOTH).build(),
                distance.build(),
                Keyframes.window(0, total - END, BLEND_IN, BLEND_OUT, 1))
            .shake(Keyframes.startAt(burst, 0).to(burst + 1, jolt).to(burst + 20, 0).build(), 0.8f)
            .fov(Keyframes.start(1).to(burst, 0.92f, Ease.SMOOTH).to(burst + 3, 1.12f).to(burst + 33, 1, Ease.SMOOTH).build())
            .build();
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(Mth.lerp(t, a >> 16 & 0xFF, b >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, a >> 8 & 0xFF, b >> 8 & 0xFF));
        int bl = Math.round(Mth.lerp(t, a & 0xFF, b & 0xFF));
        return r << 16 | g << 8 | bl;
    }

    private static float easeOut(float t) {
        t = Mth.clamp(t, 0, 1);
        float u = 1 - t;
        return 1 - u * u * u;
    }
}
