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

public record BezierLiftEditPayload(BlockPos anchorPos, Vec3 virtualTargetWorld) implements CustomPacketPayload {
   private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, v -> v.x, ByteBufCodecs.DOUBLE, v -> v.y, ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new
   );
   public static final Type<BezierLiftEditPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "bezier_lift_edit"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BezierLiftEditPayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, BezierLiftEditPayload::anchorPos, VEC3_CODEC, BezierLiftEditPayload::virtualTargetWorld, BezierLiftEditPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(BezierLiftEditPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            if (!(payload.virtualTargetWorld.lengthSqr() < 1.0E-12)) {
               int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
               if (player.canInteractWithBlock(payload.anchorPos(), (double)maxRange)) {
                  CoasterBezierHandleEdit.commitLift(player, payload.anchorPos(), payload.virtualTargetWorld());
               }
            }
         }
      });
   }
}
