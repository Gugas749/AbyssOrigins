package com.gugas749.abyssorigins.event;

import com.gugas749.abyssorigins.Abyssorigins;
import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Abyssorigins.MODID)
public class SpecializationEventHandler {

    private static final int SYNC_INTERVAL = 20;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PlayerSpecializationData data =
            player.getData(SpecializationAttachments.PLAYER_SPEC.get());

        if (data.hasSpec()) {
            Specialization spec = SpecializationRegistry.get(data.getSpecId()).orElse(null);
            if (spec != null) {
                spec.getPassives().forEach(p -> {
                    try { p.onEquip(player); }
                    catch (Exception e) { Abyssorigins.LOGGER.error("Spec passive onEquip error: {}", e.getMessage()); }
                });
            } else {
                Abyssorigins.LOGGER.warn("[AbyssOrigins] Unknown saved spec {} for {} — clearing",
                    data.getSpecId(), player.getName().getString());
                data.setSpecId(null);
            }
        }

        ModPackets.syncSpecialization(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        PlayerSpecializationData data =
            sp.getData(SpecializationAttachments.PLAYER_SPEC.get());

        if (data.hasSpec()) {
            Specialization spec = SpecializationRegistry.get(data.getSpecId()).orElse(null);
            if (spec != null) spec.getPassives().forEach(p -> {
                try { p.onEquip(sp); }
                catch (Exception e) { Abyssorigins.LOGGER.error("Spec passive clone error: {}", e.getMessage()); }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        PlayerSpecializationData data =
            player.getData(SpecializationAttachments.PLAYER_SPEC.get());
        if (!data.hasSpec()) return;

        Specialization spec = SpecializationRegistry.get(data.getSpecId()).orElse(null);
        if (spec == null) return;

        // Tick cooldown
        data.tickAbilityCooldown();

        // Tick passives
        spec.getPassives().forEach(passive -> {
            try { passive.onTick(player); }
            catch (Exception e) {
                Abyssorigins.LOGGER.error("Spec passive tick error {} for {}: {}",
                    passive.getId(), player.getName().getString(), e.getMessage());
            }
        });

        if (player.tickCount % SYNC_INTERVAL == 0) {
            ModPackets.syncSpecialization(sp);
        }
    }
}
