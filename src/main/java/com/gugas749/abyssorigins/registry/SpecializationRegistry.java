package com.gugas749.abyssorigins.registry;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.specialization.assassin.AssassinSpecialization;
import com.gugas749.abyssorigins.specialization.guardian.GuardianSpecialization;
import com.gugas749.abyssorigins.specialization.occultist.OccultistSpecialization;
import com.gugas749.abyssorigins.specialization.ranger.RangerSpecialization;
import com.gugas749.abyssorigins.specialization.warrior.WarriorSpecialization;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class SpecializationRegistry {

    private static final Map<ResourceLocation, Specialization> SPECS = new LinkedHashMap<>();

    private SpecializationRegistry() {}

    /** Called once during FMLCommonSetupEvent. Add new classes here — one line each. */
    public static void registerAll() {
        register(new WarriorSpecialization());
        register(new AssassinSpecialization());
        register(new GuardianSpecialization());
        register(new RangerSpecialization());
        register(new OccultistSpecialization());
    }

    public static void register(Specialization spec) {
        SPECS.put(spec.getId(), spec);
        AbilityRegistry.invalidate();
        Abyssorigins.LOGGER.info("[AbyssOrigins] Registered specialization: {}", spec.getId());
    }

    public static Optional<Specialization> get(ResourceLocation id) {
        return Optional.ofNullable(SPECS.get(id));
    }

    public static Collection<Specialization> getAll() {
        return Collections.unmodifiableCollection(SPECS.values());
    }

    public static boolean isRegistered(ResourceLocation id) {
        return SPECS.containsKey(id);
    }
}
