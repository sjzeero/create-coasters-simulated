package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterTrackAnchorFirstFromCurvePayload(BlockPos anchor) implements CustomPacketPayload {
   public static final Type<CoasterTrackAnchorFirstFromCurvePayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_track_anchor_first_from_curve")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, CoasterTrackAnchorFirstFromCurvePayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, CoasterTrackAnchorFirstFromCurvePayload::anchor, CoasterTrackAnchorFirstFromCurvePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(CoasterTrackAnchorFirstFromCurvePayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            Level var8 = player.level();
            BlockPos anchor = payload.anchor();
            if (var8.getBlockState(anchor).getBlock() instanceof CoasterAnchorpointBlock) {
               if (var8.getBlockEntity(anchor) instanceof CoasterAnchorpointBlockEntity be) {
                  if (be.legCount() < 2) {
                     ItemStack stack = player.getMainHandItem();
                     if (stack.getItem() instanceof CoasterTrackItem) {
                        BlockPos existing = CoasterTrackPlacement.readAnchorFirstSelection(stack);
                        if (existing == null || !existing.equals(anchor)) {
                           CoasterTrackPlacement.applyAnchorFirstSelection(var8, stack, anchor, player, InteractionHand.MAIN_HAND);
                        }
                     }
                  }
               }
            }
         }
      });
   }
}
