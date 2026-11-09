package com.gugas749.abyssorigins.client.input;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.client.ClientAbilityData;
import com.gugas749.abyssorigins.client.ClientSpecializationData;
import com.gugas749.abyssorigins.client.ClientTimers;
import com.gugas749.abyssorigins.client.gui.AbilitiesScreen;
import com.gugas749.abyssorigins.client.gui.HudLayoutScreen;
import com.gugas749.abyssorigins.client.gui.OriginInfoScreen;
import com.gugas749.abyssorigins.client.gui.skilltree.SkillTreeScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = com.gugas749.abyssorigins.Abyssorigins.MODID)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        // Smooth client-side countdowns between server syncs — once per TICK (the old HUD did it per frame)
        if (!mc.isPaused()) {
            ClientAbilityData.tick();
            ClientTimers.tick();
        }

        if (mc.screen != null) return;

        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);

        for (SlotGroup group : SlotGroup.values()) {
            KeyMapping[] keys = KeyBindings.forGroup(group);
            boolean live = group == SlotGroup.ORIGIN ? origin != null : ClientSpecializationData.hasSpec();
            for (int i = 0; i < keys.length; i++)
                while (keys[i].consumeClick())
                    if (live) ClientAbilityData.activate(group, i);
        }

        if (KeyBindings.ORIGIN_INFO.consumeClick() && origin != null)
            mc.setScreen(new OriginInfoScreen());

        // The Eminence walks no single path: without a class tree its "tree" is the Abilities tab
        if (KeyBindings.SKILL_TREE.consumeClick() && origin != null)
            mc.setScreen(new SkillTreeScreen());

        if (KeyBindings.ABILITIES.consumeClick() && origin != null)
            mc.setScreen(new AbilitiesScreen(null, SlotGroup.ORIGIN));

        if (KeyBindings.HUD_CONFIG.consumeClick())
            HudLayoutScreen.open();
    }
}
