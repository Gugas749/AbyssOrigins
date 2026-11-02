package com.gugas749.abyssorigins.client.ritual;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.client.gui.OriginChoiceScreen;
import com.gugas749.abyssorigins.network.packet.RitualPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Client staging of the Orb / Seed ritual. While the player awakens, the camera swings to the
 * front so they see their own aura flare and change colour; it returns to whatever view they
 * had a few seconds after the transformation (or at once if the ritual is abandoned).
 */
@EventBusSubscriber(modid = Abyssorigins.MODID, value = Dist.CLIENT)
public final class ClientRitual {

    private static final int RESTORE_DELAY = 60;

    private static CameraType saved = null;
    private static int restoreIn = -1;

    private ClientRitual() {}

    public static void handle(RitualPacket.Stage stage, List<String> origins) {
        Minecraft mc = Minecraft.getInstance();
        switch (stage) {
            case AWAKEN -> {
                if (saved == null) saved = mc.options.getCameraType();
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                restoreIn = -1;
            }
            case CHOOSE -> {
                List<Origin> choices = new ArrayList<>();
                for (String id : origins) {
                    ResourceLocation rl = ResourceLocation.tryParse(id);
                    Origin o = rl == null ? null : AbyssOriginsAPI.getOrigin(rl);
                    if (o != null) choices.add(o);
                }
                mc.setScreen(new OriginChoiceScreen(choices));
            }
            case TRANSFORMED -> restoreIn = RESTORE_DELAY;
            case CANCELLED   -> {
                // e.g. the choice timed out server-side: take the menu down too
                if (mc.screen instanceof OriginChoiceScreen choice) choice.dismiss();
                restore();
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (restoreIn > 0 && --restoreIn == 0) restore();
    }

    private static void restore() {
        restoreIn = -1;
        if (saved == null) return;
        Minecraft.getInstance().options.setCameraType(saved);
        saved = null;
    }

    public static void clear() {
        restore();
    }
}
