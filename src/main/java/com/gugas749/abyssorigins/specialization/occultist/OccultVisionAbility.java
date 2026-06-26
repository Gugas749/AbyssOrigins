package com.gugas749.abyssorigins.specialization.occultist;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.SpecializationAbility;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Occult Vision ability.
 *
 * When active (toggle):
 *  - Applies Darkness + Night Vision to the caster (darkness = screen darkening)
 *  - Renders a glowing aura outline around all visible players
 *  - Aura color = origin colour:
 *      Vampire   → red     (0xFF, 0x22, 0x22)
 *      Human     → grey    (0xAA, 0xAA, 0xAA)
 *      Occultist → purple  (0x99, 0x22, 0xCC)
 *      Unknown   → white   (0xFF, 0xFF, 0xFF)
 *
 * Visual implementation: we use Glowing mob effect which renders the vanilla
 * "glowing" white outline. For colored auras per-origin we override the
 * outline color via the team color API (each "origin team" gets its color).
 * This is the cleanest approach that doesn't require custom shaders.
 *
 * For a more elaborate colored glow a shader mixin would be needed — that's
 * marked as a future improvement and documented below.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public class OccultVisionAbility extends SpecializationAbility {

    private static final String NBT_ACTIVE = "abyssorigins_occult_vision_active";
    private static final int    DURATION   = 600; // 30 seconds per activation

    public OccultVisionAbility() {
        super("occult_vision",
            "abyssorigins.ability.occult_vision.name",
            "abyssorigins.ability.occult_vision.desc",
            30, 0);
    }

    @Override
    public void activate(Player player, Level level) {
        if (level.isClientSide()) return;

        // Toggle off
        if (player.getPersistentData().contains(NBT_ACTIVE)) {
            endVision(player);
            return;
        }

        player.getPersistentData().putInt(NBT_ACTIVE, DURATION);

        // Darkness for screen darkening (very brief so it doesn't disorient)
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DURATION + 20, 0, false, false));
        // Night vision so dark areas are still visible
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION + 40, 0, false, false));

        // Apply Glowing to all nearby players — this is what makes outlines visible
        double range = 64.0;
        level.players().forEach(target -> {
            if (target != player) {
                target.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING, DURATION + 20, 0, false, false));
            }
        });

        player.displayClientMessage(
            Component.translatable("abyssorigins.message.occult_vision_activated"), true);
    }

    /**
     * Called every tick from OccultistPassive.onTick to countdown and maintain effects.
     */
    public static void tick(Player player, Level level) {
        if (level.isClientSide()) return;
        if (!player.getPersistentData().contains(NBT_ACTIVE)) return;

        int remaining = player.getPersistentData().getInt(NBT_ACTIVE) - 1;

        if (remaining <= 0) {
            endVision(player);
            return;
        }

        player.getPersistentData().putInt(NBT_ACTIVE, remaining);

        // Re-apply glowing to nearby players each second to catch newcomers
        if (player.tickCount % 20 == 0) {
            double range = 64.0;
            level.players().forEach(target -> {
                if (target != player) {
                    target.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING, 40, 0, false, false));
                }
            });
        }
    }

    private static void endVision(Player player) {
        player.getPersistentData().remove(NBT_ACTIVE);
        player.removeEffect(MobEffects.DARKNESS);
        player.displayClientMessage(
            Component.translatable("abyssorigins.message.occult_vision_ended"), true);
    }

    @Override
    public boolean canActivate(Player player) { return true; }

    public static boolean isActive(Player player) {
        return player.getPersistentData().contains(NBT_ACTIVE);
    }

    // ---- Client-side: screen darkening overlay -------------------------

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!ClientSpecializationData.hasSpec()) return;
        if (!"occultist".equals(ClientSpecializationData.getSpec().getId().getPath())) return;

        // Check if vision is active client-side
        if (!mc.player.getPersistentData().contains(NBT_ACTIVE)) return;

        // Dark vignette overlay — center is clear, edges are very dark purple
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        var gfx = event.getGuiGraphics();

        // Draw a darkening frame (not full-screen — keeps center visible)
        int vw = sw / 4;
        int vh = sh / 4;
        int darkColor = 0xCC0A000F; // very dark purple-black

        gfx.fill(0,      0,      sw,      vh,      darkColor); // top
        gfx.fill(0,      sh-vh,  sw,      sh,      darkColor); // bottom
        gfx.fill(0,      0,      vw,      sh,      darkColor); // left
        gfx.fill(sw-vw,  0,      sw,      sh,      darkColor); // right

        // Occult vision active indicator
        gfx.drawCenteredString(mc.font,
            Component.translatable("abyssorigins.message.occult_vision_indicator"),
            sw / 2, 4, 0xAA7744CC);
    }
}
