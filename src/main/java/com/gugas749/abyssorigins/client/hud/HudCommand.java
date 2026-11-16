package com.gugas749.abyssorigins.client.hud;

import com.gugas749.abyssorigins.Abyssorigins;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * /abyssoriginshud opens the layout editor; /abyssoriginshud reset restores the stock HUD (and saves).
 * Client-side, for every player. Both come from AbyssCore's {@code AbyssHud.editorCommand}.
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public final class HudCommand {

    private HudCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(OriginsHud.INSTANCE.editorCommand("abyssoriginshud"));
    }
}
