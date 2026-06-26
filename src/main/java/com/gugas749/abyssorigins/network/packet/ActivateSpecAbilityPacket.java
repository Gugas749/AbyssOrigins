package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.api.Specialization;
import com.gugas749.abyssorigins.api.SpecializationAbility;
import com.gugas749.abyssorigins.data.PlayerSpecializationData;
import com.gugas749.abyssorigins.data.SpecializationAttachments;
import com.gugas749.abyssorigins.network.ModPackets;
import com.gugas749.abyssorigins.registry.SpecializationRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ActivateSpecAbilityPacket(int slot) implements CustomPacketPayload {

    public static final Type<ActivateSpecAbilityPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "activate_spec_ability"));

    public static final StreamCodec<FriendlyByteBuf, ActivateSpecAbilityPacket> STREAM_CODEC =
        StreamCodec.of((buf, p) -> buf.writeInt(p.slot), buf -> new ActivateSpecAbilityPacket(buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            PlayerSpecializationData data =
                player.getData(SpecializationAttachments.PLAYER_SPEC.get());
            if (!data.hasSpec()) return;

            Specialization spec = SpecializationRegistry.get(data.getSpecId()).orElse(null);
            if (spec == null || slot >= spec.getAbilities().size()) return;

            SpecializationAbility ability = spec.getAbilities().get(slot);

            if (data.getAbilityCooldown() > 0) return;
            if (!ability.canActivate(player)) return;

            ability.activate(player, player.level());
            data.setAbilityCooldown(ability.getBaseCooldownTicks());

            ModPackets.syncSpecialization(player);
        });
    }
}
