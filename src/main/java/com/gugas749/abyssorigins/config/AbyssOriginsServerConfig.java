package com.gugas749.abyssorigins.config;

import com.gugas749.abyssorigins.Abyssorigins;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side config for AbyssOrigins, stored per world in serverconfig/abyssorigins-server.toml.
 * Registered in Abyssorigins.java as ModConfig.Type.SERVER (synced to clients, edited by server owners).
 */
public final class AbyssOriginsServerConfig {

    public static final String FILE = Abyssorigins.MODID + "-server.toml";

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ATOMIC_DESTROYS_BLOCKS;
    public static final ModConfigSpec.IntValue ATOMIC_BLOCKS_PER_TICK;
    public static final ModConfigSpec.BooleanValue ASCENSION_SKY;

    static {
        BUILDER.push("eminence");
        ATOMIC_DESTROYS_BLOCKS = BUILDER
            .comment("Whether I Am Atomic destroys the blocks inside its blast (a crater of up to 100 blocks radius).")
            .define("atomicDestroysBlocks", true);
        ATOMIC_BLOCKS_PER_TICK = BUILDER
            .comment("How many blocks I Am Atomic removes per server tick. Lower it if the blast lags the server.")
            .defineInRange("atomicBlocksPerTick", 6000, 500, 100000);
        BUILDER.pop();

        BUILDER.push("ascension");
        ASCENSION_SKY = BUILDER
            .comment("Colour an ascending player's sky with their origin's colour (the Eminence gets a starry night sky),",
                     "from the start of the ceremony until two minutes after it. Only they see it.")
            .define("ascensionSky", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AbyssOriginsServerConfig() {}
}
