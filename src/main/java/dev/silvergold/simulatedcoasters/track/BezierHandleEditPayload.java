package dev.silvergold.simulatedcoasters.track;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BezierHandleEditPayload(BlockPos primaryHost, BlockPos remoteEnd, int endpointIndex, Vec3 handleOffsetWorld) implements CustomPacketPayload {
   private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, v -> v.x, ByteBufCodecs.DOUBLE, v -> v.y, ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new
   );
   public static final Type<BezierHandleEditPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "bezier_handle_edit"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BezierHandleEditPayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      BezierHandleEditPayload::primaryHost,
      BlockPos.STREAM_CODEC,
      BezierHandleEditPayload::remoteEnd,
      ByteBufCodecs.VAR_INT,
      BezierHandleEditPayload::endpointIndex,
      VEC3_CODEC,
      BezierHandleEditPayload::handleOffsetWorld,
      BezierHandleEditPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(BezierHandleEditPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(
         () -> {
            if (ctx.player() instanceof ServerPlayer player) {
               if (payload.endpointIndex == 0 || payload.endpointIndex == 1) {
                  if (!(payload.handleOffsetWorld.lengthSqr() < 1.0E-12)) {
                     int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
                     if (player.canInteractWithBlock(payload.primaryHost(), (double)maxRange)
                        || player.canInteractWithBlock(payload.remoteEnd(), (double)maxRange)) {
                        CoasterBezierHandleEdit.commit(player, payload.primaryHost(), payload.remoteEnd(), payload.endpointIndex(), payload.handleOffsetWorld());
                     }
                  }
               }
            }
         }
      );
   }
}
