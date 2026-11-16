package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.api.SlotGroup;
import com.gugas749.abyssorigins.network.ModPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * On / off switches for passives that act on everything around the player (auras, eruptions,
 * bursts, pulses). Each is keyed by the skill node that grants it; switching one off only
 * silences the area effect - the node's stats and any other perks it gives stay active.
 *
 * Switched-off ids persist in PlayerAbilityData and are toggled from the Abilities tab.
 */
public final class AuraToggles {

    /** @param group the Abilities tab that lists the switch: origin-tree or class-tree skill. */
    public record Entry(String skill, SlotGroup group) {
        public Component name()        { return Component.translatable("abyssorigins.skill." + skill + ".name"); }
        public Component description() { return Component.translatable("abyssorigins.skill." + skill + ".desc"); }
    }

    public static final List<Entry> ALL = List.of(
        new Entry("demon_archfiend",      SlotGroup.ORIGIN),   // burning aura above 50 Wrath
        new Entry("demon_ember_heart",    SlotGroup.ORIGIN),   // Hellfire Nova eruption at peak Wrath
        new Entry("void_entropy_field",   SlotGroup.ORIGIN),   // slow + Sundered field
        new Entry("seraph_beacon",        SlotGroup.ORIGIN),   // regeneration pulse for allies
        new Entry("vamp_blood_lord",      SlotGroup.ORIGIN),   // blood burst on blooded kills
        new Entry("assassin_smoke_bomb",  SlotGroup.CLASS),    // blinding smoke when cornered
        new Entry("guardian_martyr",      SlotGroup.CLASS),    // take a share of nearby allies' damage
        new Entry("guardian_eternal_vow", SlotGroup.CLASS)     // save a nearby ally from death
    );

    private AuraToggles() {}

    public static Entry get(String skill) {
        for (Entry e : ALL) if (e.skill.equals(skill)) return e;
        return null;
    }

    /** The player's switch for {@code skill} (on unless they turned it off). */
    public static boolean on(Player p, String skill) {
        return Loadouts.data(p).isPassiveOn(skill);
    }

    /** Has the skill and has not switched its area effect off. */
    public static boolean active(Player p, String skill) {
        return Skills.has(p, skill) && on(p, skill) && !Nullification.is(p);
    }

    /** Server side: flips a switch (unknown ids are ignored), syncs, and confirms in the action bar. */
    public static void set(ServerPlayer p, String skill, boolean on) {
        Entry e = get(skill);
        if (e == null) return;
        Loadouts.data(p).setPassiveOn(skill, on);
        ModPackets.syncAbilities(p);
        p.displayClientMessage(Component.translatable(on ? "abyssorigins.message.passive_on" : "abyssorigins.message.passive_off",
            e.name().copy().withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY)), true);
    }
}
