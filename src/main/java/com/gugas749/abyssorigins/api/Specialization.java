package com.gugas749.abyssorigins.api;

import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * A Specialization is an optional second layer on top of an Origin.
 * It is origin-agnostic by default — set allowedOrigins() to restrict.
 *
 * Self-contained: carries its own passives, abilities, and skill tree.
 * Adding a new spec requires only extending this class + one register() call.
 */
public abstract class Specialization {

    private final ResourceLocation id;
    private final Component        displayName;
    private final Component        description;

    private final List<SpecializationPassive> passives   = new ArrayList<>();
    private final List<SpecializationAbility> abilities  = new ArrayList<>();
    private final Set<String>                 allowedOriginPaths = new HashSet<>();

    private SkillTree skillTree = null;

    /** Theme colour shown in the HUD and Origin Info screen. ARGB. */
    private int themeColor = 0xFF9944CC; // default purple

    public Specialization(ResourceLocation id, String nameKey, String descKey) {
        this.id          = id;
        this.displayName = Component.translatable(nameKey);
        this.description = Component.translatable(descKey);
    }

    // ---- Builder-style setters (call from constructor) ------------------

    protected Specialization addPassive(SpecializationPassive passive) {
        passives.add(passive); return this;
    }

    protected Specialization addAbility(SpecializationAbility ability) {
        abilities.add(ability); return this;
    }

    protected Specialization setSkillTree(SkillTree tree) {
        this.skillTree = tree; return this;
    }

    /**
     * Restrict this specialization to certain origin paths.
     * Leave empty (default) to allow all origins.
     * Example: allowedOrigins("vampire", "human")
     */
    protected Specialization allowedOrigins(String... paths) {
        allowedOriginPaths.addAll(Arrays.asList(paths));
        return this;
    }

    protected Specialization setThemeColor(int color) {
        this.themeColor = color; return this;
    }

    // ---- Accessors -------------------------------------------------------

    public ResourceLocation getId()        { return id; }
    public Component getDisplayName()      { return displayName; }
    public Component getDescription()      { return description; }
    public int       getThemeColor()       { return themeColor; }
    public SkillTree getSkillTree()        { return skillTree; }

    public List<SpecializationPassive> getPassives()   { return Collections.unmodifiableList(passives); }
    public List<SpecializationAbility> getAbilities()  { return Collections.unmodifiableList(abilities); }

    /** True if this spec can be assigned to a player of the given origin path. */
    public boolean isAllowedForOrigin(String originPath) {
        return allowedOriginPaths.isEmpty() || allowedOriginPaths.contains(originPath);
    }
}
