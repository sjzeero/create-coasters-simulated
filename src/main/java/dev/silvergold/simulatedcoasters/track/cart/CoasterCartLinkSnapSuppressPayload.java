package dev.silvergold.simulatedcoasters.track.cart;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterCartLinkSnapSuppressPayload(boolean suppressLinkSnap) implements CustomPacketPayload {
   public static final Type<CoasterCartLinkSnapSuppressPayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_cart_link_snap_suppress")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, CoasterCartLinkSnapSuppressPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, CoasterCartLinkSnapSuppressPayload::suppressLinkSnap, CoasterCartLinkSnapSuppressPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(CoasterCartLinkSnapSuppressPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            CoasterCartLinkSnap.setServerSuppress(player.getUUID(), payload.suppressLinkSnap());
         }
      });
   }
}
