/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.item;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSnap;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlacementCollision;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartSpawner;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTangentOrientation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CoasterCartItem
extends Item {
    public CoasterCartItem(Item.Properties properties) {
        super(properties);
    }

    public static void clearLinkSelection(ItemStack stack) {
        CoasterCartLinkSelection.clearLinkTarget(stack);
    }

    public static boolean hasLinkSelection(ItemStack stack) {
        return CoasterCartLinkSelection.hasLinkTarget(stack);
    }

    public boolean isFoil(ItemStack stack) {
        return CoasterCartLinkSelection.hasLinkTarget(stack);
    }

    public InteractionResult useOn(UseOnContext context) {
        boolean onConnectedAnchor;
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || context.getHand() == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        if (player.isShiftKeyDown() && CoasterCartItem.hasLinkSelection(stack)) {
            CoasterCartItem.clearLinkSelection(stack);
            player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.cart.link_cleared").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }
        SubLevel clickedCart = CoasterCartResolve.findCartSubLevelAt(level, clicked);
        if (clickedCart != null) {
            if (!level.isClientSide()) {
                if (clickedCart instanceof ServerSubLevel) {
                    ServerSubLevel serverCart = (ServerSubLevel)clickedCart;
                    CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling((ServerLevel)level, serverCart);
                }
                CoasterCartLinkSelection.applyLinkTarget(level, stack, clickedCart);
                SimulatedCoasters.LOGGER.info("[cart-link] selected cart {} for linking", (Object)clickedCart.getUniqueId());
                player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.cart.link_selected").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (AnchorPeerFakeTracks.isManagedFakeTrack(level, clicked)) {
            return InteractionResult.PASS;
        }
        BlockState clickedState = level.getBlockState(clicked);
        boolean bl = onConnectedAnchor = clickedState.getBlock() instanceof CoasterAnchorpointBlock && CoasterCartMidTrackPlacement.isAcceptedAnchor(level, clicked);
        if (onConnectedAnchor) {
            return this.tryPlaceOnTrack(context, level, player, stack, clicked);
        }
        SubLevel hostSub = CoasterCartResolve.findSubLevelAt(level, clicked);
        if (hostSub != null) {
            return this.tryPlaceOnSubLevel(context, level, player, stack, hostSub);
        }
        if (!level.isClientSide()) {
            player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.must_place_on_track_or_sublevel"), true);
        }
        return InteractionResult.FAIL;
    }

    private InteractionResult tryPlaceOnSubLevel(UseOnContext context, Level level, Player player, ItemStack stack, SubLevel hostSub) {
        ServerPlayer sp;
        ServerLevel sl;
        block9: {
            block8: {
                if (CoasterCartResolve.isCartSubLevel(hostSub)) {
                    if (!level.isClientSide()) {
                        player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.sublevel_already_has_cart"), true);
                    }
                    return InteractionResult.FAIL;
                }
                if (level.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }
                if (!(level instanceof ServerLevel)) break block8;
                sl = (ServerLevel)level;
                if (hostSub instanceof ServerSubLevel) break block9;
            }
            return InteractionResult.PASS;
        }
        ServerSubLevel serverSub = (ServerSubLevel)hostSub;
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos placePos = clicked.relative(face);
        ServerSubLevel cart = CoasterCartSpawner.retrofitSubLevelAsCart(serverSub, level, placePos);
        if (cart == null) {
            player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.bogie.no_room_for_placement"), true);
            return InteractionResult.FAIL;
        }
        CoasterCartTrainLinkConstraint.tryLinkFromItemSelection(sl, cart, stack, player instanceof ServerPlayer ? (sp = (ServerPlayer)player) : null);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult tryPlaceOnTrack(UseOnContext context, Level level, Player player, ItemStack stack, BlockPos clicked) {
        ServerPlayer sp;
        ServerPlayer sp2;
        boolean suppressLinkSnap;
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.PASS;
        }
        ServerLevel sl = (ServerLevel)level;
        @Nullable CoasterCartMidTrackPlacement.PlacementPose pose = CoasterCartMidTrackPlacement.resolveFromAnchor((Level)sl, player, clicked, player.getLookAngle(), context.getClickLocation(), null);
        if (pose == null) {
            return InteractionResult.FAIL;
        }
        boolean linkCoupling = CoasterCartLinkPlacement.isLinkCouplingPlacement(stack);
        boolean bl = suppressLinkSnap = player instanceof ServerPlayer && CoasterCartLinkSnap.isSuppressLinkSnap((sp2 = (ServerPlayer)player).getUUID());
        if (CoasterCartLinkPlacement.rejectsLinkPlacement((Level)sl, stack, pose, null, suppressLinkSnap)) {
            player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_far"), true);
            return InteractionResult.FAIL;
        }
        if (CoasterCartPlacementCollision.hasSubLevelObstruction((Level)sl, (pose = CoasterCartLinkPlacement.applyLinkSnapIfSelected((Level)sl, stack, pose, null, suppressLinkSnap)).plotOriginWorld(), pose.orientation())) {
            player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.bogie.no_room_for_placement"), true);
            return InteractionResult.FAIL;
        }
        CoasterPathGraph graph = CoasterPathGraphManager.get(sl);
        CoasterPathTangentOrientation.alignConnectedComponents((Level)sl, graph);
        ServerSubLevel spawned = CoasterCartLinkPlacement.spawnPlacedCart(sl, pose, linkCoupling);
        if (spawned == null) {
            return InteractionResult.FAIL;
        }
        CoasterCartTrainLinkConstraint.tryLinkFromItemSelection(sl, spawned, stack, player instanceof ServerPlayer ? (sp = (ServerPlayer)player) : null);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}

