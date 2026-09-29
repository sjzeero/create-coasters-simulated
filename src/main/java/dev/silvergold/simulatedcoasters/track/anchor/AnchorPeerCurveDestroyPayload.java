package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurveDrops;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AnchorPeerCurveDestroyPayload(BlockPos minedAnchor, BlockPos peerAnchor, boolean wrench) implements CustomPacketPayload {
   public static final Type<AnchorPeerCurveDestroyPayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "anchor_peer_curve_destroy")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, AnchorPeerCurveDestroyPayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      AnchorPeerCurveDestroyPayload::minedAnchor,
      BlockPos.STREAM_CODEC,
      AnchorPeerCurveDestroyPayload::peerAnchor,
      ByteBufCodecs.BOOL,
      AnchorPeerCurveDestroyPayload::wrench,
      AnchorPeerCurveDestroyPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(AnchorPeerCurveDestroyPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(
         () -> {
            if (ctx.player() instanceof ServerPlayer player) {
               ServerLevel var10 = player.serverLevel();
               int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
               if (player.canInteractWithBlock(payload.minedAnchor(), (double)maxRange)) {
                  if (var10.getBlockEntity(payload.minedAnchor()) instanceof CoasterAnchorpointBlockEntity anchor) {
                     Map<BlockPos, BezierConnection> curves = anchor.getAnchorPeerCurvesView();
                     if (curves.containsKey(payload.peerAnchor())) {
                        BezierConnection bc = curves.get(payload.peerAnchor());
                        BlockState anchorState = anchor.getBlockState();
                        SoundType sound = anchorState.getSoundType(var10, payload.minedAnchor(), player);
                        if (!player.isCreative()) {
                           if (payload.wrench()) {
                              CoasterTrackCurveDrops.addItemsToPlayer(player, bc);
                           } else {
                              CoasterTrackCurveDrops.spawnItems(var10, bc);
                           }
                        }

                        bc.spawnDestroyParticles(var10);
                        if (payload.wrench()) {
                           AllSoundEvents.WRENCH_REMOVE.playOnServer(var10, payload.minedAnchor(), 1.0F, var10.random.nextFloat() * 0.5F + 0.5F);
                        } else if (sound != null) {
                           var10.playSound(
                              null,
                              payload.minedAnchor(),
                              sound.getBreakSound(),
                              SoundSource.BLOCKS,
                              (sound.getVolume() + 1.0F) / 2.0F,
                              sound.getPitch() * 0.8F
                           );
                        }

                        anchor.removeAnchorPeerCurve(var10, payload.peerAnchor());
                     }
                  }
               }
            }
         }
      );
   }
}
