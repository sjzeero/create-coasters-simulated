package dev.silvergold.simulatedcoasters.track.chainlift;

import com.simibubi.create.AllItems;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChainLiftShaftCyclePayload(BlockPos anchorPos) implements CustomPacketPayload {
   public static final Type<ChainLiftShaftCyclePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "chain_lift_shaft_cycle"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ChainLiftShaftCyclePayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, ChainLiftShaftCyclePayload::anchorPos, ChainLiftShaftCyclePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(ChainLiftShaftCyclePayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            if (AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem())) {
               int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
               if (player.canInteractWithBlock(payload.anchorPos(), (double)maxRange)) {
                  if (player.level().getBlockEntity(payload.anchorPos()) instanceof CoasterAnchorpointBlockEntity be) {
                     if (be.hasChainLiftShaft()) {
                        be.cycleChainLiftShaftAxis();
                     }
                  }
               }
            }
         }
      });
   }
}
