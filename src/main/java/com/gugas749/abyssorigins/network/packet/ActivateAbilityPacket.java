package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.api.AbyssOriginsAPI;
import com.gugas749.abyssorigins.api.Origin;
import com.gugas749.abyssorigins.api.OriginAbility;
import com.gugas749.abyssorigins.data.OriginAttachments;
import com.gugas749.abyssorigins.data.PlayerOriginData;
import com.gugas749.abyssorigins.network.ModPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ActivateAbilityPacket(int slot) implements CustomPacketPayload {

    public static final Type<ActivateAbilityPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "activate_ability"));

    public static final StreamCodec<FriendlyByteBuf, ActivateAbilityPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> buf.writeInt(pkt.slot),
            buf -> new ActivateAbilityPacket(buf.readInt())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            Origin origin = AbyssOriginsAPI.getPlayerOrigin(player);
            if (origin == null) return;
            if (slot < 0 || slot >= origin.getAbilities().size()) return;

            OriginAbility ability = origin.getAbilities().get(slot);
            PlayerOriginData data = player.getData(OriginAttachments.PLAYER_ORIGIN.get());

            int cooldown = slot == 0 ? data.getCooldown1() : data.getCooldown2();
            if (cooldown > 0) return; // still on cooldown

            // Resource check
            if (data.getResource() < ability.getResourceCost()) return;

            if (!ability.canActivate(player)) return;

            // Activate
            ability.activate(player, player.level());

            // Deduct resource
            data.drainResource(ability.getResourceCost());

            // Set cooldown
            if (slot == 0) data.setCooldown1(ability.getBaseCooldownTicks());
            else           data.setCooldown2(ability.getBaseCooldownTicks());

            // Sync result to client
            ModPackets.syncPlayer(player);
        });
    }
}
