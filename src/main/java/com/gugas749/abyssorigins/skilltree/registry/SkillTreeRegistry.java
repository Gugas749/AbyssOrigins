package com.gugas749.abyssorigins.skilltree.registry;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SkillTreeRegistry {

    private static final Map<ResourceLocation, SkillTree> TREES = new LinkedHashMap<>();

    private SkillTreeRegistry() {}

    /** Register a skill tree for the given origin. Can be called from any addon. */
    public static void register(SkillTree tree) {
        TREES.put(tree.getOriginId(), tree);
        Abyssorigins.LOGGER.info("[AbyssOrigins] Registered skill tree for {}", tree.getOriginId());
    }

    public static Optional<SkillTree> getTree(ResourceLocation originId) {
        return Optional.ofNullable(TREES.get(originId));
    }

    public static Collection<SkillTree> getAllTrees() {
        return Collections.unmodifiableCollection(TREES.values());
    }

    public static boolean hasTree(ResourceLocation originId) {
        return TREES.containsKey(originId);
    }
}
