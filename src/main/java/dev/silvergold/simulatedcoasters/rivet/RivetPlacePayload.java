package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RivetPlacePayload(BlockPos host, Direction face, Vec3 hitLocation, boolean freePosition, int yRotationDegrees, InteractionHand hand)
   implements CustomPacketPayload {
   public static final RivetPlacePayload INVALID = new RivetPlacePayload(BlockPos.ZERO, Direction.UP, Vec3.ZERO, false, 0, InteractionHand.MAIN_HAND);
   private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new
   );
   private static final StreamCodec<RegistryFriendlyByteBuf, InteractionHand> HAND_CODEC = StreamCodec.of(
      (buf, hand) -> buf.writeVarInt(hand == InteractionHand.OFF_HAND ? 1 : 0),
      buf -> buf.readVarInt() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND
   );
   public static final Type<RivetPlacePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "rivet_place"));
   public static final StreamCodec<RegistryFriendlyByteBuf, RivetPlacePayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      RivetPlacePayload::host,
      Direction.STREAM_CODEC,
      RivetPlacePayload::face,
      VEC3_CODEC,
      RivetPlacePayload::hitLocation,
      ByteBufCodecs.BOOL,
      RivetPlacePayload::freePosition,
      ByteBufCodecs.VAR_INT,
      RivetPlacePayload::yRotationDegrees,
      HAND_CODEC,
      RivetPlacePayload::hand,
      RivetPlacePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(RivetPlacePayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            ServerLevel var8 = player.serverLevel();
            ItemStack stack = player.getItemInHand(payload.hand());
            if (stack.is(SimulatedCoasters.RIVET)) {
               if (player.mayBuild()) {
                  BlockHitResult hit = RivetPlacement.findPlacementHit(var8, player);
                  if (hit != null) {
                     if (!RivetPlacement.looksAtExistingRivet(var8, player)) {
                        RivetPlacement.Resolved placement = RivetPlacement.resolve(hit, payload.freePosition()).withYRotation(payload.yRotationDegrees());
                        if (RivetPlacement.isValidPlacement(var8, placement)) {
                           ServerSubLevel placed = RivetSpawner.tryPlace(var8, placement);
                           if (placed != null) {
                              RivetPlaceAnimPayload.notifyPlacement(var8, placed.getUniqueId(), placement);
                              if (!player.getAbilities().instabuild) {
                                 stack.shrink(1);
                              }

                              player.swing(payload.hand(), true);
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
