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
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BezierLiftEditPayload(BlockPos anchorPos, Vec3 virtualTargetWorld) implements CustomPacketPayload
{
    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.DOUBLE, v -> v.x, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.y, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new);
    public static final CustomPacketPayload.Type<BezierLiftEditPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"bezier_lift_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BezierLiftEditPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, BezierLiftEditPayload::anchorPos, VEC3_CODEC, BezierLiftEditPayload::virtualTargetWorld, BezierLiftEditPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(BezierLiftEditPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            if (payload.virtualTargetWorld.lengthSqr() < 1.0E-12) {
                return;
            }
            int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
            if (!player.canInteractWithBlock(payload.anchorPos(), (double)maxRange)) {
                return;
            }
            CoasterBezierHandleEdit.commitLift(player, payload.anchorPos(), payload.virtualTargetWorld());
        });
    }
}

