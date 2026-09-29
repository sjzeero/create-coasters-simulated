package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChainLiftCommitPayload(BlockPos startAnchor, ChainLiftEndRef endRef, Vec3 shaftAxis) implements CustomPacketPayload {
   private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, v -> v.x, ByteBufCodecs.DOUBLE, v -> v.y, ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new
   );
   public static final Type<ChainLiftCommitPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "chain_lift_commit"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ChainLiftCommitPayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      ChainLiftCommitPayload::startAnchor,
      ChainLiftEndRef.STREAM_CODEC,
      ChainLiftCommitPayload::endRef,
      VEC3_CODEC,
      ChainLiftCommitPayload::shaftAxis,
      ChainLiftCommitPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(ChainLiftCommitPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(
         () -> {
            if (ctx.player() instanceof ServerPlayer player) {
               if (player.getMainHandItem().is(Items.CHAIN) || player.getOffhandItem().is(Items.CHAIN)) {
                  Level level = player.level();
                  BlockPos start = payload.startAnchor().immutable();
                  if (level.getBlockState(start).getBlock() instanceof CoasterAnchorpointBlock) {
                     if (level.getBlockEntity(start) instanceof CoasterAnchorpointBlockEntity be) {
                        if (!be.hasChainLiftShaft()) {
                           Vec3 shaftAxis = payload.shaftAxis();
                           Direction facing = (Direction)level.getBlockState(start).getValue(CoasterAnchorpointBlock.FACING);
                           if (ChainLiftShaftAxis.isValidForFacing(shaftAxis, facing)) {
                              CoasterPathGraphSpans.SpanPath spanPath = ChainLiftSpan.resolveIdealPath(
                                 level, CoasterPathGraphLookup.get(level), start, payload.endRef(), 0.0
                              );
                              if (spanPath != null && CoasterPathGraphSpans.isWithinChainLiftSpan(spanPath.totalLength())) {
                                 if (!ChainLiftSpanOverlap.overlapsAnyExisting(level, spanPath, start, 0.0)) {
                                    be.commitChainLift(ChainLiftShaftAxis.toDominantAxis(shaftAxis), payload.endRef(), spanPath);
                                    ChainLiftPlacementSounds.playChainCommit(level, ChainLiftPlacementSounds.spanEndWorldPos(level, spanPath));
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      );
   }
}
