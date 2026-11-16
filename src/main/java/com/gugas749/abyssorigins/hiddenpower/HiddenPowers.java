package com.gugas749.abyssorigins.hiddenpower;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.HiddenAbility;
import com.gugas749.abyssorigins.registry.AbilityRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The hidden powers: staff-grant only, kept apart from origin / class progression.
 *
 * They are registered here and nowhere else: no origin or class kit lists them and skill nodes
 * can't teach them (Loadouts ignores a hidden-power id on a node). The only way to get one is a staff
 * grant - {@code /abyssorigins ability grant <player> <id>} or the profile GUI's Abilities tab -
 * both of which require AbyssCore ADMIN. Once granted, a power binds into an origin slot.
 *
 *   hidden_portal  - {@link PortalAbility}: links two aimed-at spots (up to 128 blocks away) for 300 s, both ways
 *   hidden_pocket  - {@link PocketAbility}: the caster's private night-void dimension
 *   hidden_punish  - {@link PunishAbility}: crucifies a targeted player in the punishment realm for 60 s
 *   hidden_dementor - {@link DementorAbility}: summons three Dementors that target every player but the caster for 45 s
 */
public final class HiddenPowers {

    public static final ResourceLocation OWNER = ResourceLocation.fromNamespaceAndPath(Abyssorigins.MODID, "hidden");
    public static final String SIGIL = "✠";
    public static final int COLOR = 0xFFB0303C;

    public static final PortalAbility PORTAL = new PortalAbility();
    public static final PocketAbility POCKET = new PocketAbility();
    public static final PunishAbility PUNISH = new PunishAbility();
    public static final DementorAbility DEMENTOR = new DementorAbility();

    private static final List<HiddenAbility> ALL = List.of(PORTAL, POCKET, PUNISH, DEMENTOR);

    private HiddenPowers() {}

    public static List<HiddenAbility> all() { return ALL; }

    public static Component ownerName() { return Component.translatable("abyssorigins.hidden_powers.name"); }

    public static boolean isHidden(String id) {
        AbilityRegistry.Entry e = AbilityRegistry.get(id);
        return e != null && e.grantOnly();
    }
}
