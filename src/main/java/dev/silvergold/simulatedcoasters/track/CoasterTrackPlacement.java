/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllDataComponents
 *  com.simibubi.create.AllSoundEvents
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$Segment
 *  com.simibubi.create.content.trains.track.TrackBlock
 *  com.simibubi.create.content.trains.track.TrackMaterial
 *  com.simibubi.create.content.trains.track.TrackPlacement$ConnectingFrom
 *  com.simibubi.create.foundation.utility.CreateLang
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.createmod.catnip.animation.LerpedFloat
 *  net.createmod.catnip.animation.LerpedFloat$Chaser
 *  net.createmod.catnip.data.Couple
 *  net.createmod.catnip.data.Iterate
 *  net.createmod.catnip.data.Pair
 *  net.createmod.catnip.math.AngleHelper
 *  net.createmod.catnip.math.VecHelper
 *  net.createmod.catnip.outliner.Outliner
 *  net.createmod.catnip.theme.Color
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.CustomData
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackPlacement;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveOutlineSupport;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorClientSpace;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveRailInstancing;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackOpenEndpointOutlineClient;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackPlacementNeighborSmoothSuppressClient;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackPlacementOverlayRenderer;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackPlacementSmoothHud;
import dev.silvergold.simulatedcoasters.track.BezierBankBlendLevel;
import dev.silvergold.simulatedcoasters.track.BezierRailSpread;
import dev.silvergold.simulatedcoasters.track.CoasterAnchorBezierOptimizer;
import dev.silvergold.simulatedcoasters.track.CoasterBezierEndpointData;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterCircularLoopPlacement;
import dev.silvergold.simulatedcoasters.track.CoasterHalfCirclePlacement;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterQuarterCirclePlacement;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurveDrops;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.CoasterTrackHotbarSelectionSync;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacementCommitDeferral;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacementNeighborSmoothSuppress;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterTrackPropagator;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public final class CoasterTrackPlacement {
    private static final double COASTER_FIRST_LEG_AXIS_TIE_SQ = 0.0025;
    private static final String ANCHOR_FIRST_CUSTOM_TAG = "SimulatedCoastersAnchorFirst";
    public static PlacementInfo cached;
    static BlockPos hoveringPos;
    static boolean hoveringMaxed;
    static int hoveringAngle;
    static boolean hoveringSuppressNeighborSmooth;
    static ItemStack lastItem;
    static int extraTipWarmup;
    static LerpedFloat animation;
    static int lastLineCount;
    public static final int CONNECTION_VALID_GREEN = 9817409;
    public static final int ANCHOR_SELECTION_OUTLINE_COLOR = 1940687;
    private static final int COASTER_PREVIEW_VALID_COLOR = 1940687;
    private static final int COASTER_PREVIEW_INVALID_COLOR = 15359019;
    private static final String COASTER_PLACEMENT_OUTLINE_CURVE = "curve";
    private static final String COASTER_PLACEMENT_OUTLINE_NEIGHBOR_CURVE = "curve_neighbor";
    private static final String COASTER_PLACEMENT_OUTLINE_START = "start";
    private static final String COASTER_PLACEMENT_OUTLINE_ANCHOR_FIRST = "anchor_first";
    private static final String COASTER_PLACEMENT_OUTLINE_ANCHOR_HOVER = "anchor_hover";
    private static final String COASTER_PLACEMENT_OUTLINE_INVALID_TARGET = "invalid_target";
    private static final int COASTER_PLACEMENT_MAX_CURVE_LINE_KEY = 4096;
    private static final double PLACEMENT_PREVIEW_MAX_LENGTH_SPAN_FACTOR = 1.5;
    static int[] lastNeighborLineCounts;

    public static void setAnchorFirstSelection(ItemStack stack, BlockPos anchor) {
        BlockPos immutable = anchor.immutable();
        CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, (Object)CustomData.EMPTY)).copyTag();
        tag.putLong(ANCHOR_FIRST_CUSTOM_TAG, immutable.asLong());
        stack.set(DataComponents.CUSTOM_DATA, (Object)CustomData.of((CompoundTag)tag));
        Vec3 center = Vec3.atCenterOf((Vec3i)immutable);
        stack.set(AllDataComponents.TRACK_CONNECTING_FROM, (Object)new TrackPlacement.ConnectingFrom(immutable, Vec3.ZERO, new Vec3(0.0, 1.0, 0.0), center));
    }

    public static void clearAnchorFirstSelection(ItemStack stack) {
        stack.remove(AllDataComponents.TRACK_CONNECTING_FROM);
        if (!stack.has(DataComponents.CUSTOM_DATA)) {
            return;
        }
        CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
        if (!tag.contains(ANCHOR_FIRST_CUSTOM_TAG)) {
            return;
        }
        tag.remove(ANCHOR_FIRST_CUSTOM_TAG);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, (Object)CustomData.of((CompoundTag)tag));
        }
    }

    public static BlockPos readAnchorFirstSelection(ItemStack stack) {
        TrackPlacement.ConnectingFrom from;
        if (stack.getItem() instanceof CoasterTrackItem && (from = (TrackPlacement.ConnectingFrom)stack.get(AllDataComponents.TRACK_CONNECTING_FROM)) != null) {
            return from.pos();
        }
        if (!stack.has(DataComponents.CUSTOM_DATA)) {
            return null;
        }
        CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
        if (!tag.contains(ANCHOR_FIRST_CUSTOM_TAG, 4)) {
            return null;
        }
        return BlockPos.of((long)tag.getLong(ANCHOR_FIRST_CUSTOM_TAG));
    }

    public static boolean hasAnchorFirstSelection(ItemStack stack) {
        if (stack.getItem() instanceof CoasterTrackItem && stack.has(AllDataComponents.TRACK_CONNECTING_FROM)) {
            return true;
        }
        return CoasterTrackPlacement.readAnchorFirstSelection(stack) != null;
    }

    public static void applyAnchorFirstSelection(Level level, ItemStack stack, BlockPos anchor) {
        CoasterTrackPlacement.applyAnchorFirstSelection(level, stack, anchor, null, InteractionHand.MAIN_HAND);
    }

    public static void applyAnchorFirstSelection(Level level, ItemStack stack, BlockPos anchor, @Nullable Player player, InteractionHand hand) {
        stack.remove(AllDataComponents.TRACK_CONNECTING_FROM);
        CoasterTrackPlacement.setAnchorFirstSelection(stack, anchor.immutable());
        level.playSound(null, anchor, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75f, 1.0f);
        if (!level.isClientSide() && player != null) {
            player.setItemInHand(hand, stack);
            if (player instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)player;
                CoasterTrackHotbarSelectionSync.syncMainHand(sp, hand, anchor);
            }
        }
    }

    public static boolean tryConnectSecondAnchorFromCurve(Level level, Player player, ItemStack stack, BlockPos secondAnchor) {
        return CoasterTrackPlacement.tryConnectSecondAnchorFromCurve(level, player, stack, secondAnchor, InteractionHand.MAIN_HAND);
    }

    public static boolean tryConnectSecondAnchorFromCurve(Level level, Player player, ItemStack stack, BlockPos secondAnchor, InteractionHand hand) {
        if (!(stack.getItem() instanceof CoasterTrackItem)) {
            return false;
        }
        if (!CoasterTrackPlacement.hasAnchorFirstSelection(stack)) {
            return false;
        }
        BlockPos first = CoasterTrackPlacement.readAnchorFirstSelection(stack);
        if (first == null || first.equals((Object)secondAnchor)) {
            return false;
        }
        BlockState state = level.getBlockState(secondAnchor);
        if (!(state.getBlock() instanceof CoasterAnchorpointBlock)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(secondAnchor);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        if (be.legCount() >= 2) {
            return false;
        }
        stack.remove(AllDataComponents.TRACK_EXTENDED_CURVE);
        PlacementInfo info = CoasterTrackPlacement.tryConnect(level, player, secondAnchor, state, stack, false, true, false);
        CoasterTrackPlacement.displayPlacementActionBar(player, info);
        if (!info.valid) {
            if (!level.isClientSide()) {
                AllSoundEvents.DENY.playFrom((Entity)player, 1.0f, 1.0f);
            }
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof CoasterTrackItem) {
            CoasterTrackItem.clearPendingConnection(stack);
            player.setItemInHand(hand, stack);
            if (player instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)player;
                CoasterTrackHotbarSelectionSync.syncMainHand(sp, hand, null);
            }
        }
        return true;
    }

    public static boolean commitSecondAnchorOnServer(ServerPlayer player, InteractionHand hand, BlockPos firstAnchor, BlockPos secondAnchor) {
        boolean ok;
        if (firstAnchor == null || firstAnchor.equals((Object)BlockPos.ZERO) || firstAnchor.equals((Object)secondAnchor)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CoasterTrackItem)) {
            return false;
        }
        if (CoasterTrackPlacement.anchorsAlreadyLinked((Level)level, firstAnchor, secondAnchor)) {
            CoasterTrackPlacement.clearServerTrackSelection(player, hand);
            return true;
        }
        if (CoasterTrackPlacementCommitDeferral.isEdgeCommitPending(level, firstAnchor, secondAnchor)) {
            CoasterTrackPlacement.clearServerTrackSelection(player, hand);
            return true;
        }
        BlockPos existing = CoasterTrackPlacement.readAnchorFirstSelection(stack);
        if (existing == null || !existing.equals((Object)firstAnchor)) {
            CoasterTrackPlacement.applyAnchorFirstSelection((Level)level, stack, firstAnchor, (Player)player, hand);
            stack = player.getItemInHand(hand);
        }
        if (!(ok = CoasterTrackPlacement.tryConnectSecondAnchorFromCurve((Level)level, (Player)player, stack, secondAnchor, hand))) {
            CoasterTrackPlacement.clearServerTrackSelection(player, hand);
        }
        return ok;
    }

    public static void clearServerTrackSelection(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CoasterTrackItem)) {
            return;
        }
        CoasterTrackItem.clearPendingConnection(stack);
        player.setItemInHand(hand, stack);
        CoasterTrackHotbarSelectionSync.syncMainHand(player, hand, null);
    }

    private static boolean anchorsAlreadyLinked(Level level, BlockPos anchorA, BlockPos anchorB) {
        BlockEntity blockEntity = level.getBlockEntity(anchorA);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity beA = (CoasterAnchorpointBlockEntity)blockEntity;
        return beA.getAnchorPeerCurvesView().containsKey(anchorB);
    }

    private static boolean suppressNeighborSmoothing(Level level, Player player) {
        if (level.isClientSide()) {
            return CoasterTrackPlacementNeighborSmoothSuppressClient.isSuppressNeighborSmoothing();
        }
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            return CoasterTrackPlacementNeighborSmoothSuppress.isSuppress(sp.getUUID());
        }
        return false;
    }

    public static PlacementInfo tryConnect(Level level, Player player, BlockPos pos2, BlockState state2, ItemStack stack, boolean girder, boolean maximiseTurn, boolean updatePlacementCache) {
        BlockPos anchorFirst;
        boolean touchCache;
        Vec3 lookVec = player.getLookAngle();
        int lookAngle = (int)(22.5 + (double)(AngleHelper.deg((double)Mth.atan2((double)lookVec.z, (double)lookVec.x)) % 360.0f)) / 8;
        boolean suppressNeighborSmooth = CoasterTrackPlacement.suppressNeighborSmoothing(level, player);
        boolean bl = touchCache = level.isClientSide() && updatePlacementCache;
        if (touchCache && cached != null && pos2.equals((Object)hoveringPos) && stack.equals(lastItem) && hoveringMaxed == maximiseTurn && lookAngle == hoveringAngle && hoveringSuppressNeighborSmooth == suppressNeighborSmooth) {
            return cached;
        }
        if (stack.getItem() instanceof CoasterTrackItem) {
            if (CoasterTrackPlacement.isCreateTrainTrack(state2.getBlock())) {
                return CoasterTrackPlacement.denyCoasterCrossPreview(level, pos2, stack, maximiseTurn, lookAngle, suppressNeighborSmooth);
            }
            TrackPlacement.ConnectingFrom from = (TrackPlacement.ConnectingFrom)stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
            if (from != null && CoasterTrackPlacement.isCreateTrainTrack(level.getBlockState(from.pos()).getBlock())) {
                return CoasterTrackPlacement.denyCoasterCrossPreview(level, pos2, stack, maximiseTurn, lookAngle, suppressNeighborSmooth);
            }
        }
        PlacementInfo info = new PlacementInfo(CoasterTrackMaterials.COASTER);
        if (touchCache) {
            hoveringMaxed = maximiseTurn;
            hoveringAngle = lookAngle;
            hoveringSuppressNeighborSmooth = suppressNeighborSmooth;
            hoveringPos = pos2;
            lastItem = stack;
            cached = info;
        }
        BlockPos blockPos = anchorFirst = stack.getItem() instanceof CoasterTrackItem ? CoasterTrackPlacement.readAnchorFirstSelection(stack) : null;
        if (stack.getItem() instanceof CoasterTrackItem) {
            if (!(state2.getBlock() instanceof CoasterAnchorpointBlock)) {
                return CoasterTrackPlacement.denyCoasterAnchorsOnlyPlacement(info);
            }
            if (anchorFirst == null) {
                return info.withMessage("second_point").tooJumbly();
            }
            if (anchorFirst.equals((Object)pos2)) {
                return info.withMessage("second_point").tooJumbly();
            }
            return CoasterTrackPlacement.tryConnectAnchorToAnchor(level, player, anchorFirst, pos2, state2, stack, girder, info);
        }
        return info.withMessage("second_point").tooJumbly();
    }

    private static PlacementInfo tryConnectAnchorToAnchor(Level level, Player player, BlockPos anchorFirstPos, BlockPos anchorSecondPos, BlockState anchorSecondState, ItemStack stack, boolean girder, PlacementInfo info) {
        CoasterAnchorpointBlockEntity beA;
        BlockEntity blockEntity;
        block13: {
            block12: {
                if (!anchorSecondState.is((Block)SimulatedCoastersBlocks.COASTER_ANCHORPOINT.get())) {
                    return info.withMessage("second_point").tooJumbly();
                }
                if (anchorFirstPos.equals((Object)anchorSecondPos)) {
                    return info.withMessage("second_point").tooJumbly();
                }
                CoasterTrackPlacement.populatePreviewTrackRequirements(level, player, stack, anchorFirstPos, anchorSecondPos, info);
                if (CoasterTrackPlacement.anchorConnectionAcrossSubLevels(level, anchorFirstPos, anchorSecondPos)) {
                    return info.withModTranslationKey("simulatedcoasters.track.anchor_sublevel_connection").tooJumbly();
                }
                if (CoasterTrackPlacement.anchorConnectionExceedsMaxSpan(level, anchorFirstPos, anchorSecondPos)) {
                    return info.withMessage("too_far").tooJumbly();
                }
                blockEntity = level.getBlockEntity(anchorFirstPos);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block12;
                beA = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(anchorSecondPos);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block13;
            }
            return info.withMessage("original_missing").tooJumbly();
        }
        CoasterAnchorpointBlockEntity beB = (CoasterAnchorpointBlockEntity)blockEntity;
        if (!beA.canAcceptAnchorPeer(anchorSecondPos) || !beB.canAcceptAnchorPeer(anchorFirstPos)) {
            return info.withModTranslationKey("simulatedcoasters.track.anchor_full").tooJumbly();
        }
        BlockState anchorFirstState = level.getBlockState(anchorFirstPos);
        if (CoasterAnchorpointBlock.haveOppositeFacing(anchorFirstState, anchorSecondState)) {
            return info.withModTranslationKey("simulatedcoasters.track.anchor_opposite_facing").tooJumbly();
        }
        if (CoasterAnchorpointBlock.haveSameFacingCollinearAlongFacing(anchorFirstState, anchorSecondState, anchorFirstPos, anchorSecondPos)) {
            return info.withModTranslationKey("simulatedcoasters.track.anchor_same_facing_collinear").tooJumbly();
        }
        if (CoasterAnchorpointBlock.connectionHasImpossibleBend(anchorFirstState, anchorSecondState, anchorFirstPos, anchorSecondPos)) {
            return info.withModTranslationKey("simulatedcoasters.track.anchor_impossible_bend").tooJumbly();
        }
        if (CoasterAnchorpointBlock.connectionReversesOnSameFacingCoplanarPlane(level, anchorFirstPos, anchorSecondPos)) {
            return info.withModTranslationKey("simulatedcoasters.track.anchor_coplanar_plane_reversal").tooJumbly();
        }
        CoasterTrackPlacement.tryConnectAnchorToAnchorAttempt(level, player, anchorFirstPos, anchorSecondPos, stack, girder, info);
        return info;
    }

    private static void populatePreviewTrackRequirements(Level level, Player player, ItemStack stack, BlockPos anchorA, BlockPos anchorB, PlacementInfo info) {
        info.requiredTracks = CoasterTrackCurveDrops.trackItemCostForAnchorPair(level, anchorA, anchorB);
        info.hasRequiredTracks = player.isCreative() || CoasterTrackPlacement.countCoasterTracksInInventory(player, stack) >= info.requiredTracks;
    }

    private static int countCoasterTracksInInventory(Player player, ItemStack trackStack) {
        int found = 0;
        Inventory inv = player.getInventory();
        int size = inv.items.size();
        for (int j = 0; j <= size + 1; ++j) {
            boolean offhand;
            int i = j;
            boolean bl = offhand = j == size + 1;
            if (j == size) {
                i = inv.selected;
            } else if (offhand) {
                i = 0;
            } else if (j == inv.selected) continue;
            ItemStack stackInSlot = (ItemStack)(offhand ? inv.offhand : inv.items).get(i);
            if (!stackInSlot.is(trackStack.getItem())) continue;
            found += stackInSlot.getCount();
        }
        return found;
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void updateTrackCostOverlay(Minecraft mc, Player player, ItemStack stack, int requiredTracks, boolean hasRequiredTracks, boolean validPreview) {
        CoasterTrackPlacementOverlayRenderer.updateFromPlacementPreview(mc, requiredTracks, hasRequiredTracks, validPreview);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void updateTrackCostOverlayForPreviewEndpoints(Minecraft mc, Level level, Player player, ItemStack stack, BlockPos fromAnchor, BlockPos previewEnd) {
        int required = CoasterTrackCurveDrops.trackItemCostForPreviewEndpoints(level, fromAnchor, previewEnd);
        boolean hasEnough = player.isCreative() || CoasterTrackPlacement.countCoasterTracksInInventory(player, stack) >= required;
        CoasterTrackPlacement.updateTrackCostOverlay(mc, player, stack, required, hasEnough, false);
    }

    public static boolean anchorConnectionAcrossSubLevels(Level level, BlockPos anchorA, BlockPos anchorB) {
        SubLevel subB;
        SubLevel subA = Sable.HELPER.getContaining(level, (Vec3i)anchorA);
        return subA != (subB = Sable.HELPER.getContaining(level, (Vec3i)anchorB));
    }

    public static boolean anchorConnectionExceedsMaxSpan(Level level, BlockPos anchorA, BlockPos anchorB) {
        long maxSq = CoasterTrackGauge.maxCoasterCurveEndpointSpanBlocksSq();
        if (anchorA.distSqr((Vec3i)anchorB) > (double)maxSq) {
            return true;
        }
        return CoasterAnchorpointBlockEntity.worldCenter(level, anchorA).distanceToSqr(CoasterAnchorpointBlockEntity.worldCenter(level, anchorB)) > (double)maxSq;
    }

    public static boolean anchorConnectionTooFar(Level level, BlockPos anchorA, BlockPos anchorB) {
        return CoasterTrackPlacement.anchorConnectionAcrossSubLevels(level, anchorA, anchorB) || CoasterTrackPlacement.anchorConnectionExceedsMaxSpan(level, anchorA, anchorB);
    }

    static boolean placementHasNeighborsToCoSmooth(Level level, BlockPos anchorA, BlockPos anchorB) {
        return CoasterTrackPlacement.anchorHasExistingLegsBesides(level, anchorA, anchorB) || CoasterTrackPlacement.anchorHasExistingLegsBesides(level, anchorB, anchorA);
    }

    private static boolean anchorHasExistingLegsBesides(Level level, BlockPos anchor, BlockPos excludePeer) {
        BlockEntity blockEntity = level.getBlockEntity(anchor);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
            if (peer.equals((Object)excludePeer)) continue;
            return true;
        }
        return false;
    }

    private static void tryConnectAnchorToAnchorAttempt(Level level, Player player, BlockPos anchorFirstPos, BlockPos anchorSecondPos, ItemStack stack, boolean girder, PlacementInfo info) {
        info.valid = false;
        if (CoasterTrackPlacement.anchorConnectionTooFar(level, anchorFirstPos, anchorSecondPos)) {
            return;
        }
        boolean suppressNeighborSmooth = CoasterTrackPlacement.suppressNeighborSmoothing(level, player);
        CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult placement = null;
        if (!suppressNeighborSmooth) {
            placement = CoasterCircularLoopPlacement.tryBuildCircularLoop(level, anchorFirstPos, anchorSecondPos, CoasterTrackMaterials.COASTER, girder);
            if (placement == null) {
                placement = CoasterHalfCirclePlacement.tryBuildHalfCircle(level, anchorFirstPos, anchorSecondPos, CoasterTrackMaterials.COASTER, girder);
            }
            if (placement == null) {
                placement = CoasterQuarterCirclePlacement.tryBuildQuarterCircle(level, anchorFirstPos, anchorSecondPos, CoasterTrackMaterials.COASTER, girder);
            }
        }
        if (placement == null) {
            placement = CoasterAnchorBezierOptimizer.buildAnchorAnchorPlacement(level, anchorFirstPos, anchorSecondPos, CoasterTrackMaterials.COASTER, girder, suppressNeighborSmooth);
        }
        if (placement == null) {
            info.withMessage("too_sharp").tooJumbly();
            return;
        }
        if (!CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(placement.primary())) {
            info.withMessage("too_sharp").tooJumbly();
            return;
        }
        if (suppressNeighborSmooth && !CoasterAnchorBezierOptimizer.meetsAltSuppressMinCurveRadius(placement.primary())) {
            info.withMessage("too_sharp").tooJumbly();
            return;
        }
        if (CoasterAnchorpointBlock.connectionBelowMinLegAngle(level, anchorFirstPos, anchorSecondPos, placement.primary())) {
            info.withModTranslationKey("simulatedcoasters.track.anchor_leg_angle_too_sharp").tooJumbly();
            return;
        }
        info.curve = placement.primary();
        info.anchorAnchorBuildResult = placement;
        info.pos1 = anchorFirstPos;
        info.pos2 = anchorSecondPos;
        info.anchorToAnchor = true;
        info.valid = true;
        CoasterTrackPlacement.finalizeAnchorToAnchorConnection(level, player, anchorFirstPos, anchorSecondPos, stack, info);
    }

    private static PlacementInfo finalizeAnchorToAnchorConnection(Level level, Player player, BlockPos anchorA, BlockPos anchorB, ItemStack stack, PlacementInfo info) {
        CoasterAnchorpointBlockEntity beLo;
        BlockPos hi;
        BlockPos lo;
        ServerLevel sl;
        block23: {
            block22: {
                info.requiredTracks = CoasterTrackCurveDrops.trackItemCostForAnchorPair(level, anchorA, anchorB);
                info.hasRequiredTracks = true;
                if (!player.isCreative()) {
                    for (boolean simulate : Iterate.trueAndFalse) {
                        if (level.isClientSide() && !simulate) break;
                        int tracks = info.requiredTracks;
                        int foundTracks = 0;
                        Inventory inv = player.getInventory();
                        int size = inv.items.size();
                        for (int j = 0; j <= size + 1; ++j) {
                            boolean offhand;
                            int i = j;
                            boolean bl = offhand = j == size + 1;
                            if (j == size) {
                                i = inv.selected;
                            } else if (offhand) {
                                i = 0;
                            } else if (j == inv.selected) continue;
                            ItemStack stackInSlot = (ItemStack)(offhand ? inv.offhand : inv.items).get(i);
                            if (!stackInSlot.is(stack.getItem()) || foundTracks >= tracks) continue;
                            int count = stackInSlot.getCount();
                            if (!simulate) {
                                int remainingItems = count - Math.min(tracks - foundTracks, count);
                                if (i == inv.selected) {
                                    stackInSlot.remove(AllDataComponents.TRACK_CONNECTING_FROM);
                                    if (stackInSlot.getItem() instanceof CoasterTrackItem) {
                                        CoasterTrackPlacement.clearAnchorFirstSelection(stackInSlot);
                                    }
                                }
                                ItemStack newItem = stackInSlot.copyWithCount(remainingItems);
                                if (offhand) {
                                    player.setItemInHand(InteractionHand.OFF_HAND, newItem);
                                } else {
                                    inv.setItem(i, newItem);
                                }
                            }
                            foundTracks += count;
                        }
                        if (!simulate || foundTracks >= tracks) continue;
                        info.valid = false;
                        info.tooJumbly();
                        info.hasRequiredTracks = false;
                        return info.withMessage("not_enough_tracks");
                    }
                    if (!level.isClientSide()) {
                        ItemStack selected = player.getInventory().getItem(player.getInventory().selected);
                        player.setItemInHand(InteractionHand.MAIN_HAND, selected);
                    }
                }
                if (level.isClientSide()) {
                    if (info.curve != null && info.valid) {
                        BezierConnection bc;
                        long bL;
                        long aL = anchorA.asLong();
                        BlockPos lo2 = aL <= (bL = anchorB.asLong()) ? anchorA : anchorB;
                        BlockPos hi2 = aL <= bL ? anchorB : anchorA;
                        PlacementSmoothingBatch batch = CoasterTrackPlacement.finalizePlacementSmoothingBatch(level, lo2, hi2, info.curve, info.anchorAnchorBuildResult);
                        info.curve = bc = batch.primary();
                        info.placementSmoothingNeighbors = batch.neighbors();
                        info.end1 = (Vec3)bc.starts.getFirst();
                        info.end2 = (Vec3)bc.starts.getSecond();
                        info.axis1 = (Vec3)bc.axes.getFirst();
                        info.axis2 = (Vec3)bc.axes.getSecond();
                        info.normal1 = (Vec3)bc.normals.getFirst();
                        info.normal2 = (Vec3)bc.normals.getSecond();
                        info.end1Extent = 0;
                        info.end2Extent = 0;
                        info.end2IsAnchor = true;
                    }
                    return info;
                }
                if (!(level instanceof ServerLevel)) {
                    return info;
                }
                sl = (ServerLevel)level;
                long aL = anchorA.asLong();
                long bL = anchorB.asLong();
                lo = aL <= bL ? anchorA : anchorB;
                hi = aL <= bL ? anchorB : anchorA;
                BlockEntity j = level.getBlockEntity(lo);
                if (!(j instanceof CoasterAnchorpointBlockEntity)) break block22;
                beLo = (CoasterAnchorpointBlockEntity)j;
                if (level.getBlockEntity(hi) instanceof CoasterAnchorpointBlockEntity) break block23;
            }
            return info;
        }
        CoasterTrackPlacementCommitDeferral.scheduleGeometryCommit(sl, lo, hi, () -> CoasterTrackPropagator.runBatchUpdate(sl, () -> {
            PlacementSmoothingBatch batch = CoasterTrackPlacement.finalizePlacementSmoothingBatch((Level)sl, lo, hi, info.curve, info.anchorAnchorBuildResult);
            beLo.putAnchorPeerCurve(sl, hi, batch.primary());
            CoasterTrackPlacement.putPlacementNeighborCurves(sl, batch.neighbors());
            info.anchorAnchorBuildResult = null;
        }));
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            CoasterTrackHotbarSelectionSync.syncMainHand(sp, InteractionHand.MAIN_HAND, null);
        }
        if (info.unlockLockedAnchorPeersAfterCommit) {
            info.unlockLockedAnchorPeersAfterCommit = false;
        }
        if (info.valid) {
            CoasterTrackPlacement.displayAnchorWrenchHintAfterPlacement(player, info.trackMaterial);
        }
        return info;
    }

    private static void displayAnchorWrenchHintAfterPlacement(Player player, TrackMaterial material) {
        if (player == null || !material.id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return;
        }
        player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.track.anchor_wrench_adjust_curve"), true);
    }

    @Nullable
    public static BezierConnection buildAnchorAnchorBezier(Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder) {
        return CoasterAnchorBezierOptimizer.buildAnchorAnchorBezier(level, anchorA, anchorB, material, girder);
    }

    private static PlacementSmoothingBatch finalizePlacementSmoothingBatch(Level level, BlockPos placementLo, BlockPos placementHi, BezierConnection primary, @Nullable CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult buildResult) {
        if (buildResult != null && buildResult.circularLoopNeighbors() != null) {
            ArrayList<BezierConnection> liftBatch = new ArrayList<BezierConnection>(buildResult.circularLoopNeighbors().size() + 1);
            liftBatch.add(primary);
            liftBatch.addAll(buildResult.circularLoopNeighbors());
            CoasterBezierHandleEdit.repinLiftedStartsOnBatchAnchors(level, liftBatch);
            return new PlacementSmoothingBatch((BezierConnection)liftBatch.get(0), List.copyOf(liftBatch.subList(1, liftBatch.size())));
        }
        BezierConnection syncedPrimary = CoasterTrackPlacement.syncPlacementCurveHubEndpoints(level, primary, placementLo, placementHi);
        List<BezierConnection> neighbors = List.of();
        if (buildResult != null && !buildResult.neighborJoinWrites().isEmpty()) {
            neighbors = CoasterTrackPlacement.buildPlacementNeighborCurves(level, buildResult.neighborJoinWrites(), placementLo, placementHi);
        }
        PlacementSmoothingBatch pinned = CoasterTrackPlacement.pinSharedPlacementJunctionFrames(level, placementLo, placementHi, syncedPrimary, neighbors);
        ArrayList<BezierConnection> liftBatch = new ArrayList<BezierConnection>(pinned.neighbors().size() + 1);
        liftBatch.add(pinned.primary());
        liftBatch.addAll(pinned.neighbors());
        CoasterBezierHandleEdit.repinLiftedStartsOnBatchAnchors(level, liftBatch);
        return new PlacementSmoothingBatch((BezierConnection)liftBatch.get(0), List.copyOf(liftBatch.subList(1, liftBatch.size())));
    }

    private static void putPlacementNeighborCurves(ServerLevel level, List<BezierConnection> neighbors) {
        if (neighbors.isEmpty()) {
            return;
        }
        HashSet<String> done = new HashSet<String>();
        for (BezierConnection refreshed : neighbors) {
            String k = CoasterBezierHandleEdit.edgeKey(refreshed);
            if (!done.add(k)) continue;
            CoasterTrackPlacement.commitExistingAnchorPeerCurve(level, refreshed);
        }
    }

    private static void commitExistingAnchorPeerCurve(ServerLevel level, BezierConnection primaryCurve) {
        long bL;
        BlockPos a = (BlockPos)primaryCurve.bePositions.getFirst();
        BlockPos b = (BlockPos)primaryCurve.bePositions.getSecond();
        for (BlockPos host : List.of(a, b)) {
            BlockPos peer;
            BlockEntity blockEntity = level.getBlockEntity(host);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity ape = (CoasterAnchorpointBlockEntity)blockEntity;
            BlockPos blockPos = peer = host.equals((Object)a) ? b : a;
            if (!ape.getAnchorPeerCurvesView().containsKey(peer)) continue;
            ape.putAnchorPeerCurve(level, peer, primaryCurve);
            return;
        }
        long aL = a.asLong();
        BlockPos lo = aL <= (bL = b.asLong()) ? a : b;
        BlockPos hi = aL <= bL ? b : a;
        BlockEntity blockEntity = level.getBlockEntity(lo);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity loBe = (CoasterAnchorpointBlockEntity)blockEntity;
            loBe.putAnchorPeerCurve(level, hi, primaryCurve);
        }
    }

    private static PlacementSmoothingBatch pinSharedPlacementJunctionFrames(Level level, BlockPos placementLo, BlockPos placementHi, BezierConnection primary, List<BezierConnection> neighbors) {
        ArrayList<BezierConnection> all = new ArrayList<BezierConnection>(neighbors.size() + 1);
        all.add(primary);
        all.addAll(neighbors);
        for (BlockPos hub : new BlockPos[]{placementLo, placementHi}) {
            BlockPos placementPeer = CoasterTrackPlacement.placementPeerAtHub(hub, placementLo, placementHi);
            Vec3 sharedStart = CoasterTrackPlacement.resolvePlacementStartAtHub(level, hub, placementPeer, CoasterAnchorpointBlockEntity.worldCenter(level, hub));
            for (int i = 0; i < all.size(); ++i) {
                Vec3 axis;
                BezierConnection c = (BezierConnection)all.get(i);
                if (!((BlockPos)c.bePositions.getFirst()).equals((Object)hub) && !((BlockPos)c.bePositions.getSecond()).equals((Object)hub)) continue;
                Vec3 vec3 = axis = hub.equals(c.bePositions.getFirst()) ? (Vec3)c.axes.getFirst() : (Vec3)c.axes.getSecond();
                if (axis.lengthSqr() < 1.0E-12) continue;
                axis = axis.normalize();
                Vec3 normal = CoasterTrackPlacement.resolvePlacementNormalAtHub(level, hub, placementPeer, axis);
                BezierConnection before = c;
                BezierConnection updated = CoasterTrackPlacement.withStartAndFrameAtEndpoint(level, c, hub, sharedStart, axis, normal);
                CoasterPeerCurveHandleLengths.carryLengthsToReplacement(before, updated);
                all.set(i, updated);
            }
        }
        return new PlacementSmoothingBatch((BezierConnection)all.get(0), List.copyOf(all.subList(1, all.size())));
    }

    private static List<BezierConnection> buildPlacementNeighborCurves(Level level, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite> writes, BlockPos placementLo, BlockPos placementHi) {
        if (writes.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite>> byCurve = CoasterTrackPlacement.neighborJoinWritesByCurve(writes);
        ArrayList<BezierConnection> out = new ArrayList<BezierConnection>(byCurve.size());
        for (List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite> curveWrites : byCurve.values()) {
            BezierConnection preview = CoasterTrackPlacement.curveFromNeighborJoinWrites(level, curveWrites, placementLo, placementHi);
            if (preview == null) continue;
            out.add(preview);
        }
        return out;
    }

    @Nullable
    private static BezierConnection refreshedCurveFromNeighborJoinWrite(Level level, CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite w, Set<String> seen) {
        boolean atShared;
        BezierConnection p;
        BlockPos shared = w.sharedAnchor();
        BlockPos remote = w.remotePos();
        Vec3 axis = w.axisIntoCurveAtSharedAnchor();
        if (axis.lengthSqr() < 1.0E-12) {
            return null;
        }
        if (!seen.add(shared.asLong() + ":" + remote.asLong())) {
            return null;
        }
        BezierConnection raw = null;
        BlockEntity blockEntity = level.getBlockEntity(shared);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity ape = (CoasterAnchorpointBlockEntity)blockEntity;
            raw = ape.getAnchorPeerCurvesView().get(remote);
        }
        if (raw == null) {
            return null;
        }
        BezierConnection bezierConnection = p = raw.isPrimary() ? raw : raw.secondary();
        if (!p.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return null;
        }
        boolean bl = atShared = ((BlockPos)p.bePositions.getFirst()).equals((Object)shared) || ((BlockPos)p.bePositions.getSecond()).equals((Object)shared);
        if (!atShared) {
            return null;
        }
        BezierConnection refreshed = CoasterTrackPlacement.curveWithAxisAtEndpointPreservingPeerForJoin(level, p, shared, axis);
        CoasterPeerCurveHandleLengths.copyEdgeLengths(p, refreshed);
        return refreshed;
    }

    private static String canonicalAnchorPairKey(BlockPos a, BlockPos b) {
        long bl;
        long al = a.asLong();
        return al <= (bl = b.asLong()) ? al + ":" + bl : bl + ":" + al;
    }

    private static LinkedHashMap<String, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite>> neighborJoinWritesByCurve(List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite> writes) {
        LinkedHashMap<String, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite>> byCurve = new LinkedHashMap<String, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite>>();
        HashSet<CallSite> seenWrite = new HashSet<CallSite>();
        for (CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite w : writes) {
            String writeKey;
            if (w.axisIntoCurveAtSharedAnchor().lengthSqr() < 1.0E-12 || !seenWrite.add((CallSite)((Object)(writeKey = w.sharedAnchor().asLong() + ":" + w.remotePos().asLong())))) continue;
            byCurve.computeIfAbsent(CoasterTrackPlacement.canonicalAnchorPairKey(w.sharedAnchor(), w.remotePos()), k -> new ArrayList()).add(w);
        }
        return byCurve;
    }

    @Nullable
    private static BezierConnection fetchCommittedNeighborCurve(Level level, BlockPos anchor, BlockPos peer) {
        BezierConnection p;
        BlockEntity blockEntity = level.getBlockEntity(anchor);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity ape = (CoasterAnchorpointBlockEntity)blockEntity;
        @Nullable BezierConnection raw = ape.getAnchorPeerCurvesView().get(peer);
        if (raw == null) {
            return null;
        }
        BezierConnection bezierConnection = p = raw.isPrimary() ? raw : raw.secondary();
        if (!p.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return null;
        }
        boolean atAnchor = ((BlockPos)p.bePositions.getFirst()).equals((Object)anchor) || ((BlockPos)p.bePositions.getSecond()).equals((Object)anchor);
        return atAnchor ? p : null;
    }

    @Nullable
    private static BezierConnection curveFromNeighborJoinWrites(Level level, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite> writes, BlockPos placementLo, BlockPos placementHi) {
        if (writes.isEmpty()) {
            return null;
        }
        BezierConnection current = CoasterTrackPlacement.fetchCommittedNeighborCurve(level, writes.get(0).sharedAnchor(), writes.get(0).remotePos());
        if (current == null) {
            return null;
        }
        for (CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite w : writes) {
            Vec3 axis = w.axisIntoCurveAtSharedAnchor();
            if (axis.lengthSqr() < 1.0E-12) continue;
            BlockPos shared = w.sharedAnchor();
            if (!((BlockPos)current.bePositions.getFirst()).equals((Object)shared) && !((BlockPos)current.bePositions.getSecond()).equals((Object)shared)) continue;
            BezierConnection before = current;
            current = CoasterTrackPlacement.applyPlacementNeighborJoinWrite(level, current, w, placementLo, placementHi);
            CoasterPeerCurveHandleLengths.copyEdgeLengths(before, current);
        }
        return current;
    }

    private static BezierConnection applyPlacementNeighborJoinWrite(Level level, BezierConnection bc, CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite write, BlockPos placementLo, BlockPos placementHi) {
        BlockPos shared = write.sharedAnchor();
        Vec3 axis = write.axisIntoCurveAtSharedAnchor();
        if (CoasterTrackPlacement.isPlacementSpanEndpoint(shared, placementLo, placementHi)) {
            BlockPos placementPeer = CoasterTrackPlacement.placementPeerAtHub(shared, placementLo, placementHi);
            return CoasterTrackPlacement.syncPlacementHubEndpointFrame(level, bc, shared, placementPeer, axis);
        }
        CoasterBezierEndpointData at = CoasterBezierEndpointData.at(bc, shared);
        if (at == null) {
            return bc;
        }
        Vec3 normal = CoasterTrackPlacement.anchorFaceNormalForAxis(level, shared, axis);
        return CoasterTrackPlacement.withStartAndFrameAtEndpoint(level, bc, shared, at.start(), axis, normal);
    }

    private static boolean isPlacementSpanEndpoint(BlockPos anchor, BlockPos placementLo, BlockPos placementHi) {
        return anchor.equals((Object)placementLo) || anchor.equals((Object)placementHi);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void drawNeighborSmoothPreviewCurves(Level level, List<BezierConnection> curves, int railColor) {
        CoasterTrackPlacement.clearNeighborCurvePreviewOutlines();
        if (curves.isEmpty()) {
            return;
        }
        double previewLift = CoasterTrackGauge.coasterPreviewSpineVerticalBumpBlocks();
        lastNeighborLineCounts = new int[curves.size()];
        for (int i = 0; i < curves.size(); ++i) {
            String key = COASTER_PLACEMENT_OUTLINE_NEIGHBOR_CURVE + i;
            CoasterTrackPlacement.lastNeighborLineCounts[i] = CoasterTrackPlacement.drawCurvePreview(level, curves.get(i), key, railColor, previewLift, 0);
        }
    }

    private static BlockPos placementPeerAtHub(BlockPos hub, BlockPos placementLo, BlockPos placementHi) {
        return hub.equals((Object)placementLo) ? placementHi : placementLo;
    }

    private static BezierConnection syncPlacementHubEndpointFrame(Level level, BezierConnection bc, BlockPos hub, BlockPos placementPeer, Vec3 axisIntoCurve) {
        if (!bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return bc;
        }
        if (!((BlockPos)bc.bePositions.getFirst()).equals((Object)hub) && !((BlockPos)bc.bePositions.getSecond()).equals((Object)hub)) {
            return bc;
        }
        Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-12 ? axisIntoCurve : axisIntoCurve.normalize();
        Vec3 start = CoasterTrackPlacement.resolvePlacementStartForSmoothedLegAtHub(level, bc, hub, placementPeer);
        Vec3 normal = CoasterTrackPlacement.resolvePlacementNormalForSmoothedLegAtHub(level, bc, hub, placementPeer, axis);
        return CoasterTrackPlacement.withStartAndFrameAtEndpoint(level, bc, hub, start, axis, normal);
    }

    private static Vec3 resolvePlacementStartForSmoothedLegAtHub(Level level, BezierConnection bc, BlockPos hub, BlockPos placementPeer) {
        CoasterBezierEndpointData at = CoasterBezierEndpointData.at(bc, hub);
        if (at != null) {
            return at.start();
        }
        return CoasterTrackPlacement.resolvePlacementStartAtHub(level, hub, placementPeer, CoasterAnchorpointBlockEntity.worldCenter(level, hub));
    }

    private static Vec3 resolvePlacementNormalForSmoothedLegAtHub(Level level, BezierConnection bc, BlockPos hub, BlockPos placementPeer, Vec3 axisIntoCurve) {
        CoasterBezierEndpointData at = CoasterBezierEndpointData.at(bc, hub);
        if (at != null && at.normal().lengthSqr() > 1.0E-12) {
            Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-12 ? axisIntoCurve : axisIntoCurve.normalize();
            Vec3 ref = at.normal().normalize();
            Vec3 inPlane = ref.subtract(axis.scale(ref.dot(axis)));
            if (inPlane.lengthSqr() > 1.0E-12) {
                return inPlane.normalize();
            }
        }
        return CoasterTrackPlacement.resolvePlacementNormalAtHub(level, hub, placementPeer, axisIntoCurve);
    }

    private static BezierConnection syncPlacementCurveHubEndpoints(Level level, BezierConnection bc, BlockPos placementLo, BlockPos placementHi) {
        if (!bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return bc;
        }
        Vec3 axisLo = (Vec3)bc.axes.getFirst();
        Vec3 axisHi = (Vec3)bc.axes.getSecond();
        bc = CoasterTrackPlacement.syncPlacementHubEndpointFrame(level, bc, placementLo, placementHi, axisLo);
        bc = CoasterTrackPlacement.syncPlacementHubEndpointFrame(level, bc, placementHi, placementLo, axisHi);
        return bc;
    }

    private static BezierConnection withAxisAtEndpoint(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newAxis, boolean preservePeerEndpoint) {
        if (bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id) && newAxis.lengthSqr() > 1.0E-12) {
            newAxis = newAxis.normalize();
        }
        if (preservePeerEndpoint && bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            Vec3 s1 = (Vec3)bc.starts.getFirst();
            Vec3 s2 = (Vec3)bc.starts.getSecond();
            Vec3 a1 = (Vec3)bc.axes.getFirst();
            Vec3 a2 = (Vec3)bc.axes.getSecond();
            Vec3 n1 = (Vec3)bc.normals.getFirst();
            Vec3 n2 = (Vec3)bc.normals.getSecond();
            if (((BlockPos)bc.bePositions.getFirst()).equals((Object)endpoint)) {
                a1 = newAxis;
                n1 = CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, endpoint, n1, newAxis);
            }
            if (((BlockPos)bc.bePositions.getSecond()).equals((Object)endpoint)) {
                a2 = newAxis;
                n2 = CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, endpoint, n2, newAxis);
            }
            return new BezierConnection(Couple.create((Object)((BlockPos)bc.bePositions.getFirst()), (Object)((BlockPos)bc.bePositions.getSecond())), Couple.create((Object)s1, (Object)s2), Couple.create((Object)a1, (Object)a2), Couple.create((Object)n1, (Object)n2), bc.isPrimary(), bc.hasGirder, bc.getMaterial());
        }
        Vec3 s1 = (Vec3)bc.starts.getFirst();
        Vec3 s2 = (Vec3)bc.starts.getSecond();
        Vec3 a1 = (Vec3)bc.axes.getFirst();
        Vec3 a2 = (Vec3)bc.axes.getSecond();
        Vec3 n1 = (Vec3)bc.normals.getFirst();
        Vec3 n2 = (Vec3)bc.normals.getSecond();
        if (((BlockPos)bc.bePositions.getFirst()).equals((Object)endpoint)) {
            a1 = newAxis;
        }
        if (((BlockPos)bc.bePositions.getSecond()).equals((Object)endpoint)) {
            a2 = newAxis;
        }
        if (((BlockPos)bc.bePositions.getFirst()).equals((Object)endpoint) && level.getBlockState(endpoint).getBlock() instanceof CoasterAnchorpointBlock) {
            s1 = CoasterAnchorpointBlockEntity.worldCenter(level, endpoint);
            n1 = CoasterTrackPlacement.anchorEndpointNormal(level, endpoint, newAxis);
        } else if (((BlockPos)bc.bePositions.getSecond()).equals((Object)endpoint) && level.getBlockState(endpoint).getBlock() instanceof CoasterAnchorpointBlock) {
            s2 = CoasterAnchorpointBlockEntity.worldCenter(level, endpoint);
            n2 = CoasterTrackPlacement.anchorEndpointNormal(level, endpoint, newAxis);
        }
        return new BezierConnection(Couple.create((Object)((BlockPos)bc.bePositions.getFirst()), (Object)((BlockPos)bc.bePositions.getSecond())), Couple.create((Object)s1, (Object)s2), Couple.create((Object)a1, (Object)a2), Couple.create((Object)n1, (Object)n2), bc.isPrimary(), bc.hasGirder, bc.getMaterial());
    }

    public static Vec3 anchorGeometricFaceNormal(Level level, BlockPos anchorPos, Vec3 axisIntoCurve) {
        return CoasterTrackPlacement.anchorEndpointNormalGeometric(level, anchorPos, axisIntoCurve);
    }

    public static Vec3 anchorFaceNormalForAxis(Level level, BlockPos anchorPos, Vec3 axisIntoCurve) {
        return CoasterTrackPlacement.anchorEndpointNormal(level, anchorPos, axisIntoCurve);
    }

    public static Vec3 resolvePlacementStartAtHub(Level level, BlockPos hub, BlockPos newPeer, Vec3 fallbackWorldCenter) {
        CoasterBezierEndpointData incident = CoasterTrackPlacement.incidentHubEndpointFromExistingLeg(level, hub, newPeer);
        if (incident != null) {
            return CoasterBezierHandleEdit.liftedWorldCenterFromCanonicalTangent(level, hub, incident.axis());
        }
        return fallbackWorldCenter;
    }

    public static Vec3 resolvePlacementNormalAtHub(Level level, BlockPos hub, BlockPos newPeer, Vec3 axisIntoNewLeg) {
        CoasterBezierEndpointData incident = CoasterTrackPlacement.incidentHubEndpointFromExistingLeg(level, hub, newPeer);
        if (incident != null && incident.normal().lengthSqr() > 1.0E-12) {
            Vec3 axis = axisIntoNewLeg.lengthSqr() < 1.0E-12 ? axisIntoNewLeg : axisIntoNewLeg.normalize();
            Vec3 ref = incident.normal().normalize();
            Vec3 inPlane = ref.subtract(axis.scale(ref.dot(axis)));
            if (inPlane.lengthSqr() > 1.0E-12) {
                return inPlane.normalize();
            }
            Vec3 fallback = CoasterTrackPlacement.anchorFaceNormalForAxis(level, hub, axis);
            if (fallback.lengthSqr() > 1.0E-12 && fallback.normalize().dot(ref) < 0.0) {
                return fallback.scale(-1.0);
            }
            return fallback;
        }
        return CoasterTrackPlacement.anchorFaceNormalForAxis(level, hub, axisIntoNewLeg);
    }

    @Nullable
    public static Vec3 storedHubStartFromIncidentCurve(Level level, BlockPos hub, BlockPos excludePeer) {
        CoasterBezierEndpointData at = CoasterTrackPlacement.incidentHubEndpointFromExistingLeg(level, hub, excludePeer);
        return at == null ? null : at.start();
    }

    @Nullable
    static CoasterBezierEndpointData incidentHubEndpointFromExistingLeg(Level level, BlockPos hub, BlockPos excludePeer) {
        BlockEntity blockEntity = level.getBlockEntity(hub);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        BlockPos bestPeer = null;
        BezierConnection best = null;
        for (Map.Entry<BlockPos, BezierConnection> e : be.getAnchorPeerCurvesView().entrySet()) {
            BezierConnection c;
            BlockPos peer = e.getKey();
            if (peer.equals((Object)excludePeer) || (c = e.getValue()) == null || bestPeer != null && peer.asLong() >= bestPeer.asLong()) continue;
            bestPeer = peer.immutable();
            best = c;
        }
        if (best == null) {
            return null;
        }
        BezierConnection p = best.isPrimary() ? best : best.secondary();
        return CoasterBezierEndpointData.at(p, hub);
    }

    public static Vec3 anchorMountingPlaneReference(Level level, BlockPos anchorPos) {
        BlockState state = level.getBlockState(anchorPos);
        Direction facing = state.hasProperty((Property)CoasterAnchorpointBlock.FACING) ? (Direction)state.getValue((Property)CoasterAnchorpointBlock.FACING) : Direction.UP;
        Direction refDir = facing.getAxis() == Direction.Axis.Y ? facing.getClockWise(Direction.Axis.Y) : Direction.UP;
        Vec3 localUp = CoasterAnchorpointBlockEntity.localUp(level, anchorPos);
        Vec3 ref = Vec3.atLowerCornerOf((Vec3i)refDir.getNormal());
        if ((ref = ref.subtract(localUp.scale(localUp.dot(ref)))).lengthSqr() < 1.0E-10) {
            refDir = facing.getAxis() == Direction.Axis.Y ? facing.getCounterClockWise(Direction.Axis.Y) : facing.getClockWise(Direction.Axis.Y);
            ref = Vec3.atLowerCornerOf((Vec3i)refDir.getNormal());
            ref = ref.subtract(localUp.scale(localUp.dot(ref)));
        }
        return ref.lengthSqr() < 1.0E-10 ? new Vec3(1.0, 0.0, 0.0) : ref.normalize();
    }

    public static Vec3 anchorFacingPerpendicularToAxis(Level level, BlockPos anchorPos, Vec3 axisUnit) {
        Vec3 ref = CoasterTrackPlacement.anchorMountingPlaneReference(level, anchorPos);
        Vec3 inPlane = ref.subtract(axisUnit.scale(axisUnit.dot(ref)));
        if (inPlane.lengthSqr() < 1.0E-10) {
            Vec3 localUp = CoasterAnchorpointBlockEntity.localUp(level, anchorPos);
            inPlane = localUp.cross(axisUnit);
        }
        if (inPlane.lengthSqr() < 1.0E-10) {
            Vec3 x = new Vec3(1.0, 0.0, 0.0);
            inPlane = x.subtract(axisUnit.scale(axisUnit.dot(x)));
        }
        return inPlane.normalize();
    }

    private static Vec3 anchorEndpointNormalGeometric(Level level, BlockPos anchorPos, Vec3 axisIntoCurve) {
        Vec3 side;
        Vec3 localUp = CoasterAnchorpointBlockEntity.localUp(level, anchorPos);
        Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axisIntoCurve.normalize();
        Vec3 n = localUp.subtract(axis.scale(axis.dot(localUp)));
        if (n.lengthSqr() < 1.0E-10 && (n = axis.cross(side = CoasterTrackPlacement.anchorFacingPerpendicularToAxis(level, anchorPos, axis))).lengthSqr() < 1.0E-10) {
            n = side.cross(axis);
        }
        return n.normalize();
    }

    private static Vec3 anchorEndpointNormal(Level level, BlockPos anchorPos, Vec3 axisIntoCurve) {
        Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axisIntoCurve.normalize();
        Vec3 n0 = CoasterTrackPlacement.anchorEndpointNormalGeometric(level, anchorPos, axis);
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return CoasterTrackPlacement.orthonormalToAxis(n0, axis);
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        return CoasterBezierHandleEdit.jointBankFaceNormalAfterGeometric(level, anchorPos, axis, n0, be.getJointBankRadians());
    }

    public static BezierConnection curveWithReferenceNormalAtAnchorEndpoint(Level level, BezierConnection bc, BlockPos anchorPos, Vec3 referenceNormal) {
        Vec3 axis = ((BlockPos)bc.bePositions.getFirst()).equals((Object)anchorPos) ? (Vec3)bc.axes.getFirst() : (Vec3)bc.axes.getSecond();
        return CoasterTrackPlacement.withFrameAtEndpoint(level, bc, anchorPos, axis, referenceNormal);
    }

    private static BezierConnection withFrameAtEndpoint(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newAxis, Vec3 newNormal) {
        Vec3 a1 = (Vec3)bc.axes.getFirst();
        Vec3 a2 = (Vec3)bc.axes.getSecond();
        Vec3 n1 = (Vec3)bc.normals.getFirst();
        Vec3 n2 = (Vec3)bc.normals.getSecond();
        if (((BlockPos)bc.bePositions.getFirst()).equals((Object)endpoint)) {
            a1 = newAxis;
            n1 = CoasterTrackPlacement.orthonormalToAxis(newNormal, newAxis, level, endpoint);
        }
        if (((BlockPos)bc.bePositions.getSecond()).equals((Object)endpoint)) {
            a2 = newAxis;
            n2 = CoasterTrackPlacement.orthonormalToAxis(newNormal, newAxis, level, endpoint);
        }
        return new BezierConnection(Couple.create((Object)((BlockPos)bc.bePositions.getFirst()), (Object)((BlockPos)bc.bePositions.getSecond())), Couple.create((Object)((Vec3)bc.starts.getFirst()), (Object)((Vec3)bc.starts.getSecond())), Couple.create((Object)a1, (Object)a2), Couple.create((Object)n1, (Object)n2), bc.isPrimary(), bc.hasGirder, bc.getMaterial());
    }

    private static BezierConnection withStartAndFrameAtEndpoint(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newStart, Vec3 newAxis, Vec3 newNormal) {
        Vec3 s1 = (Vec3)bc.starts.getFirst();
        Vec3 s2 = (Vec3)bc.starts.getSecond();
        Vec3 a1 = (Vec3)bc.axes.getFirst();
        Vec3 a2 = (Vec3)bc.axes.getSecond();
        Vec3 n1 = (Vec3)bc.normals.getFirst();
        Vec3 n2 = (Vec3)bc.normals.getSecond();
        if (((BlockPos)bc.bePositions.getFirst()).equals((Object)endpoint)) {
            s1 = newStart;
            a1 = newAxis;
            n1 = CoasterTrackPlacement.orthonormalToAxis(newNormal, newAxis, level, endpoint);
        }
        if (((BlockPos)bc.bePositions.getSecond()).equals((Object)endpoint)) {
            s2 = newStart;
            a2 = newAxis;
            n2 = CoasterTrackPlacement.orthonormalToAxis(newNormal, newAxis, level, endpoint);
        }
        return new BezierConnection(Couple.create((Object)((BlockPos)bc.bePositions.getFirst()), (Object)((BlockPos)bc.bePositions.getSecond())), Couple.create((Object)s1, (Object)s2), Couple.create((Object)a1, (Object)a2), Couple.create((Object)n1, (Object)n2), bc.isPrimary(), bc.hasGirder, bc.getMaterial());
    }

    private static Vec3 orthonormalToAxis(Vec3 normal, Vec3 axis) {
        return CoasterTrackPlacement.orthonormalToAxis(normal, axis, null, null);
    }

    private static Vec3 orthonormalToAxis(Vec3 normal, Vec3 axis, @Nullable Level level, @Nullable BlockPos anchorPos) {
        Vec3 a = axis.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axis.normalize();
        Vec3 n = normal.subtract(a.scale(a.dot(normal)));
        if (n.lengthSqr() < 1.0E-10) {
            if (level != null && anchorPos != null && level.getBlockState(anchorPos).getBlock() instanceof CoasterAnchorpointBlock) {
                n = CoasterTrackPlacement.anchorEndpointNormalGeometric(level, anchorPos, a);
            } else {
                Vec3 up = new Vec3(0.0, 1.0, 0.0);
                n = up.subtract(a.scale(a.dot(up)));
                if (n.lengthSqr() < 1.0E-10) {
                    Vec3 x = new Vec3(1.0, 0.0, 0.0);
                    n = x.subtract(a.scale(a.dot(x)));
                }
            }
        }
        return n.normalize();
    }

    public static Vec3 orthonormalFaceNormalForAxis(Vec3 referenceNormal, Vec3 axisIntoCurve) {
        return CoasterTrackPlacement.orthonormalToAxis(referenceNormal, axisIntoCurve);
    }

    public static Vec3 orthonormalFaceNormalForAxis(Level level, BlockPos anchorPos, Vec3 referenceNormal, Vec3 axisIntoCurve) {
        return CoasterTrackPlacement.orthonormalToAxis(referenceNormal, axisIntoCurve, level, anchorPos);
    }

    public static Component placementErrorMessage(String translationKey) {
        return Component.translatable((String)translationKey).withStyle(ChatFormatting.RED);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void clearNeighborCurvePreviewOutlines() {
        for (int n = 0; n < lastNeighborLineCounts.length; ++n) {
            CoasterTrackPlacement.clearCurvePreviewOutlinesForKey(COASTER_PLACEMENT_OUTLINE_NEIGHBOR_CURVE + n, lastNeighborLineCounts[n]);
        }
        lastNeighborLineCounts = new int[0];
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void clearCoasterTrackPlacementPreviewOutlines() {
        Outliner o = Outliner.getInstance();
        for (int id = 1; id <= 4; ++id) {
            o.remove((Object)Pair.of((Object)COASTER_PLACEMENT_OUTLINE_START, (Object)id));
        }
        for (int k = 2; k <= 4096; ++k) {
            o.remove((Object)Pair.of((Object)COASTER_PLACEMENT_OUTLINE_CURVE, (Object)k));
        }
        lastLineCount = 0;
        CoasterTrackPlacement.clearNeighborCurvePreviewOutlines();
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void drawAnchorSelectionOutlines(Minecraft mc, ItemStack stack, int outlineColor, @Nullable BlockPos hoverOverride) {
        HitResult hit;
        BlockPos first = CoasterTrackPlacement.readAnchorFirstSelection(stack);
        if (first == null) {
            Outliner.getInstance().remove((Object)COASTER_PLACEMENT_OUTLINE_ANCHOR_FIRST);
            Outliner.getInstance().remove((Object)COASTER_PLACEMENT_OUTLINE_ANCHOR_HOVER);
            return;
        }
        ClientLevel level = mc.level;
        CoasterTrackPlacement.showAnchorBlockOutline((Level)level, first, COASTER_PLACEMENT_OUTLINE_ANCHOR_FIRST, outlineColor);
        BlockPos hover = hoverOverride;
        if (hover == null && (hit = mc.hitResult) instanceof BlockHitResult) {
            BlockHitResult bhr = (BlockHitResult)hit;
            if (hit.getType() == HitResult.Type.BLOCK) {
                hover = bhr.getBlockPos();
            }
        }
        if (hover != null && !first.equals((Object)hover) && level.getBlockState(hover).getBlock() instanceof CoasterAnchorpointBlock) {
            CoasterTrackPlacement.showAnchorBlockOutline((Level)level, hover, COASTER_PLACEMENT_OUTLINE_ANCHOR_HOVER, outlineColor);
        } else {
            Outliner.getInstance().remove((Object)COASTER_PLACEMENT_OUTLINE_ANCHOR_HOVER);
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    @Nullable
    private static BlockPos resolvePlacementLookBlock(Minecraft mc, ItemStack stack) {
        HitResult hitResult = mc.hitResult;
        if (hitResult instanceof BlockHitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult)hitResult).getBlockPos();
        }
        if (CoasterTrackPlacement.hasAnchorFirstSelection(stack)) {
            return CoasterTrackOpenEndpointOutlineClient.resolveClosestOpenAnchor(mc);
        }
        return null;
    }

    private static void displayPlacementActionBar(Player player, PlacementInfo info) {
        if (info.message == null) {
            return;
        }
        if (info.valid) {
            player.displayClientMessage((Component)CreateLang.translateDirect((String)"track.valid_connection", (Object[])new Object[0]).withStyle(ChatFormatting.WHITE), true);
        } else {
            MutableComponent line = info.modTranslationKey ? Component.translatable((String)info.message) : CreateLang.translateDirect((String)info.message, (Object[])new Object[0]);
            player.displayClientMessage((Component)line.withStyle(info.message.equals("track.second_point") ? ChatFormatting.WHITE : ChatFormatting.RED), true);
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void showAnchorBlockOutline(Level level, BlockPos pos, String outlinerKey, int color) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CoasterAnchorpointBlock)) {
            Outliner.getInstance().remove((Object)outlinerKey);
            return;
        }
        VoxelShape shape = state.getShape((BlockGetter)level, pos);
        if (shape.isEmpty()) {
            Outliner.getInstance().remove((Object)outlinerKey);
            return;
        }
        Outliner.getInstance().showAABB((Object)outlinerKey, shape.bounds().move(pos)).colored(color).lineWidth(0.0625f);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void showTargetBlockOutline(Level level, BlockPos pos, String outlinerKey, int color) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getShape((BlockGetter)level, pos);
        AABB box = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
        Outliner.getInstance().showAABB((Object)outlinerKey, box).colored(color).lineWidth(0.0625f);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static boolean drawInvalidHoverPreview(Minecraft mc, LocalPlayer player, ItemStack stack, BlockPos lookBlock, int restoreWarmup) {
        if (!CoasterTrackPlacement.hasAnchorFirstSelection(stack)) {
            return false;
        }
        if (lookBlock == null) {
            return false;
        }
        Level level = player.level();
        if (level.getBlockState(lookBlock).getBlock() instanceof CoasterAnchorpointBlock) {
            return false;
        }
        BlockPos anchorFirst = CoasterTrackPlacement.readAnchorFirstSelection(stack);
        if (anchorFirst == null || anchorFirst.equals((Object)lookBlock)) {
            return false;
        }
        BlockPos virtualSecond = lookBlock.above();
        if (virtualSecond.equals((Object)anchorFirst)) {
            return false;
        }
        CoasterTrackPlacement.updateTrackCostOverlayForPreviewEndpoints(mc, level, (Player)player, stack, anchorFirst, virtualSecond);
        CoasterTrackPlacement.clearCoasterTrackPlacementPreviewOutlines();
        Outliner.getInstance().remove((Object)COASTER_PLACEMENT_OUTLINE_ANCHOR_HOVER);
        CoasterTrackPlacement.showTargetBlockOutline(level, lookBlock, COASTER_PLACEMENT_OUTLINE_INVALID_TARGET, 15359019);
        extraTipWarmup = restoreWarmup;
        extraTipWarmup = 0;
        player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.track.must_attach_to_anchors").withStyle(ChatFormatting.RED), true);
        if (CoasterTrackPlacement.shouldSkipAnchorPreviewDraw(level, anchorFirst, virtualSecond)) {
            return true;
        }
        TrackMaterial material = TrackMaterial.fromItem((Item)stack.getItem());
        CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult placement = CoasterAnchorBezierOptimizer.buildAnchorToVirtualPeerPlacement(level, anchorFirst, virtualSecond, material, false, true);
        if (placement == null) {
            return true;
        }
        BezierConnection bc = placement.primary();
        if (CoasterTrackPlacement.shouldSkipHeavyCurvePreview(bc)) {
            return true;
        }
        PlacementInfo info = new PlacementInfo(material);
        info.curve = bc;
        info.pos1 = (BlockPos)bc.bePositions.getFirst();
        info.pos2 = (BlockPos)bc.bePositions.getSecond();
        info.valid = false;
        info.end1 = (Vec3)bc.starts.getFirst();
        info.end2 = (Vec3)bc.starts.getSecond();
        info.axis1 = (Vec3)bc.axes.getFirst();
        info.axis2 = (Vec3)bc.axes.getSecond();
        info.normal1 = (Vec3)bc.normals.getFirst();
        info.normal2 = (Vec3)bc.normals.getSecond();
        info.end1Extent = 0;
        info.end2Extent = 0;
        animation.chase(0.0, 0.25, LerpedFloat.Chaser.EXP);
        animation.tickChaser();
        CoasterTrackPlacement.drawClientPlacementPreview(level, info, 15359019);
        return true;
    }

    @OnlyIn(value=Dist.CLIENT)
    private static boolean shouldSkipAnchorPreviewDraw(Level level, BlockPos anchorA, BlockPos anchorB) {
        if (CoasterTrackPlacement.anchorConnectionTooFar(level, anchorA, anchorB)) {
            return true;
        }
        return !CoasterAnchorClientSpace.anchorHostsWithinPreviewSpan(level, anchorA, anchorB);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static boolean shouldSkipHeavyCurvePreview(@Nullable BezierConnection bc) {
        if (bc == null) {
            return true;
        }
        double length = bc.getLength();
        if (!Double.isFinite(length) || length <= 0.0) {
            return true;
        }
        double maxLen = (double)CoasterTrackGauge.maxCoasterCurveEndpointSpanBlocks() * 1.5;
        if (length > maxLen) {
            return true;
        }
        return !AnchorPeerCurveOutlineSupport.hasSaneOutlineBounds(bc);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void drawClientPlacementPreview(Level level, PlacementInfo info, int railColor) {
        if (info.end1 == null || info.axis1 == null || info.end2 == null || info.axis2 == null || info.normal1 == null || info.normal2 == null) {
            return;
        }
        if (info.pos1 != null && info.pos2 != null && CoasterTrackPlacement.shouldSkipAnchorPreviewDraw(level, info.pos1, info.pos2)) {
            CoasterTrackPlacement.clearCurvePreviewOutlines(lastLineCount);
            lastLineCount = 0;
            return;
        }
        if (CoasterTrackPlacement.shouldSkipHeavyCurvePreview(info.curve)) {
            CoasterTrackPlacement.clearCurvePreviewOutlines(lastLineCount);
            lastLineCount = 0;
            return;
        }
        BezierConnection bc = info.curve;
        double previewLift = CoasterTrackGauge.coasterPreviewSpineVerticalBumpBlocks();
        double previewLiftAlongFaceUp = CoasterTrackGauge.coasterPreviewCurveLiftAlongFaceUp();
        double previewLiftWorldY = CoasterTrackGauge.coasterPreviewRailWorldYOffsetBlocks() + CoasterTrackPlacement.previewExtraWorldYForAnchors(bc, info.pos1, info.pos2);
        float previewHalf = CoasterTrackGauge.coasterPreviewRailHalfGauge();
        Vec3 previewEnd1 = info.end1;
        Vec3 previewEnd2 = info.end2;
        Vec3 previewAxis1 = info.axis1;
        Vec3 previewAxis2 = info.axis2;
        Vec3 previewNormal1 = info.normal1;
        Vec3 previewNormal2 = info.normal2;
        if (bc != null) {
            CoasterBezierEndpointData at1 = CoasterBezierEndpointData.at(bc, info.pos1);
            CoasterBezierEndpointData at2 = CoasterBezierEndpointData.at(bc, info.pos2);
            if (at1 != null) {
                previewEnd1 = at1.start();
                previewAxis1 = at1.axis();
                previewNormal1 = at1.normal();
            }
            if (at2 != null) {
                previewEnd2 = at2.start();
                previewAxis2 = at2.axis();
                previewNormal2 = at2.normal();
            }
        }
        Vec3 v1 = previewEnd1;
        Vec3 a1 = previewAxis1.normalize();
        Vec3 n1 = BezierRailSpread.faceCrossTangentOrFallback(previewNormal1, a1).normalize().scale((double)previewHalf);
        Vec3 lift1 = (previewNormal1.lengthSqr() < 1.0E-10 ? Vec3.ZERO : previewNormal1.normalize().scale(previewLiftAlongFaceUp)).add(0.0, previewLiftWorldY, 0.0);
        Vec3 o1 = a1.scale(0.125);
        Vec3 ex1 = a1.scale((double)(info.end1Extent - (bc == null && info.end1Extent > 0 ? 2 : 0)) * previewAxis1.length());
        CoasterTrackPlacement.line(1, v1.add(n1).add(lift1), o1, ex1, railColor);
        CoasterTrackPlacement.line(2, v1.subtract(n1).add(lift1), o1, ex1, railColor);
        Vec3 v2 = previewEnd2;
        Vec3 a2 = previewAxis2.normalize();
        Vec3 n2 = BezierRailSpread.faceCrossTangentOrFallback(previewNormal2, a2).normalize().scale((double)previewHalf);
        Vec3 lift2 = (previewNormal2.lengthSqr() < 1.0E-10 ? Vec3.ZERO : previewNormal2.normalize().scale(previewLiftAlongFaceUp)).add(0.0, previewLiftWorldY, 0.0);
        Vec3 o2 = a2.scale(0.125);
        Vec3 ex2 = a2.scale((double)info.end2Extent * previewAxis2.length());
        CoasterTrackPlacement.line(3, v2.add(n2).add(lift2), o2, ex2, railColor);
        CoasterTrackPlacement.line(4, v2.subtract(n2).add(lift2), o2, ex2, railColor);
        if (bc != null) {
            lastLineCount = CoasterTrackPlacement.drawCurvePreview(level, bc, COASTER_PLACEMENT_OUTLINE_CURVE, railColor, previewLift, lastLineCount);
        }
        if (info.placementSmoothingNeighbors != null && !info.placementSmoothingNeighbors.isEmpty()) {
            CoasterTrackPlacement.drawNeighborSmoothPreviewCurves(level, info.placementSmoothingNeighbors, railColor);
        } else {
            CoasterTrackPlacement.clearNeighborCurvePreviewOutlines();
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    @Nullable
    private static ItemStack findCoasterTrackStack(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof CoasterTrackItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof CoasterTrackItem) {
            return off;
        }
        return null;
    }

    @OnlyIn(value=Dist.CLIENT)
    public static void clientTick() {
        CoasterTrackPlacementSmoothHud.setHintActive(false);
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            CoasterTrackPlacementOverlayRenderer.clear();
            CoasterTrackPlacement.clearCoasterTrackPlacementPreviewOutlines();
            return;
        }
        ItemStack coasterStack = CoasterTrackPlacement.findCoasterTrackStack(player);
        int anchorOutlineColor = 1940687;
        ItemStack stack = CoasterTrackPlacement.findCoasterTrackStack(player);
        if (stack == null) {
            CoasterTrackPlacementOverlayRenderer.clear();
            CoasterTrackPlacement.clearCoasterTrackPlacementPreviewOutlines();
            return;
        }
        if (!stack.hasFoil() && !CoasterTrackPlacement.hasAnchorFirstSelection(stack)) {
            CoasterTrackPlacementOverlayRenderer.clear();
            CoasterTrackPlacement.clearCoasterTrackPlacementPreviewOutlines();
            Outliner.getInstance().remove((Object)COASTER_PLACEMENT_OUTLINE_INVALID_TARGET);
            return;
        }
        HitResult hitResult = mc.hitResult;
        int restoreWarmup = extraTipWarmup;
        extraTipWarmup = 0;
        BlockPos lookBlock = CoasterTrackPlacement.resolvePlacementLookBlock(mc, stack);
        if (lookBlock == null) {
            CoasterTrackPlacementOverlayRenderer.clear();
            CoasterTrackPlacement.clearCoasterTrackPlacementPreviewOutlines();
            if (coasterStack != null && CoasterTrackPlacement.hasAnchorFirstSelection(coasterStack)) {
                CoasterTrackPlacement.drawAnchorSelectionOutlines(mc, coasterStack, anchorOutlineColor, null);
            }
            return;
        }
        try {
            boolean skipCurvePreview;
            if (hitResult instanceof BlockHitResult && hitResult.getType() == HitResult.Type.BLOCK && CoasterTrackPlacement.drawInvalidHoverPreview(mc, player, stack, lookBlock, restoreWarmup)) {
                if (coasterStack != null && CoasterTrackPlacement.hasAnchorFirstSelection(coasterStack)) {
                    CoasterTrackPlacement.drawAnchorSelectionOutlines(mc, coasterStack, anchorOutlineColor, lookBlock);
                }
                return;
            }
            Outliner.getInstance().remove((Object)COASTER_PLACEMENT_OUTLINE_INVALID_TARGET);
            Level level = player.level();
            BlockPos pos = lookBlock;
            BlockState hitState = level.getBlockState(pos);
            CoasterTrackPlacement.clearCoasterTrackPlacementPreviewOutlines();
            extraTipWarmup = restoreWarmup;
            boolean maxTurns = true;
            PlacementInfo info = CoasterTrackPlacement.tryConnect(level, (Player)player, pos, hitState, stack, false, maxTurns, true);
            CoasterTrackPlacement.applyCoasterAnchorsOnlyPreviewInvalidMarks(level, info, pos, hitState, stack);
            BlockPos anchorFirst = CoasterTrackPlacement.readAnchorFirstSelection(stack);
            if (anchorFirst != null && !anchorFirst.equals((Object)pos) && hitState.getBlock() instanceof CoasterAnchorpointBlock && !info.valid) {
                anchorOutlineColor = 15359019;
            }
            if (extraTipWarmup < 20) {
                ++extraTipWarmup;
            }
            if (!info.valid || !hoveringMaxed && (info.end1Extent == 0 || info.end2Extent == 0)) {
                extraTipWarmup = 0;
            }
            CoasterTrackPlacementOverlayRenderer.updateFromPlacementPreview(mc, info.requiredTracks, info.hasRequiredTracks, info.valid);
            if (info.valid) {
                player.displayClientMessage((Component)CreateLang.translateDirect((String)"track.valid_connection", (Object[])new Object[0]).withStyle(ChatFormatting.WHITE), true);
                if (anchorFirst != null && hitState.getBlock() instanceof CoasterAnchorpointBlock && !anchorFirst.equals((Object)pos) && CoasterTrackPlacement.placementHasNeighborsToCoSmooth(level, anchorFirst, pos)) {
                    CoasterTrackPlacementSmoothHud.setHintActive(true);
                }
            } else if (info.message != null) {
                MutableComponent line = info.modTranslationKey ? Component.translatable((String)info.message) : CreateLang.translateDirect((String)info.message, (Object[])new Object[0]);
                player.displayClientMessage((Component)line.withStyle(info.message.equals("track.second_point") ? ChatFormatting.WHITE : ChatFormatting.RED), true);
            }
            boolean bl = skipCurvePreview = anchorFirst != null && hitState.getBlock() instanceof CoasterAnchorpointBlock && !anchorFirst.equals((Object)pos) && CoasterTrackPlacement.shouldSkipAnchorPreviewDraw(level, anchorFirst, pos);
            if (!skipCurvePreview && info.end1 != null && info.axis1 != null && info.end2 != null && info.axis2 != null && info.normal1 != null && info.normal2 != null) {
                animation.chase(info.valid ? 1.0 : 0.0, 0.25, LerpedFloat.Chaser.EXP);
                animation.tickChaser();
                if (!info.valid) {
                    info.end1Extent = 0;
                    info.end2Extent = 0;
                }
                int color = Color.mixColors((int)15359019, (int)1940687, (float)animation.getValue());
                CoasterTrackPlacement.drawClientPlacementPreview(level, info, color);
            }
        }
        catch (RuntimeException ex) {
            CoasterTrackPlacementOverlayRenderer.clear();
            CoasterTrackPlacement.clearCoasterTrackPlacementPreviewOutlines();
            SimulatedCoasters.LOGGER.warn("Coaster track placement preview failed (skipped this tick)", (Throwable)ex);
        }
        if (coasterStack != null && CoasterTrackPlacement.hasAnchorFirstSelection(coasterStack)) {
            CoasterTrackPlacement.drawAnchorSelectionOutlines(mc, coasterStack, anchorOutlineColor, lookBlock);
        } else {
            Outliner.getInstance().remove((Object)COASTER_PLACEMENT_OUTLINE_ANCHOR_FIRST);
            Outliner.getInstance().remove((Object)COASTER_PLACEMENT_OUTLINE_ANCHOR_HOVER);
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void clearCurvePreviewOutlines(int prevSegmentCount) {
        if (prevSegmentCount <= 0) {
            return;
        }
        for (int i = 1; i <= prevSegmentCount; ++i) {
            Outliner.getInstance().remove((Object)Pair.of((Object)COASTER_PLACEMENT_OUTLINE_CURVE, (Object)(i * 2)));
            Outliner.getInstance().remove((Object)Pair.of((Object)COASTER_PLACEMENT_OUTLINE_CURVE, (Object)(i * 2 + 1)));
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    private static int drawCurvePreview(Level level, BezierConnection bc, String key, int railcolor, double previewLift, int prevCount) {
        float s = animation.getValue() * 7.0f / 8.0f + 0.125f;
        float lw = animation.getValue() * 1.0f / 16.0f + 0.0625f;
        return CoasterTrackPlacement.drawCurvePreviewInner(level, bc, key, railcolor, previewLift, prevCount, s, lw, false);
    }

    @OnlyIn(value=Dist.CLIENT)
    public static int drawCoasterCurveOutlinePreview(BezierConnection bc, String outlinerKey, int railColor, double previewLift, int prevSegmentCount) {
        ClientLevel level;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel clientLevel = level = mc != null ? mc.level : null;
        if (level == null) {
            return prevSegmentCount;
        }
        return CoasterTrackPlacement.drawCurvePreviewInner((Level)level, bc, outlinerKey, railColor, previewLift, prevSegmentCount, 1.0f, 0.125f, true);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void clearCurvePreviewOutlinesForKey(String outlinerKey, int prevSegmentCount) {
        if (prevSegmentCount <= 0) {
            return;
        }
        for (int i = 1; i <= prevSegmentCount; ++i) {
            Outliner.getInstance().remove((Object)Pair.of((Object)outlinerKey, (Object)(i * 2)));
            Outliner.getInstance().remove((Object)Pair.of((Object)outlinerKey, (Object)(i * 2 + 1)));
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    public static void clearCoasterCurveOutlinePreview(String outlinerKey, int prevSegmentCount) {
        CoasterTrackPlacement.clearCurvePreviewOutlinesForKey(outlinerKey, prevSegmentCount);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static float previewExtraWorldYForAnchors(@Nullable BezierConnection bc, @Nullable BlockPos pos1, @Nullable BlockPos pos2) {
        Level level = BezierBankBlendLevel.tryGetClientLevel();
        if (level == null) {
            return 0.0f;
        }
        if (bc != null) {
            for (BlockPos p : bc.bePositions) {
                if (p == null || !CoasterTrackPlacement.anchorFacingIsNotUp(level, p)) continue;
                return 0.0f;
            }
            return 0.0f;
        }
        if (pos1 != null && CoasterTrackPlacement.anchorFacingIsNotUp(level, pos1)) {
            return 0.0f;
        }
        if (pos2 != null && CoasterTrackPlacement.anchorFacingIsNotUp(level, pos2)) {
            return 0.0f;
        }
        return 0.0f;
    }

    @OnlyIn(value=Dist.CLIENT)
    private static boolean anchorFacingIsNotUp(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CoasterAnchorpointBlock)) {
            return false;
        }
        return state.getValue((Property)CoasterAnchorpointBlock.FACING) != Direction.UP;
    }

    @OnlyIn(value=Dist.CLIENT)
    private static Vec3 iteratorSpineSampleToWorld(BezierConnection bc, Vec3 iteratorPosition) {
        return iteratorPosition.add(Vec3.atLowerCornerOf((Vec3i)((Vec3i)bc.bePositions.getFirst()))).add(0.0, -0.1875, 0.0);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static int drawCurvePreviewInner(Level level, BezierConnection bc, String key, int railcolor, double previewLift, int prevCount, float smoothBlend, float lineWidth, boolean drawOpenEndExtensions) {
        Vec3 prevLat;
        BlockPos p2;
        BlockPos p1 = (BlockPos)bc.bePositions.getFirst();
        if (CoasterTrackPlacement.shouldSkipAnchorPreviewDraw(level, p1, p2 = (BlockPos)bc.bePositions.getSecond())) {
            CoasterTrackPlacement.clearCurvePreviewOutlinesForKey(key, prevCount);
            return 0;
        }
        if (CoasterTrackPlacement.shouldSkipHeavyCurvePreview(bc)) {
            CoasterTrackPlacement.clearCurvePreviewOutlinesForKey(key, prevCount);
            return 0;
        }
        int actualSegCount = bc.getSegmentCount();
        boolean useIteratorParameterization = actualSegCount >= 16;
        int segCount = useIteratorParameterization ? actualSegCount : 16;
        Vec3 end1 = (Vec3)bc.starts.getFirst();
        Vec3 end2 = (Vec3)bc.starts.getSecond();
        Vec3 finish1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
        Vec3 finish2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
        double iteratorWorldY = 0.1875;
        double liftAlongFaceUp = previewLift - iteratorWorldY;
        double railHalf = CoasterTrackGauge.coasterPreviewRailHalfGauge();
        float extraWorldY = CoasterTrackPlacement.previewExtraWorldYForAnchors(bc, null, null);
        Vec3 worldYOffset = new Vec3(0.0, (double)(CoasterTrackGauge.coasterPreviewRailWorldYOffsetBlocks() + extraWorldY), 0.0);
        @Nullable BlockPos sublevelHost = CoasterTrackPlacement.previewSublevelHost(level, p1, p2);
        float partialTick = level.isClientSide() ? AnimationTickHolder.getPartialTicks((LevelAccessor)level) : 0.0f;
        Vec3[] spine = new Vec3[segCount + 1];
        Vec3[] lateralUnit = new Vec3[segCount + 1];
        Vec3[] tangentUnit = new Vec3[segCount + 1];
        Level previewLevel = BezierBankBlendLevel.tryGetClientLevel();
        boolean filledFromIterator = false;
        if (useIteratorParameterization) {
            prevLat = null;
            int idx = 0;
            for (BezierConnection.Segment segment : bc) {
                Vec3 tan;
                float segT = bc.getSegmentT(segment.index);
                Vec3 lat = CoasterBezierRailFrames.lateralAt(bc, segT, segment.derivative, previewLevel);
                if (prevLat != null && lat.dot(prevLat) < 0.0) {
                    lat = lat.scale(-1.0);
                }
                tan = (tan = segment.derivative).lengthSqr() < 1.0E-14 ? CoasterBezierRailFrames.unitTangentAt(bc, segT) : tan.normalize();
                spine[idx] = CoasterTrackPlacement.iteratorSpineSampleToWorld(bc, segment.position);
                lateralUnit[idx] = lat;
                tangentUnit[idx] = tan;
                prevLat = lat;
                if (++idx <= segCount + 1) continue;
                break;
            }
            boolean bl = filledFromIterator = idx == segCount + 1;
        }
        if (!filledFromIterator) {
            prevLat = null;
            for (int i = 0; i <= segCount; ++i) {
                float t = useIteratorParameterization ? bc.getSegmentT(i) : (float)i / (float)segCount;
                spine[i] = VecHelper.bezier((Vec3)end1, (Vec3)end2, (Vec3)finish1, (Vec3)finish2, (float)t);
                Vec3 tan = VecHelper.bezierDerivative((Vec3)end1, (Vec3)end2, (Vec3)finish1, (Vec3)finish2, (float)t);
                tan = tan.lengthSqr() < 1.0E-12 ? CoasterBezierRailFrames.unitTangentAt(bc, t) : tan.normalize();
                Vec3 lat = CoasterBezierRailFrames.lateralAt(bc, t, tan, previewLevel);
                if (prevLat != null && lat.dot(prevLat) < 0.0) {
                    lat = lat.scale(-1.0);
                }
                lateralUnit[i] = lat;
                tangentUnit[i] = tan;
                prevLat = lat;
            }
        }
        Vec3 prevRail1 = spine[0].add(lateralUnit[0].scale(railHalf));
        Vec3 prevRail2 = spine[0].subtract(lateralUnit[0].scale(railHalf));
        for (int i = 1; i <= segCount; ++i) {
            Vec3 latI = lateralUnit[i];
            Vec3 currRail1 = spine[i].add(latI.scale(railHalf));
            Vec3 currRail2 = spine[i].subtract(latI.scale(railHalf));
            Vec3 diff1 = currRail1.subtract(prevRail1);
            Vec3 chordFaceUp1Raw = diff1.cross(latI);
            Vec3 chordFaceUp1 = chordFaceUp1Raw.lengthSqr() < 1.0E-12 ? Vec3.ZERO : chordFaceUp1Raw.normalize();
            Vec3 lift1 = chordFaceUp1.scale(liftAlongFaceUp).add(worldYOffset);
            Vec3 prevW1 = prevRail1.add(lift1);
            Vec3 currW1 = currRail1.add(lift1);
            Vec3 diff2 = currRail2.subtract(prevRail2);
            Vec3 chordFaceUp2Raw = diff2.cross(latI);
            Vec3 chordFaceUp2 = chordFaceUp2Raw.lengthSqr() < 1.0E-12 ? Vec3.ZERO : chordFaceUp2Raw.normalize();
            Vec3 lift2 = chordFaceUp2.scale(liftAlongFaceUp).add(worldYOffset);
            Vec3 prevW2 = prevRail2.add(lift2);
            Vec3 currW2 = currRail2.add(lift2);
            prevW1 = CoasterTrackPlacement.previewPointToRenderWorld(level, sublevelHost, prevW1, partialTick);
            currW1 = CoasterTrackPlacement.previewPointToRenderWorld(level, sublevelHost, currW1, partialTick);
            prevW2 = CoasterTrackPlacement.previewPointToRenderWorld(level, sublevelHost, prevW2, partialTick);
            currW2 = CoasterTrackPlacement.previewPointToRenderWorld(level, sublevelHost, currW2, partialTick);
            Vec3 middle1 = prevW1.add(currW1).scale(0.5);
            Vec3 middle2 = prevW2.add(currW2).scale(0.5);
            Outliner.getInstance().showLine((Object)Pair.of((Object)key, (Object)(i * 2)), VecHelper.lerp((float)smoothBlend, (Vec3)middle1, (Vec3)prevW1), VecHelper.lerp((float)smoothBlend, (Vec3)middle1, (Vec3)currW1)).colored(railcolor).disableLineNormals().lineWidth(lineWidth);
            Outliner.getInstance().showLine((Object)Pair.of((Object)key, (Object)(i * 2 + 1)), VecHelper.lerp((float)smoothBlend, (Vec3)middle2, (Vec3)prevW2), VecHelper.lerp((float)smoothBlend, (Vec3)middle2, (Vec3)currW2)).colored(railcolor).disableLineNormals().lineWidth(lineWidth);
            prevRail1 = currRail1;
            prevRail2 = currRail2;
        }
        int outlineSegments = segCount;
        if (drawOpenEndExtensions) {
            if (CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, true)) {
                float ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, true);
                CoasterTrackPlacement.drawOpenEndExtensionOutlineSegment(level, bc, key, ++outlineSegments, railcolor, smoothBlend, lineWidth, spine[0], lateralUnit[0], tangentUnit[0], true, ext, railHalf, liftAlongFaceUp, worldYOffset, sublevelHost, partialTick);
            }
            if (CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, false)) {
                float ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, false);
                CoasterTrackPlacement.drawOpenEndExtensionOutlineSegment(level, bc, key, ++outlineSegments, railcolor, smoothBlend, lineWidth, spine[segCount], lateralUnit[segCount], tangentUnit[segCount], false, ext, railHalf, liftAlongFaceUp, worldYOffset, sublevelHost, partialTick);
            }
        }
        for (int i = outlineSegments + 1; i <= prevCount; ++i) {
            Outliner.getInstance().remove((Object)Pair.of((Object)key, (Object)(i * 2)));
            Outliner.getInstance().remove((Object)Pair.of((Object)key, (Object)(i * 2 + 1)));
        }
        return outlineSegments;
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void drawOpenEndExtensionOutlineSegment(Level level, BezierConnection bc, String key, int outlinePairIndex, int railColor, float smoothBlend, float lineWidth, Vec3 junctionSpine, Vec3 lat, Vec3 tan, boolean atStart, float extLen, double railHalf, double liftAlongFaceUp, Vec3 worldYOffset, @Nullable BlockPos sublevelHost, float partialTick) {
        Vec3 tipRail2;
        if (extLen < 1.0E-6f) {
            return;
        }
        Vec3 latN = lat.lengthSqr() < 1.0E-14 ? new Vec3(1.0, 0.0, 0.0) : lat.normalize();
        Vec3 tanN = tan.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : tan.normalize();
        Vec3 junctionRail1 = junctionSpine.add(latN.scale(railHalf));
        Vec3 junctionRail2 = junctionSpine.subtract(latN.scale(railHalf));
        Vec3 tipRail1 = atStart ? junctionRail1.subtract(tanN.scale((double)extLen)) : junctionRail1.add(tanN.scale((double)extLen));
        Vec3 vec3 = tipRail2 = atStart ? junctionRail2.subtract(tanN.scale((double)extLen)) : junctionRail2.add(tanN.scale((double)extLen));
        if (atStart) {
            CoasterTrackPlacement.drawOutlineRailLine(level, key, outlinePairIndex, true, railColor, smoothBlend, lineWidth, tipRail1, junctionRail1, latN, liftAlongFaceUp, worldYOffset, sublevelHost, partialTick);
            CoasterTrackPlacement.drawOutlineRailLine(level, key, outlinePairIndex, false, railColor, smoothBlend, lineWidth, tipRail2, junctionRail2, latN, liftAlongFaceUp, worldYOffset, sublevelHost, partialTick);
        } else {
            CoasterTrackPlacement.drawOutlineRailLine(level, key, outlinePairIndex, true, railColor, smoothBlend, lineWidth, junctionRail1, tipRail1, latN, liftAlongFaceUp, worldYOffset, sublevelHost, partialTick);
            CoasterTrackPlacement.drawOutlineRailLine(level, key, outlinePairIndex, false, railColor, smoothBlend, lineWidth, junctionRail2, tipRail2, latN, liftAlongFaceUp, worldYOffset, sublevelHost, partialTick);
        }
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void drawOutlineRailLine(Level level, String key, int outlinePairIndex, boolean firstRail, int railColor, float smoothBlend, float lineWidth, Vec3 fromRail, Vec3 toRail, Vec3 latI, double liftAlongFaceUp, Vec3 worldYOffset, @Nullable BlockPos sublevelHost, float partialTick) {
        Vec3 diff = toRail.subtract(fromRail);
        Vec3 chordFaceUpRaw = diff.cross(latI);
        Vec3 chordFaceUp = chordFaceUpRaw.lengthSqr() < 1.0E-12 ? Vec3.ZERO : chordFaceUpRaw.normalize();
        Vec3 lift = chordFaceUp.scale(liftAlongFaceUp).add(worldYOffset);
        Vec3 fromW = CoasterTrackPlacement.previewPointToRenderWorld(level, sublevelHost, fromRail.add(lift), partialTick);
        Vec3 toW = CoasterTrackPlacement.previewPointToRenderWorld(level, sublevelHost, toRail.add(lift), partialTick);
        Vec3 middle = fromW.add(toW).scale(0.5);
        int lineId = outlinePairIndex * 2 + (firstRail ? 0 : 1);
        Outliner.getInstance().showLine((Object)Pair.of((Object)key, (Object)lineId), VecHelper.lerp((float)smoothBlend, (Vec3)middle, (Vec3)fromW), VecHelper.lerp((float)smoothBlend, (Vec3)middle, (Vec3)toW)).colored(railColor).disableLineNormals().lineWidth(lineWidth);
    }

    @OnlyIn(value=Dist.CLIENT)
    @Nullable
    private static BlockPos previewSublevelHost(Level level, BlockPos p1, BlockPos p2) {
        if (Sable.HELPER.getContaining(level, (Vec3i)p1) != null) {
            return p1;
        }
        if (Sable.HELPER.getContaining(level, (Vec3i)p2) != null) {
            return p2;
        }
        return null;
    }

    @OnlyIn(value=Dist.CLIENT)
    private static Vec3 previewPointToRenderWorld(Level level, @Nullable BlockPos sublevelHost, Vec3 plotLocal, float partialTick) {
        return sublevelHost == null ? plotLocal : CoasterAnchorClientSpace.toRenderWorld(level, sublevelHost, plotLocal, partialTick);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void line(int id, Vec3 v1, Vec3 o1, Vec3 ex) {
        int color = Color.mixColors((int)15359019, (int)1940687, (float)animation.getValue());
        CoasterTrackPlacement.line(id, v1, o1, ex, color);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void line(int id, Vec3 v1, Vec3 o1, Vec3 ex, int color) {
        Outliner.getInstance().showLine((Object)Pair.of((Object)COASTER_PLACEMENT_OUTLINE_START, (Object)id), v1.subtract(o1), v1.add(ex)).lineWidth(0.125f).disableLineNormals().colored(color);
    }

    private static boolean isCreateTrainTrack(Block block) {
        return block instanceof TrackBlock;
    }

    private static PlacementInfo denyCoasterAnchorsOnlyPlacement(PlacementInfo info) {
        return info.withModTranslationKey("simulatedcoasters.track.must_attach_to_anchors").tooJumbly();
    }

    private static boolean coasterAnchorsOnlyPlacementForbidden(@Nullable BlockPos anchorFirst, BlockState state2) {
        boolean secondIsAnchor = state2.getBlock() instanceof CoasterAnchorpointBlock;
        return !secondIsAnchor || anchorFirst == null;
    }

    public static void applyCoasterAnchorsOnlyPreviewInvalidMarks(Level level, PlacementInfo info, BlockPos pos2, BlockState state2, ItemStack stack) {
        if (!level.isClientSide()) {
            return;
        }
        if (!(stack.getItem() instanceof CoasterTrackItem)) {
            return;
        }
        if (!info.trackMaterial.id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return;
        }
        BlockPos anchorFirst = CoasterTrackPlacement.readAnchorFirstSelection(stack);
        if (!CoasterTrackPlacement.coasterAnchorsOnlyPlacementForbidden(anchorFirst, state2)) {
            return;
        }
        if (info.end1 == null || info.axis1 == null || info.end2 == null || info.axis2 == null || info.normal1 == null || info.normal2 == null) {
            return;
        }
        info.valid = false;
        info.message = null;
    }

    private static PlacementInfo denyCoasterCrossPreview(Level level, BlockPos pos2, ItemStack stack, boolean maximiseTurn, int lookAngle, boolean suppressNeighborSmooth) {
        PlacementInfo bad = new PlacementInfo(CoasterTrackMaterials.COASTER).tooJumbly();
        if (level.isClientSide()) {
            hoveringMaxed = maximiseTurn;
            hoveringAngle = lookAngle;
            hoveringSuppressNeighborSmooth = suppressNeighborSmooth;
            hoveringPos = pos2;
            lastItem = stack;
            cached = bad;
        }
        return bad;
    }

    public static boolean validateEditedCoasterCurve(Level level, BezierConnection bc) {
        return CoasterTrackPlacement.validateEditedCoasterCurve(level, bc, null);
    }

    public static boolean validateEditedCoasterCurve(Level level, BezierConnection bc, @Nullable List<BezierConnection> previewBatch) {
        return bc != null && bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id);
    }

    public static boolean validateCoasterCurveForPlacement(Level level, BezierConnection bc, @Nullable List<BezierConnection> previewBatch) {
        return CoasterTrackPlacement.validateEditedCoasterCurve(level, bc, previewBatch);
    }

    public static BezierConnection curveWithAxisAtEndpointForEdit(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newAxisIntoCurve) {
        return CoasterTrackPlacement.withAxisAtEndpoint(level, bc, endpoint, newAxisIntoCurve, false);
    }

    public static BezierConnection curveWithAxisAtEndpointPreservingPeerForJoin(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newAxisIntoCurve) {
        return CoasterTrackPlacement.withAxisAtEndpoint(level, bc, endpoint, newAxisIntoCurve, true);
    }

    public static BezierConnection curveWithStartAtAnchorEndpoint(Level level, BezierConnection bc, BlockPos anchorPos, Vec3 newStart) {
        if (((BlockPos)bc.bePositions.getFirst()).equals((Object)anchorPos)) {
            return CoasterTrackPlacement.withStartAndFrameAtEndpoint(level, bc, anchorPos, newStart, (Vec3)bc.axes.getFirst(), (Vec3)bc.normals.getFirst());
        }
        if (((BlockPos)bc.bePositions.getSecond()).equals((Object)anchorPos)) {
            return CoasterTrackPlacement.withStartAndFrameAtEndpoint(level, bc, anchorPos, newStart, (Vec3)bc.axes.getSecond(), (Vec3)bc.normals.getSecond());
        }
        return bc;
    }

    public static BezierConnection curveWithStartAndAxisAtEndpointForEdit(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newStart, Vec3 newAxisIntoCurve) {
        if (bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id) && newAxisIntoCurve.lengthSqr() > 1.0E-12) {
            newAxisIntoCurve = newAxisIntoCurve.normalize();
        }
        Vec3 normal = CoasterTrackPlacement.anchorEndpointNormal(level, endpoint, newAxisIntoCurve);
        return CoasterTrackPlacement.withStartAndFrameAtEndpoint(level, bc, endpoint, newStart, newAxisIntoCurve, normal);
    }

    public static BezierConnection curveWithStartAndReferenceNormalAtAnchorEndpoint(Level level, BezierConnection bc, BlockPos anchorPos, Vec3 newStart, Vec3 referenceNormal) {
        Vec3 axis = ((BlockPos)bc.bePositions.getFirst()).equals((Object)anchorPos) ? (Vec3)bc.axes.getFirst() : (Vec3)bc.axes.getSecond();
        return CoasterTrackPlacement.withStartAndFrameAtEndpoint(level, bc, anchorPos, newStart, axis, referenceNormal);
    }

    static {
        animation = LerpedFloat.linear().startWithValue(0.0);
        lastLineCount = 0;
        lastNeighborLineCounts = new int[0];
    }

    public static class PlacementInfo {
        BezierConnection curve = null;
        boolean valid = false;
        int end1Extent = 0;
        int end2Extent = 0;
        String message = null;
        boolean modTranslationKey = false;
        public int requiredTracks = 0;
        public boolean hasRequiredTracks = false;
        public int requiredPavement = 0;
        public boolean hasRequiredPavement = false;
        public final TrackMaterial trackMaterial;
        Vec3 end1;
        Vec3 end2;
        Vec3 normal1;
        Vec3 normal2;
        Vec3 axis1;
        Vec3 axis2;
        BlockPos pos1;
        BlockPos pos2;
        boolean end2IsAnchor;
        boolean anchorToAnchor;
        boolean unlockLockedAnchorPeersAfterCommit;
        @Nullable
        CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult anchorAnchorBuildResult;
        @Nullable
        List<BezierConnection> placementSmoothingNeighbors;

        public PlacementInfo(TrackMaterial material) {
            this.trackMaterial = material;
        }

        public PlacementInfo withMessage(String message) {
            this.message = "track." + message;
            this.modTranslationKey = false;
            return this;
        }

        public PlacementInfo withModTranslationKey(String translationKey) {
            this.message = translationKey;
            this.modTranslationKey = true;
            return this;
        }

        public PlacementInfo tooJumbly() {
            this.curve = null;
            this.valid = false;
            this.anchorAnchorBuildResult = null;
            this.placementSmoothingNeighbors = null;
            return this;
        }
    }

    private record PlacementSmoothingBatch(BezierConnection primary, List<BezierConnection> neighbors) {
    }

    private record CoasterSnapGeometryBackup(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2, Vec3 axis1, Vec3 axis2, Vec3 normedAxis1, Vec3 normedAxis2, Vec3 end1, Vec3 end2, Vec3 normal1, Vec3 normal2, boolean skipCurve, boolean parallel, double[] intersect, double dist, int end1Extent, int end2Extent) {
        static CoasterSnapGeometryBackup capture(BlockPos pos1, BlockPos pos2, BlockState state1, BlockState state2, Vec3 axis1, Vec3 axis2, Vec3 normedAxis1, Vec3 normedAxis2, Vec3 end1, Vec3 end2, Vec3 normal1, Vec3 normal2, boolean skipCurve, boolean parallel, double[] intersect, double dist, int end1Extent, int end2Extent) {
            return new CoasterSnapGeometryBackup(pos1, pos2, state1, state2, axis1, axis2, normedAxis1, normedAxis2, end1, end2, normal1, normal2, skipCurve, parallel, intersect == null ? null : (double[])intersect.clone(), dist, end1Extent, end2Extent);
        }
    }
}

