package com.gugas749.abyssorigins.power;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.origin.eminence.Eminence;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import com.gugas749.abyssorigins.skilltree.data.SkillAttachments;
import net.minecraft.world.entity.player.Player;

/**
 * One place to ask "does this player have skill X", regardless of whether X lives
 * in the origin tree or the class tree (node ids are globally unique by prefix).
 */
public final class Skills {

    private Skills() {}

    /** The Eminence holds every origin-tree skill without unlocking it (class trees still need points). */
    public static boolean has(Player p, String skillId) {
        return p.getData(SkillAttachments.PLAYER_SKILLS.get()).hasSkill(skillId)
            || p.getData(SpecializationAttachments.PLAYER_SPEC.get()).hasSkill(skillId)
            || Eminence.grantsSkill(p, skillId);
    }

    public static boolean isOrigin(Player p, String originPath) {
        Origin o = AbyssOriginsAPI.getPlayerOrigin(p);
        return o != null && o.getPath().equals(originPath);
    }

    public static Specialization specOf(Player p) {
        PlayerSpecializationData data = p.getData(SpecializationAttachments.PLAYER_SPEC.get());
        return data.hasSpec() ? SpecializationRegistry.get(data.getSpecId()).orElse(null) : null;
    }

    public static boolean isClass(Player p, String specPath) {
        Specialization s = specOf(p);
        return s != null && s.getPath().equals(specPath);
    }

    public static PlayerOriginData originData(Player p) {
        return p.getData(OriginAttachments.PLAYER_ORIGIN.get());
    }

    public static float resource(Player p)               { return originData(p).getResource(); }
    public static void  addResource(Player p, float v)   { originData(p).addResource(v); }
    public static boolean spendResource(Player p, float v) {
        PlayerOriginData d = originData(p);
        if (d.getResource() < v) return false;
        d.drainResource(v);
        return true;
    }
}
