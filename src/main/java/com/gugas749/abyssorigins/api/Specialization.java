package com.gugas749.abyssorigins.api;

import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.skilltree.data.SkillTree;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * A Specialization (shown to players as a "Class") is the second layer on top of an
 * Origin. It is origin-agnostic by default — set allowedOrigins() to restrict.
 *
 * Self-contained: carries its own passives, abilities, hooks, resonances and skill tree.
 * Adding a new class requires only extending this class + one register() call.
 */
public abstract class Specialization {

    private final ResourceLocation id;
    private final Component        displayName;
    private final Component        description;

    private final List<SpecializationPassive> passives   = new ArrayList<>();
    private final List<SpecializationAbility> abilities  = new ArrayList<>();
    private final List<SpecializationAbility> unlockable = new ArrayList<>();
    private final List<StatBonus>             baseStats  = new ArrayList<>();
    private final Map<String, Resonance>      resonances = new LinkedHashMap<>();
    private final Set<String>                 allowedOriginPaths = new HashSet<>();

    private SkillTree  skillTree = null;
    private PowerHooks hooks     = PowerHooks.NONE;
    private String     sigil     = "✦";

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

    /** Part of the default kit: known from the start and laid into the first free slots. */
    protected Specialization addAbility(SpecializationAbility ability) {
        abilities.add(ability); return this;
    }

    /** A power this class owns but must learn (skill node or staff grant) before binding it. */
    protected Specialization addUnlockableAbility(SpecializationAbility ability) {
        unlockable.add(ability); return this;
    }

    protected Specialization addStat(StatBonus bonus) {
        baseStats.add(bonus); return this;
    }

    protected Specialization addResonance(Resonance resonance) {
        resonances.put(resonance.originPath(), resonance); return this;
    }

    protected Specialization setSkillTree(SkillTree tree) {
        this.skillTree = tree; return this;
    }

    protected Specialization setHooks(PowerHooks hooks) {
        this.hooks = hooks; return this;
    }

    protected Specialization setSigil(String glyph) {
        this.sigil = glyph; return this;
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
    public String    getPath()             { return id.getPath(); }
    public Component getDisplayName()      { return displayName; }
    public Component getDescription()      { return description; }
    public int       getThemeColor()       { return themeColor; }
    public String    getSigil()            { return sigil; }
    public SkillTree getSkillTree()        { return skillTree; }
    public PowerHooks getHooks()           { return hooks; }

    public List<SpecializationPassive> getPassives()   { return Collections.unmodifiableList(passives); }
    public List<SpecializationAbility> getAbilities()  { return Collections.unmodifiableList(abilities); }
    public List<SpecializationAbility> getUnlockableAbilities() { return Collections.unmodifiableList(unlockable); }
    public List<StatBonus>             getBaseStats()  { return Collections.unmodifiableList(baseStats); }
    public Collection<Resonance>       getResonances() { return Collections.unmodifiableCollection(resonances.values()); }

    public Optional<Resonance> getResonance(String originPath) {
        return Optional.ofNullable(resonances.get(originPath));
    }

    /** True if the player's current origin resonates with this class. */
    public boolean hasResonance(Player player) {
        Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
        return origin != null && resonances.containsKey(origin.getPath());
    }

    /** True if this spec can be assigned to a player of the given origin path. */
    public boolean isAllowedForOrigin(String originPath) {
        return allowedOriginPaths.isEmpty() || allowedOriginPaths.contains(originPath);
    }

    /** Convenience: does the player have this class's skill node unlocked? */
    public static boolean hasClassSkill(Player player, String skillId) {
        PlayerSpecializationData data = player.getData(SpecializationAttachments.PLAYER_SPEC.get());
        return data.hasSkill(skillId);
    }
}
