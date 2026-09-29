/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.CoasterCurveDyeInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterCurveUndyeApplyPayload(BlockPos curveOwner, BlockPos curveRemote, Vec3 soundPos, InteractionHand hand) implements CustomPacketPayload
{
    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.DOUBLE, v -> v.x, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.y, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, InteractionHand> HAND_CODEC = StreamCodec.of((buf, hand) -> buf.writeVarInt(hand == InteractionHand.OFF_HAND ? 1 : 0), buf -> buf.readVarInt() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    public static final CustomPacketPayload.Type<CoasterCurveUndyeApplyPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_curve_undye_apply"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoasterCurveUndyeApplyPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, CoasterCurveUndyeApplyPayload::curveOwner, (StreamCodec)BlockPos.STREAM_CODEC, CoasterCurveUndyeApplyPayload::curveRemote, VEC3_CODEC, CoasterCurveUndyeApplyPayload::soundPos, HAND_CODEC, CoasterCurveUndyeApplyPayload::hand, CoasterCurveUndyeApplyPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(CoasterCurveUndyeApplyPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)patt0$temp;
                CoasterCurveDyeInteraction.clearDyeFromPreview(player, payload.curveOwner(), payload.curveRemote(), payload.soundPos(), payload.hand());
            }
        });
    }
}

