/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSnap;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlacementCollision;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterCartMidTrackPlacePayload(BlockPos rasterCell, Vec3 lookDirection, InteractionHand hand, boolean suppressLinkSnap) implements CustomPacketPayload
{
    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.DOUBLE, v -> v.x, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.y, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, InteractionHand> HAND_CODEC = StreamCodec.of((buf, hand) -> buf.writeVarInt(hand == InteractionHand.OFF_HAND ? 1 : 0), buf -> buf.readVarInt() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    public static final CustomPacketPayload.Type<CoasterCartMidTrackPlacePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_cart_mid_track_place"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoasterCartMidTrackPlacePayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, CoasterCartMidTrackPlacePayload::rasterCell, VEC3_CODEC, CoasterCartMidTrackPlacePayload::lookDirection, HAND_CODEC, CoasterCartMidTrackPlacePayload::hand, (StreamCodec)ByteBufCodecs.BOOL, CoasterCartMidTrackPlacePayload::suppressLinkSnap, CoasterCartMidTrackPlacePayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(CoasterCartMidTrackPlacePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            boolean suppressLinkSnap;
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            ServerLevel level = player.serverLevel();
            ItemStack stack = player.getItemInHand(payload.hand());
            if (!stack.is(SimulatedCoasters.COASTER_CART)) {
                return;
            }
            if (!player.mayBuild()) {
                return;
            }
            CoasterCartMidTrackPlacement.PlacementPose pose = CoasterCartMidTrackPlacement.resolveMidSpan((Level)level, (Player)player, payload.lookDirection(), null, payload.rasterCell());
            if (pose == null) {
                return;
            }
            boolean linkCoupling = CoasterCartLinkPlacement.isLinkCouplingPlacement(stack);
            boolean bl = suppressLinkSnap = payload.suppressLinkSnap() || CoasterCartLinkSnap.isSuppressLinkSnap(player.getUUID());
            if (CoasterCartLinkPlacement.rejectsLinkPlacement((Level)level, stack, pose, null, suppressLinkSnap)) {
                player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_far"), true);
                return;
            }
            pose = CoasterCartLinkPlacement.applyLinkSnapIfSelected((Level)level, stack, pose, null, suppressLinkSnap);
            double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 3.0;
            if (player.getEyePosition().distanceTo(pose.plotOriginWorld()) > reach) {
                return;
            }
            if (CoasterCartPlacementCollision.hasSubLevelObstruction((Level)level, pose.plotOriginWorld(), pose.orientation())) {
                player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.bogie.no_room_for_placement"), true);
                return;
            }
            ServerSubLevel spawned = CoasterCartLinkPlacement.spawnPlacedCart(level, pose, linkCoupling);
            if (spawned == null) {
                return;
            }
            CoasterCartTrainLinkConstraint.tryLinkFromItemSelection(level, spawned, stack, player);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.swing(payload.hand(), true);
        });
    }
}

