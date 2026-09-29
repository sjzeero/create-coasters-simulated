package dev.silvergold.simulatedcoasters.track.chainlift;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
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

public record ChainLiftRemovePayload(BlockPos anchorPos, Vec3 soundWorldPos) implements CustomPacketPayload {
   private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, v -> v.x, ByteBufCodecs.DOUBLE, v -> v.y, ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new
   );
   public static final Type<ChainLiftRemovePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "chain_lift_remove"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ChainLiftRemovePayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, ChainLiftRemovePayload::anchorPos, VEC3_CODEC, ChainLiftRemovePayload::soundWorldPos, ChainLiftRemovePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(ChainLiftRemovePayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            if (player.isShiftKeyDown()) {
               if (AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem())) {
                  int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
                  Vec3 sound = payload.soundWorldPos();
                  if (Double.isFinite(sound.x) && Double.isFinite(sound.y) && Double.isFinite(sound.z)) {
                     boolean nearHost = player.canInteractWithBlock(payload.anchorPos(), (double)maxRange);
                     boolean nearClick = player.position().distanceToSqr(sound) <= (double)maxRange * (double)maxRange;
                     if (nearHost || nearClick) {
                        if (player.level().getBlockEntity(payload.anchorPos()) instanceof CoasterAnchorpointBlockEntity be) {
                           if (be.hasChainLiftShaft()) {
                              be.clearChainLiftShaft();
                              float pitch = player.serverLevel().random.nextFloat() * 0.5F + 0.5F;
                              AllSoundEvents.WRENCH_REMOVE.play(player.serverLevel(), null, sound, 1.0F, pitch);
                           }
                        }
                     }
                  }
               }
            }
         }
      });
   }
}
