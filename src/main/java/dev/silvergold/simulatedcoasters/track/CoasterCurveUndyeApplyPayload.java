package dev.silvergold.simulatedcoasters.track;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterCurveUndyeApplyPayload(BlockPos curveOwner, BlockPos curveRemote, Vec3 soundPos, InteractionHand hand) implements CustomPacketPayload {
   private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, v -> v.x, ByteBufCodecs.DOUBLE, v -> v.y, ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new
   );
   private static final StreamCodec<RegistryFriendlyByteBuf, InteractionHand> HAND_CODEC = StreamCodec.of(
      (buf, hand) -> buf.writeVarInt(hand == InteractionHand.OFF_HAND ? 1 : 0),
      buf -> buf.readVarInt() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND
   );
   public static final Type<CoasterCurveUndyeApplyPayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_curve_undye_apply")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, CoasterCurveUndyeApplyPayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      CoasterCurveUndyeApplyPayload::curveOwner,
      BlockPos.STREAM_CODEC,
      CoasterCurveUndyeApplyPayload::curveRemote,
      VEC3_CODEC,
      CoasterCurveUndyeApplyPayload::soundPos,
      HAND_CODEC,
      CoasterCurveUndyeApplyPayload::hand,
      CoasterCurveUndyeApplyPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(CoasterCurveUndyeApplyPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            CoasterCurveDyeInteraction.clearDyeFromPreview(player, payload.curveOwner(), payload.curveRemote(), payload.soundPos(), payload.hand());
         }
      });
   }
}
