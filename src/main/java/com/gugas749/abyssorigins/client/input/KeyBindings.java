package com.gugas749.abyssorigins.client.input;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.SlotGroup;
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
    private static final int UNBOUND = InputConstants.UNKNOWN.getValue();

    /**
     * One key per slot, sized from SlotGroup. Names are kept from before the slot system so
     * existing rebinds survive. The third class slot starts unbound.
     */
    public static final KeyMapping[] ORIGIN_SLOTS = {
        slot("ability1", GLFW.GLFW_KEY_R), slot("ability2", GLFW.GLFW_KEY_V), slot("ability3", GLFW.GLFW_KEY_Z),
        slot("ability4", GLFW.GLFW_KEY_B), slot("ability5", GLFW.GLFW_KEY_N)
    };
    public static final KeyMapping[] CLASS_SLOTS = {
        slot("spec_ability", GLFW.GLFW_KEY_F), slot("spec_ability2", GLFW.GLFW_KEY_G), slot("spec_ability3", UNBOUND)
    };

    public static final KeyMapping ORIGIN_INFO  = new KeyMapping("key.abyssorigins.origin_info",  KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping SKILL_TREE   = new KeyMapping("key.abyssorigins.skill_tree",   KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping ABILITIES    = new KeyMapping("key.abyssorigins.abilities",    KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping HUD_CONFIG   = new KeyMapping("key.abyssorigins.hud_config",   KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY);

    static {
        if (ORIGIN_SLOTS.length != SlotGroup.ORIGIN.size() || CLASS_SLOTS.length != SlotGroup.CLASS.size())
            throw new IllegalStateException("Slot keys out of step with SlotGroup sizes");
    }

    private static KeyMapping slot(String name, int key) {
        return new KeyMapping("key.abyssorigins." + name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, CATEGORY);
    }

    public static KeyMapping[] forGroup(SlotGroup group) {
        return group == SlotGroup.ORIGIN ? ORIGIN_SLOTS : CLASS_SLOTS;
    }

    public static KeyMapping forSlot(SlotGroup group, int slot) {
        return forGroup(group)[slot];
    }

    /** Short key label for slot badges: "R", "F", or "—" when unbound. */
    public static String label(SlotGroup group, int slot, int max) {
        KeyMapping k = forSlot(group, slot);
        if (k.isUnbound()) return "—";
        String s = k.getTranslatedKeyMessage().getString();
        return s.length() > max ? s.substring(0, max) : s;
    }

    @SubscribeEvent
    public static void onRegisterBindings(RegisterKeyMappingsEvent event) {
        for (KeyMapping k : ORIGIN_SLOTS) event.register(k);
        for (KeyMapping k : CLASS_SLOTS) event.register(k);
        event.register(ORIGIN_INFO);
        event.register(SKILL_TREE);
        event.register(ABILITIES);
        event.register(HUD_CONFIG);
    }
}
