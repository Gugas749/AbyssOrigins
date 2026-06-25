package com.gugas749.abyssorigins.client.input;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.gui.OriginInfoScreen;
import com.gugas749.abyssorigins.client.gui.skilltree.SkillTreeScreen;
import com.gugas749.abyssorigins.network.packet.ActivateAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT, modid = Abyssorigins.MODID)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) return;

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);

        if (KeyBindings.ABILITY_1.consumeClick()) {
            if (origin != null && !origin.getAbilities().isEmpty()) {
                PacketDistributor.sendToServer(new ActivateAbilityPacket(0));
            }
        }

        if (KeyBindings.ABILITY_2.consumeClick()) {
            if (origin != null && origin.getAbilities().size() > 1) {
                PacketDistributor.sendToServer(new ActivateAbilityPacket(1));
            }
        }

        if (KeyBindings.ORIGIN_INFO.consumeClick()) {
            mc.setScreen(new OriginInfoScreen());
        }

        if (KeyBindings.SKILL_TREE.consumeClick()) {
            mc.setScreen(new SkillTreeScreen());
        }
    }
}
