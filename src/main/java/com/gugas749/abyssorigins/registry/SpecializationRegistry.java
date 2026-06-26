package com.gugas749.abyssorigins.registry;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.specialization.occultist.OccultistSpecialization;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class SpecializationRegistry {

    private static final Map<ResourceLocation, Specialization> SPECS = new LinkedHashMap<>();

    private SpecializationRegistry() {}

    /** Called once during FMLCommonSetupEvent. Add new specs here — one line each. */
    public static void registerAll() {
        register(new OccultistSpecialization());
        // register(new NecromancerSpecialization());  <- future: one line
        // register(new PaladinSpecialization());       <- future: one line
    }

    public static void register(Specialization spec) {
        SPECS.put(spec.getId(), spec);
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
