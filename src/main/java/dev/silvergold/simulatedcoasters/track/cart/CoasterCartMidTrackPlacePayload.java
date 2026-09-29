package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterCartMidTrackPlacePayload(BlockPos rasterCell, Vec3 lookDirection, InteractionHand hand, boolean suppressLinkSnap)
   implements CustomPacketPayload {
   private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, v -> v.x, ByteBufCodecs.DOUBLE, v -> v.y, ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new
   );
   private static final StreamCodec<RegistryFriendlyByteBuf, InteractionHand> HAND_CODEC = StreamCodec.of(
      (buf, hand) -> buf.writeVarInt(hand == InteractionHand.OFF_HAND ? 1 : 0),
      buf -> buf.readVarInt() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND
   );
   public static final Type<CoasterCartMidTrackPlacePayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_cart_mid_track_place")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, CoasterCartMidTrackPlacePayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      CoasterCartMidTrackPlacePayload::rasterCell,
      VEC3_CODEC,
      CoasterCartMidTrackPlacePayload::lookDirection,
      HAND_CODEC,
      CoasterCartMidTrackPlacePayload::hand,
      ByteBufCodecs.BOOL,
      CoasterCartMidTrackPlacePayload::suppressLinkSnap,
      CoasterCartMidTrackPlacePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(CoasterCartMidTrackPlacePayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(
         () -> {
            if (ctx.player() instanceof ServerPlayer player) {
               ServerLevel var11 = player.serverLevel();
               ItemStack stack = player.getItemInHand(payload.hand());
               if (stack.is(SimulatedCoasters.COASTER_CART)) {
                  if (player.mayBuild()) {
                     CoasterCartMidTrackPlacement.PlacementPose pose = CoasterCartMidTrackPlacement.resolveMidSpan(
                        var11, player, payload.lookDirection(), null, payload.rasterCell()
                     );
                     if (pose != null) {
                        boolean linkCoupling = CoasterCartLinkPlacement.isLinkCouplingPlacement(stack);
                        boolean suppressLinkSnap = payload.suppressLinkSnap() || CoasterCartLinkSnap.isSuppressLinkSnap(player.getUUID());
                        if (CoasterCartLinkPlacement.rejectsLinkPlacement(var11, stack, pose, null, suppressLinkSnap)) {
                           player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_far"), true);
                        } else {
                           pose = CoasterCartLinkPlacement.applyLinkSnapIfSelected(var11, stack, pose, null, suppressLinkSnap);
                           double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 3.0;
                           if (!(player.getEyePosition().distanceTo(pose.plotOriginWorld()) > reach)) {
                              if (CoasterCartPlacementCollision.hasSubLevelObstruction(var11, pose.plotOriginWorld(), pose.orientation())) {
                                 player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.bogie.no_room_for_placement"), true);
                              } else {
                                 ServerSubLevel spawned = CoasterCartLinkPlacement.spawnPlacedCart(var11, pose, linkCoupling);
                                 if (spawned != null) {
                                    CoasterCartTrainLinkConstraint.tryLinkFromItemSelection(var11, spawned, stack, player);
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
            }
         }
      );
   }
}
