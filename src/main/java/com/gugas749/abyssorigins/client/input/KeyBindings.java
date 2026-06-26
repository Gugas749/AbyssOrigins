package com.gugas749.abyssorigins.client.input;

import com.gugas749.abyssorigins.Abyssorigins;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT, modid = Abyssorigins.MODID)
public class KeyBindings {

    public static final String CATEGORY = "key.categories.abyssorigins";

    public static final KeyMapping ABILITY_1 = new KeyMapping(
        "key.abyssorigins.ability1", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    public static final KeyMapping ABILITY_2 = new KeyMapping(
        "key.abyssorigins.ability2", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);

    public static final KeyMapping ORIGIN_INFO = new KeyMapping(
        "key.abyssorigins.origin_info", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY);

    public static final KeyMapping SKILL_TREE = new KeyMapping(
        "key.abyssorigins.skill_tree", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    public static final KeyMapping HUD_CONFIG = new KeyMapping(
        "key.abyssorigins.hud_config", KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    @SubscribeEvent
    public static void onRegisterBindings(RegisterKeyMappingsEvent event) {
        event.register(ABILITY_1);
        event.register(ABILITY_2);
        event.register(ORIGIN_INFO);
        event.register(SKILL_TREE);
        event.register(HUD_CONFIG);
    }
}
