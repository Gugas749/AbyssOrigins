package com.gugas749.abyssorigins.client.eminence;

import com.gugas749.abysscore.api.cinematic.client.AbyssCinematicsClient;
import com.gugas749.abyssorigins.item.gear.SovereignScytheItem;
import com.gugas749.abyssorigins.origin.eminence.AtomicCinematic;
import com.gugas749.abyssorigins.origin.eminence.EminenceAbilities.Atomic;
import com.gugas749.abyssorigins.origin.eminence.ScytheCombo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * How the Sovereign Scythe is carried and swung - third person (arm poses through PlayerModelMixin, the
 * scythe itself through ItemInHandLayerMixin) and first person (ScytheClientExtensions).
 *
 * At rest it lies over the right shoulder, gripped low in front of the chest, the blade hanging behind the
 * head. Each swing of the chain ({@link ScytheCombo}) plays its own move: a flat sweep right-to-left, an
 * overhead chop, a rising backhand reap. Holding use winds it back over the head for a throw.
 *
 * The scythe's orientation is given in the wielder's body frame (yaw / pitch / roll of its shaft, see
 * {@link #orient}) rather than relative to the arm: the arm only decides where the hand is.
 */
public final class ScytheAnimations {

    /** Length of one swing of the chain, in ticks. */
    public static final float SWING_TICKS = 12f;
    /** Rendered size of the scythe model (it is authored large: ~4 blocks at 1.0). */
    public static final float SCALE = 0.7f;
    /** Where the hand grips the shaft, in model pixels (0 = between the two grip wraps). */
    public static final float GRIP = -9f;

    /**
     * One moment of a move.
     * arm: right arm x/y/z rotation (radians); left: left arm (null = left alone); body: torso twist;
     * yaw/pitch/roll: the scythe in the body frame (degrees).
     */
    public record Pose(float ax, float ay, float az, float[] left, float body, float yaw, float pitch, float roll) {
        static Pose of(float axDeg, float ayDeg, float azDeg, float[] leftDeg, float bodyDeg, float yaw, float pitch, float roll) {
            float[] l = leftDeg == null ? null : new float[]{ rad(leftDeg[0]), rad(leftDeg[1]), rad(leftDeg[2]) };
            return new Pose(rad(axDeg), rad(ayDeg), rad(azDeg), l, rad(bodyDeg), yaw, pitch, roll);
        }
    }

    /** A keyframe; a null pose is the shoulder rest. */
    private record Key(float t, Pose pose) {
        Pose get() { return pose != null ? pose : REST; }
    }

    // ---- Third person -------------------------------------------------------------------------

    /** Over the right shoulder: shaft up and back, the blade hanging across the back. */
    static final Pose REST = Pose.of(-52, -26, 0, null, 0, 155, 46, 115);

    private static final Key[][] MOVES = {
        { // Reaping Sweep - flat, right to left
            new Key(0f, null),
            new Key(0.28f, Pose.of(-80, 40, 15, new float[]{ -70, 60, 0 }, 22, 80, 8, -90)),
            new Key(0.55f, Pose.of(-88, -8, 0, new float[]{ -80, 20, 0 }, -8, -10, 0, -90)),
            new Key(0.8f, Pose.of(-70, -55, -10, new float[]{ -60, -20, 0 }, -28, -100, -6, -90)),
            new Key(1f, null) },
        { // Sovereign Strike - overhead chop
            new Key(0f, null),
            new Key(0.3f, Pose.of(-168, -8, 0, new float[]{ -160, 15, 0 }, 0, 0, 120, 180)),
            new Key(0.55f, Pose.of(-62, -6, 0, new float[]{ -55, 20, 0 }, 0, 0, -12, 180)),
            new Key(0.78f, Pose.of(-34, -6, 0, new float[]{ -30, 20, 0 }, 0, 0, -35, 180)),
            new Key(1f, null) },
        { // Harvest - rising backhand reap, low left to high right
            new Key(0f, null),
            new Key(0.28f, Pose.of(-30, -50, -10, new float[]{ -40, -10, 0 }, -25, -100, -30, 90)),
            new Key(0.55f, Pose.of(-110, 10, 20, new float[]{ -100, 40, 0 }, 10, 0, 25, 90)),
            new Key(0.8f, Pose.of(-150, 40, 30, new float[]{ -130, 60, 0 }, 25, 80, 55, 90)),
            new Key(1f, null) },
    };

    /** Winding up a throw: raised behind the head, blade high. */
    static final Pose THROW = Pose.of(-165, 5, 10, null, -8, 175, 70, 0);

    /** Length of the I Am Atomic move (from the cast) after which the scythe is back at rest, in ticks. */
    static final float ATOMIC_TICKS = 120f;

    /**
     * I Am Atomic ({@link Atomic}): raised straight overhead while the atom gathers, wound back, then cut down
     * through the atom at Atomic.CUT and held low through the explosion. Key times are ticks / ATOMIC_TICKS.
     */
    private static final Key[] ATOMIC = {
        new Key(0f, null),
        new Key(Atomic.RAISED / ATOMIC_TICKS, Pose.of(-172, -4, 0, new float[]{ -165, 12, 0 }, 0, 0, 100, 180)),
        new Key(Atomic.SWING / ATOMIC_TICKS, Pose.of(-172, -4, 0, new float[]{ -165, 12, 0 }, 0, 0, 100, 180)),
        new Key((Atomic.SWING + 3) / ATOMIC_TICKS, Pose.of(-178, -6, 0, new float[]{ -170, 14, 0 }, 0, 0, 128, 180)),
        new Key(Atomic.CUT / ATOMIC_TICKS, Pose.of(-25, -6, 0, new float[]{ -22, 20, 0 }, 0, 0, -55, 180)),
        new Key(96 / ATOMIC_TICKS, Pose.of(-20, -10, 0, new float[]{ -18, 18, 0 }, 5, 0, -60, 180)),
        new Key(1f, null) };

    /** Last pose computed per entity (setupAnim -> item layer, same frame). */
    private static final Map<LivingEntity, Pose> CURRENT = new WeakHashMap<>();

    private ScytheAnimations() {}

    public static boolean holds(LivingEntity e) {
        return e.getMainHandItem().getItem() instanceof SovereignScytheItem;
    }

    /** Progress 0..1 of the current swing, or -1 between swings. */
    public static float swingProgress(LivingEntity e, float partial) {
        float t = (e.level().getGameTime() - ScytheCombo.started(e) + partial) / SWING_TICKS;
        return t >= 0 && t < 1 ? t : -1;
    }

    /** The pose {@code e} is in right now. */
    public static Pose poseOf(LivingEntity e, float partial) {
        float atomic = AbyssCinematicsClient.age(AtomicCinematic.id(e.getId()), partial);
        if (atomic >= 0) {
            Pose p = sample(ATOMIC, Math.min(1f, atomic / ATOMIC_TICKS));
            if (atomic >= Atomic.RAISED && atomic < Atomic.SWING) {      // the raised scythe trembles with the charge
                float s = 1.5f * (atomic - Atomic.RAISED) / (Atomic.SWING - Atomic.RAISED);
                p = new Pose(p.ax(), p.ay(), p.az(), p.left(), p.body(), p.yaw() + s * Mth.sin(atomic * 2.3f),
                    p.pitch() + s * Mth.sin(atomic * 1.7f + 1f), p.roll());
            }
            return p;
        }
        if (e.isUsingItem() && e.getUseItem().getItem() instanceof SovereignScytheItem) {
            float c = Math.min(1f, (e.getTicksUsingItem() + partial) / SovereignScytheItem.FULL_CHARGE);
            return blend(REST, THROW, ease(c));
        }
        float p = swingProgress(e, partial);
        return p < 0 ? REST : sample(MOVES[ScytheCombo.stage(e)], p);
    }

    /** Poses a player model holding the scythe (end of setupAnim). */
    public static void poseModel(HumanoidModel<?> m, LivingEntity e, float age) {
        if (!holds(e)) {
            CURRENT.remove(e);
            return;
        }
        float partial = age - (float) Math.floor(age);
        Pose pose = poseOf(e, partial);
        CURRENT.put(e, pose);
        boolean right = e.getMainArm() == HumanoidArm.RIGHT;
        float mirror = right ? 1 : -1;
        ModelPart main = right ? m.rightArm : m.leftArm, off = right ? m.leftArm : m.rightArm;

        float crouch = m.crouching ? 0.4f : 0f;
        float bob = Mth.cos(age * 0.09f) * 0.03f;
        m.body.yRot = pose.body() * mirror;
        float by = m.body.yRot;
        // Keep the shoulders on the twisted torso
        m.rightArm.z = Mth.sin(by) * 5f;
        m.rightArm.x = -Mth.cos(by) * 5f;
        m.leftArm.z = -Mth.sin(by) * 5f;
        m.leftArm.x = Mth.cos(by) * 5f;

        main.xRot = pose.ax() + crouch + bob;
        main.yRot = pose.ay() * mirror + by;
        main.zRot = pose.az() * mirror;
        if (pose.left() != null) {
            off.xRot = pose.left()[0] + crouch;
            off.yRot = pose.left()[1] * mirror + by;
            off.zRot = pose.left()[2] * mirror;
        } else if (by != 0) {
            off.yRot += by;
        }
    }

    /**
     * Item layer, third person: replaces the hand's rotation with the scythe's own orientation in the body
     * frame. {@code pose} is at the hand, after vanilla's translateToHand / rotate / offset.
     */
    public static void thirdPerson(LivingEntity e, ModelPart arm, boolean leftHand, PoseStack pose) {
        Pose p = CURRENT.getOrDefault(e, REST);
        // Undo arm rotation and vanilla's hand rotations (X -90, Y 180): back to model axes at the hand
        Quaternionf handRot = new Quaternionf().rotationZYX(arm.zRot, arm.yRot, arm.xRot)
            .rotateX((float) -Math.PI / 2).rotateY((float) Math.PI);
        pose.mulPose(handRot.invert());
        // Model axes: X = wielder's left, Y = down, Z = back
        pose.mulPose(orient(p.yaw(), p.pitch(), p.roll(), leftHand, new Vector3f(-1, 0, 0), new Vector3f(0, -1, 0), new Vector3f(0, 0, -1)));
        applyGrip(pose);
    }

    /** Scale the model and put the grip at the origin. */
    public static void applyGrip(PoseStack pose) {
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(0, -GRIP / 16f, 0);
    }

    /**
     * Rotation taking the scythe model (shaft along +Y, blade toward -X) to: shaft pointing {@code yaw}° to the
     * right of forward, {@code pitch}° up, blade rolled {@code roll}° around the shaft (0 = blade up when the
     * shaft is level; 90 = blade to the right). {@code right/up/fwd} are the frame's axes.
     */
    public static Quaternionf orient(float yaw, float pitch, float roll, boolean mirror, Vector3f right, Vector3f up, Vector3f fwd) {
        if (mirror) { yaw = -yaw; roll = -roll; }
        float y = rad(yaw), p = rad(pitch), r = rad(roll);
        // In (R, U, F) coordinates: shaft d starts at F, blade b at U
        Vector3f d = new Vector3f(0, 0, 1), b = new Vector3f(0, 1, 0);
        Quaternionf q = new Quaternionf()
            .rotateY(y)                    // yaw about U, positive turns F toward R
            .rotateX(-p)                   // pitch about R, positive raises F toward U
            .rotateZ(-r);                  // roll about F, positive turns U toward R
        // (R,U,F) mapped straight onto JOML's (x,y,z) is left-handed, hence the flipped pitch/roll signs.
        q.transform(d);
        q.transform(b);
        Vector3f geoY = toFrame(d, right, up, fwd);
        Vector3f geoX = toFrame(b, right, up, fwd).negate();
        Vector3f geoZ = new Vector3f(geoX).cross(geoY).normalize();
        geoX = new Vector3f(geoY).cross(geoZ).normalize();
        Matrix3f m = new Matrix3f().setColumn(0, geoX).setColumn(1, geoY).setColumn(2, geoZ);
        return new Quaternionf().setFromNormalized(m);
    }

    private static Vector3f toFrame(Vector3f v, Vector3f right, Vector3f up, Vector3f fwd) {
        return new Vector3f(right).mul(v.x).add(new Vector3f(up).mul(v.y)).add(new Vector3f(fwd).mul(v.z)).normalize();
    }

    // ---- Keyframes ----------------------------------------------------------------------------

    private static Pose sample(Key[] keys, float t) {
        for (int i = 0; i < keys.length - 1; i++) {
            Key a = keys[i], b = keys[i + 1];
            if (t <= b.t()) return blend(a.get(), b.get(), ease((t - a.t()) / (b.t() - a.t())));
        }
        return keys[keys.length - 1].get();
    }

    static Pose blend(Pose a, Pose b, float t) {
        float[] l;
        if (a.left() == null && b.left() == null) l = null;
        else {
            float[] la = a.left() != null ? a.left() : new float[3], lb = b.left() != null ? b.left() : new float[3];
            l = new float[]{ Mth.lerp(t, la[0], lb[0]), Mth.lerp(t, la[1], lb[1]), Mth.lerp(t, la[2], lb[2]) };
        }
        return new Pose(Mth.lerp(t, a.ax(), b.ax()), Mth.lerp(t, a.ay(), b.ay()), Mth.lerp(t, a.az(), b.az()), l,
            Mth.lerp(t, a.body(), b.body()), Mth.rotLerp(t, a.yaw(), b.yaw()), Mth.lerp(t, a.pitch(), b.pitch()), Mth.rotLerp(t, a.roll(), b.roll()));
    }

    static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    static float rad(float deg) { return deg * Mth.DEG_TO_RAD; }

    /** Used by first-person and entity renderers. */
    public static boolean isScythe(ItemStack s) { return s.getItem() instanceof SovereignScytheItem; }
}
