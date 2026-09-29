/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacementNeighborSmoothSuppress;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterTrackPlacementNeighborSmoothSuppressPayload(boolean suppressNeighborSmoothing) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CoasterTrackPlacementNeighborSmoothSuppressPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track_neighbor_smooth_suppress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoasterTrackPlacementNeighborSmoothSuppressPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.BOOL, CoasterTrackPlacementNeighborSmoothSuppressPayload::suppressNeighborSmoothing, CoasterTrackPlacementNeighborSmoothSuppressPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(CoasterTrackPlacementNeighborSmoothSuppressPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)patt0$temp;
                CoasterTrackPlacementNeighborSmoothSuppress.setServerSuppress(player.getUUID(), payload.suppressNeighborSmoothing());
            }
        });
    }
}

