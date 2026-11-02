package com.gugas749.abyssorigins.client;

import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.item.gear.ArmorSet;
import com.gugas749.abyssorigins.item.gear.Gear;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Client-side queries about the local player, used by gear tooltips. */
public final class GearClient {

    private GearClient() {}

    public static boolean localAttuned(String origin) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return true;
        if (p.isCreative()) return true;
        Origin o = ClientOriginData.getOrigin();
        return o != null && (o.getPath().equals(origin) || com.gugas749.abyssorigins.origin.eminence.Eminence.is(o));
    }

    public static int localWorn(ArmorSet set) {
        Player p = Minecraft.getInstance().player;
        return p == null ? 0 : Gear.worn(p, set);
    }
}
