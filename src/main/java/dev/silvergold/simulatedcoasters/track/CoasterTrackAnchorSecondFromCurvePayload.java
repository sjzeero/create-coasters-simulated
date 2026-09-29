package dev.silvergold.simulatedcoasters.track;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterTrackAnchorSecondFromCurvePayload(BlockPos secondAnchor, BlockPos firstAnchor) implements CustomPacketPayload {
   public static final Type<CoasterTrackAnchorSecondFromCurvePayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_track_anchor_second_from_curve")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, CoasterTrackAnchorSecondFromCurvePayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      CoasterTrackAnchorSecondFromCurvePayload::secondAnchor,
      BlockPos.STREAM_CODEC,
      CoasterTrackAnchorSecondFromCurvePayload::firstAnchor,
      CoasterTrackAnchorSecondFromCurvePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(CoasterTrackAnchorSecondFromCurvePayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof CoasterTrackItem) {
               BlockPos first = payload.firstAnchor();
               if (first != null && !first.equals(BlockPos.ZERO)) {
                  CoasterTrackPlacement.commitSecondAnchorOnServer(player, InteractionHand.MAIN_HAND, first, payload.secondAnchor());
               }
            }
         }
      });
   }
}
