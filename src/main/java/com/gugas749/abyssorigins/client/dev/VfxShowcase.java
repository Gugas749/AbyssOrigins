package com.gugas749.abyssorigins.client.dev;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.client.gui.OriginInfoScreen;
import com.gugas749.abyssorigins.client.gui.skilltree.SkillTreeScreen;
import com.gugas749.abyssorigins.client.vfx.VfxManager;
import com.gugas749.abyssorigins.vfx.Vfx;
import com.gugas749.abyssorigins.vfx.VfxSpec;
import com.gugas749.abyssorigins.vfx.VfxType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * DEVELOPER TOOL — inert unless the JVM is started with -Dabyssorigins.vfxShowcase=true
 * (gradle task: runVfxShowcase).
 *
 * Creates/opens a flat night world, stages every VFX type in front of the camera,
 * captures screenshots to run/screenshots/abyss_*.png, captures the HUD and screens,
 * then quits. Lets the visuals be reviewed without playing.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public final class VfxShowcase {

    private static final boolean ENABLED = Boolean.getBoolean("abyssorigins.vfxShowcase");
    private static final String WORLD = "abyss_vfx_showcase";
    private static final Vec3 STAGE = new Vec3(0.5, -60, 6.5);

    private record Step(String name, int delay, Consumer<Minecraft> action) {}

    private static final List<Step> STEPS = new ArrayList<>();
    private static int phase = 0, timer = 0, stepIndex = 0;

    private VfxShowcase() {}

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        switch (phase) {
            case 0 -> {   // wait for the title screen, then enter the world
                if (mc.getOverlay() != null) return;
                if (mc.screen instanceof AccessibilityOnboardingScreen) {   // fresh run dir: skip first-launch onboarding
                    mc.options.onboardAccessibility = false;
                    mc.options.save();
                    mc.setScreen(new TitleScreen());
                    return;
                }
                if (!(mc.screen instanceof TitleScreen)) return;
                mc.options.pauseOnLostFocus = false;
                if (mc.getLevelSource().levelExists(WORLD)) {
                    mc.createWorldOpenFlows().openWorld(WORLD, () -> {});
                } else {
                    LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                        true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(1234L, false, false),
                        reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        mc.screen);
                }
                phase = 1;
            }
            case 1 -> {   // in world: stage the scene
                if (mc.level == null || mc.player == null || mc.screen != null) return;
                if (++timer < 60) return;
                cmd(mc, "time set 18000");
                cmd(mc, "weather clear");
                cmd(mc, "gamerule doDaylightCycle false");
                cmd(mc, "gamerule doMobSpawning false");
                cmd(mc, "kill @e[type=!player]");
                cmd(mc, "tp @s 0.5 -60 0.5 0 14");
                cmd(mc, "summon armor_stand -1.6 -60 6.5 {Rotation:[180f,0f],NoGravity:1b,ShowArms:1b}");
                cmd(mc, "summon armor_stand 2.6 -60 6.5 {Rotation:[180f,0f],NoGravity:1b,ShowArms:1b}");
                mc.options.hideGui = true;
                buildSteps();
                phase = 2;
                timer = 0;
            }
            case 2 -> {   // run steps
                if (stepIndex >= STEPS.size()) { phase = 3; return; }
                Step s = STEPS.get(stepIndex);
                if (timer == 0) s.action().accept(mc);
                if (++timer >= s.delay()) {
                    if (!s.name().isEmpty()) shot(mc, s.name());
                    timer = 0;
                    stepIndex++;
                }
            }
            case 3 -> {
                Abyssorigins.LOGGER.info("[AbyssOrigins] VFX showcase complete.");
                mc.stop();
                phase = 4;
            }
            default -> {}
        }
    }

    private static void buildSteps() {
        STEPS.clear();
        // name = screenshot taken after `wait` ticks; "" = setup step without a shot
        add("shockwave", 7, mc -> spawn(VfxType.SHOCKWAVE, STAGE, Vec3.ZERO, 5, 0, 0xFF6A1F, 0xFFD27A, 24, -1, 0));
        add("dome", 20, mc -> spawn(VfxType.DOME, STAGE, Vec3.ZERO, 3.2f, 0, 0xF6D77A, 0xFFF4C8, 60, -1, Vfx.HALF));
        add("pillar_sigil", 14, mc -> {
            spawn(VfxType.PILLAR, STAGE, Vec3.ZERO, 8, 1.0f, 0xF6D77A, 0xFFFFFF, 40, -1, 0);
            spawn(VfxType.SIGIL, STAGE, Vec3.ZERO, 3.5f, 6, 0xF6D77A, 0xFFF0B0, 50, -1, 0);
        });
        add("burst", 4, mc -> spawn(VfxType.BURST, STAGE.add(0, 1.2, 0), Vec3.ZERO, 1.2f, 0, 0x9B6BFF, 0xE0D0FF, 14, -1, 0));
        add("sigil_occult", 26, mc -> spawn(VfxType.SIGIL, STAGE, Vec3.ZERO, 3f, 5, 0xB35CFF, 0xE0B0FF, 60, -1, 0));
        add("helix", 12, mc -> spawn(VfxType.HELIX, STAGE, Vec3.ZERO, 2.4f, 0, 0xE8C872, 0xFFFFFF, 30, -1, 0));
        add("auras", 22, mc -> {
            spawn(VfxType.AURA, STAGE.add(-2, 0, 0), Vec3.ZERO, 1.2f, 0, 0x7CD957, 0xC8FFB0, 60, -1, 0);
            spawn(VfxType.AURA, STAGE.add(2, 0, 0), Vec3.ZERO, 1.2f, 0, 0x2A2F55, 0x7D93C9, 60, -1, Vfx.VOID);
        });
        add("beam", 4, mc -> {
            spawn(VfxType.BEAM, STAGE.add(-3.5, 1.4, -1), STAGE.add(3, 1.0, 2), 0, 0.35f, 0xC0182E, 0xFF8A9A, 14, -1, 0);
            spawn(VfxType.BEAM, STAGE.add(-3.5, 0.6, 1), STAGE.add(3, 1.8, -1), 0, 0.3f, 0x8A2BE2, 0xE0B0FF, 14, -1, 0);
        });
        add("bolt", 3, mc -> spawn(VfxType.BOLT, STAGE.add(0, 12, 0), STAGE, 0, 0, 0xF6D77A, 0xFFFFFF, 10, -1, 0));
        add("slash_h", 5, mc -> spawn(VfxType.SLASH, STAGE.add(0, 1.0, 0), new Vec3(0, 0, -1), 2.2f, 220, 0xE6E1F5, 0xFFFFFF, 14, -1, 0));
        add("slash_v", 5, mc -> spawn(VfxType.SLASH, STAGE.add(0, 1.2, 0), new Vec3(0, 0, -1), 1.6f, 150, 0x3FB58A, 0xC8FFE0, 14, -1, Vfx.VERTICAL));
        add("vortex", 25, mc -> spawn(VfxType.VORTEX, STAGE.add(0, 1.5, 0), Vec3.ZERO, 5, 0, 0x9B6BFF, 0x2A0F4F, 60, -1, 0));
        add("tendrils", 11, mc -> spawn(VfxType.TENDRILS, STAGE, Vec3.ZERO, 2.4f, 6, 0x232A44, 0x7D93C9, 34, -1, 0));
        add("rift", 6, mc -> spawn(VfxType.RIFT, STAGE.add(0, 1.2, 0), new Vec3(0, 0, -1), 2.4f, 0, 0x9B6BFF, 0xE0D0FF, 18, -1, 0));
        add("wings_marks", 16, mc -> {
            List<ArmorStand> stands = stands(mc);
            if (stands.size() >= 2) {
                spawn(VfxType.WINGS, stands.get(0).position(), Vec3.ZERO, 1, 0, 0xFFF1B8, 0xFFFFFF, 80, stands.get(0).getId(), 0);
                spawn(VfxType.WINGS, stands.get(1).position(), Vec3.ZERO, 1, 0, 0x7A0A1A, 0xFF3048, 80, stands.get(1).getId(), Vfx.VOID);
                spawn(VfxType.MARK, stands.get(0).position(), Vec3.ZERO, 1, 0, 0x5FD37A, 0xC8FFD8, 80, stands.get(0).getId(), 0);
                spawn(VfxType.MARK, stands.get(1).position(), Vec3.ZERO, 1, 0, 0xB35CFF, 0xE0B0FF, 80, stands.get(1).getId(), 0);
            }
        });
        add("", 70, mc -> {});
        // ---- UI review: give the player an origin + class with progress
        add("", 40, mc -> {
            cmd(mc, "abyssorigins set @s vampire");
            cmd(mc, "abyssorigins specialization set @s occultist");
            cmd(mc, "abyssorigins setlevel @s 12");
            cmd(mc, "abyssorigins addskillpoints @s 6");
            cmd(mc, "abyssorigins specialization addlevel @s 6");
            mc.options.hideGui = false;
        });
        add("hud", 10, mc -> spawn(VfxType.HELIX, STAGE, Vec3.ZERO, 2.4f, 0, 0xC0182E, 0xFF8A9A, 30, -1, 0));
        add("ui_skilltree", 20, mc -> mc.setScreen(new SkillTreeScreen()));
        add("ui_skilltree_class", 20, mc -> { if (mc.screen != null) mc.screen.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0); });
        add("ui_codex", 20, mc -> mc.setScreen(new OriginInfoScreen()));
        add("", 5, mc -> mc.setScreen(null));
    }

    private static void add(String name, int wait, Consumer<Minecraft> action) {
        STEPS.add(new Step(name, wait, action));
    }

    private static List<ArmorStand> stands(Minecraft mc) {
        if (mc.level == null || mc.player == null) return List.of();
        return mc.level.getEntitiesOfClass(ArmorStand.class, new AABB(STAGE, STAGE).inflate(8)).stream()
            .sorted(Comparator.comparingDouble(Entity::getX)).toList();
    }

    private static void spawn(VfxType type, Vec3 pos, Vec3 dir, float scale, float extra, int c1, int c2,
                              int duration, int follow, int flags) {
        VfxManager.spawn(new VfxSpec(type, pos, dir, scale, extra, c1, c2, duration, follow, flags, (int) System.nanoTime()));
    }

    private static void cmd(Minecraft mc, String command) {
        if (mc.player != null) mc.player.connection.sendCommand(command);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "abyss_" + name + ".png", mc.getMainRenderTarget(),
            msg -> Abyssorigins.LOGGER.info("[AbyssOrigins] showcase screenshot {}", name));
    }
}
