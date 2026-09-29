package dev.silvergold.simulatedcoasters.track;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterTrackPlacementNeighborSmoothSuppressPayload(boolean suppressNeighborSmoothing) implements CustomPacketPayload {
   public static final Type<CoasterTrackPlacementNeighborSmoothSuppressPayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_track_neighbor_smooth_suppress")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, CoasterTrackPlacementNeighborSmoothSuppressPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      CoasterTrackPlacementNeighborSmoothSuppressPayload::suppressNeighborSmoothing,
      CoasterTrackPlacementNeighborSmoothSuppressPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(CoasterTrackPlacementNeighborSmoothSuppressPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            CoasterTrackPlacementNeighborSmoothSuppress.setServerSuppress(player.getUUID(), payload.suppressNeighborSmoothing());
         }
      });
   }
}
