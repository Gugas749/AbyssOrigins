package com.gugas749.abyssorigins.network.packet;

import com.gugas749.abyssorigins.admin.ProfileService;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Staff client → server: one edit to a player's profile. Untrusted — ProfileService re-checks
 * ADMIN permission, that the target is online, and validates / clamps every argument.
 */
public record ProfileEditPacket(UUID target, ProfileService.Action action, String arg, int value) implements CustomPacketPayload {

    public static final Type<ProfileEditPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("abyssorigins", "profile_edit"));

    public static final StreamCodec<FriendlyByteBuf, ProfileEditPacket> STREAM_CODEC = StreamCodec.of(
        (buf, p) -> { UUIDUtil.STREAM_CODEC.encode(buf, p.target); buf.writeEnum(p.action); buf.writeUtf(p.arg, 256); buf.writeVarInt(p.value); },
        buf -> new ProfileEditPacket(UUIDUtil.STREAM_CODEC.decode(buf), buf.readEnum(ProfileService.Action.class), buf.readUtf(256), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer admin) ProfileService.handle(admin, target, action, arg, value);
        });
    }
}
