package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.BezierConnection.Segment;
import com.simibubi.create.content.trains.track.TrackPlacement.ConnectingFrom;
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
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterTrackPropagator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public final class CoasterTrackPlacement {
   private static final double COASTER_FIRST_LEG_AXIS_TIE_SQ = 0.0025;
   private static final String ANCHOR_FIRST_CUSTOM_TAG = "SimulatedCoastersAnchorFirst";
   public static CoasterTrackPlacement.PlacementInfo cached;
   static BlockPos hoveringPos;
   static boolean hoveringMaxed;
   static int hoveringAngle;
   static boolean hoveringSuppressNeighborSmooth;
   static ItemStack lastItem;
   static int extraTipWarmup;
   static LerpedFloat animation = LerpedFloat.linear().startWithValue(0.0);
   static int lastLineCount = 0;
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
   static int[] lastNeighborLineCounts = new int[0];

   public static void setAnchorFirstSelection(ItemStack stack, BlockPos anchor) {
      BlockPos immutable = anchor.immutable();
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      tag.putLong("SimulatedCoastersAnchorFirst", immutable.asLong());
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
      Vec3 center = Vec3.atCenterOf(immutable);
      stack.set(AllDataComponents.TRACK_CONNECTING_FROM, new ConnectingFrom(immutable, Vec3.ZERO, new Vec3(0.0, 1.0, 0.0), center));
   }

   public static void clearAnchorFirstSelection(ItemStack stack) {
      stack.remove(AllDataComponents.TRACK_CONNECTING_FROM);
      if (stack.has(DataComponents.CUSTOM_DATA)) {
         CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
         if (tag.contains("SimulatedCoastersAnchorFirst")) {
            tag.remove("SimulatedCoastersAnchorFirst");
            if (tag.isEmpty()) {
               stack.remove(DataComponents.CUSTOM_DATA);
            } else {
               stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
         }
      }
   }

   public static BlockPos readAnchorFirstSelection(ItemStack stack) {
      if (stack.getItem() instanceof CoasterTrackItem) {
         ConnectingFrom from = (ConnectingFrom)stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
         if (from != null) {
            return from.pos();
         }
      }

      if (!stack.has(DataComponents.CUSTOM_DATA)) {
         return null;
      } else {
         CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
         return !tag.contains("SimulatedCoastersAnchorFirst", 4) ? null : BlockPos.of(tag.getLong("SimulatedCoastersAnchorFirst"));
      }
   }

   public static boolean hasAnchorFirstSelection(ItemStack stack) {
      return stack.getItem() instanceof CoasterTrackItem && stack.has(AllDataComponents.TRACK_CONNECTING_FROM) ? true : readAnchorFirstSelection(stack) != null;
   }

   public static void applyAnchorFirstSelection(Level level, ItemStack stack, BlockPos anchor) {
      applyAnchorFirstSelection(level, stack, anchor, null, InteractionHand.MAIN_HAND);
   }

   public static void applyAnchorFirstSelection(Level level, ItemStack stack, BlockPos anchor, @Nullable Player player, InteractionHand hand) {
      stack.remove(AllDataComponents.TRACK_CONNECTING_FROM);
      setAnchorFirstSelection(stack, anchor.immutable());
      level.playSound(null, anchor, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75F, 1.0F);
      if (!level.isClientSide() && player != null) {
         player.setItemInHand(hand, stack);
         if (player instanceof ServerPlayer sp) {
            CoasterTrackHotbarSelectionSync.syncMainHand(sp, hand, anchor);
         }
      }
   }

   public static boolean tryConnectSecondAnchorFromCurve(Level level, Player player, ItemStack stack, BlockPos secondAnchor) {
      return tryConnectSecondAnchorFromCurve(level, player, stack, secondAnchor, InteractionHand.MAIN_HAND);
   }

   public static boolean tryConnectSecondAnchorFromCurve(Level level, Player player, ItemStack stack, BlockPos secondAnchor, InteractionHand hand) {
      if (!(stack.getItem() instanceof CoasterTrackItem)) {
         return false;
      } else if (!hasAnchorFirstSelection(stack)) {
         return false;
      } else {
         BlockPos first = readAnchorFirstSelection(stack);
         if (first != null && !first.equals(secondAnchor)) {
            BlockState state = level.getBlockState(secondAnchor);
            if (!(state.getBlock() instanceof CoasterAnchorpointBlock)) {
               return false;
            } else if (level.getBlockEntity(secondAnchor) instanceof CoasterAnchorpointBlockEntity be) {
               if (be.legCount() >= 2) {
                  return false;
               } else {
                  stack.remove(AllDataComponents.TRACK_EXTENDED_CURVE);
                  CoasterTrackPlacement.PlacementInfo info = tryConnect(level, player, secondAnchor, state, stack, false, true, false);
                  displayPlacementActionBar(player, info);
                  if (!info.valid) {
                     if (!level.isClientSide()) {
                        AllSoundEvents.DENY.playFrom(player, 1.0F, 1.0F);
                     }

                     return false;
                  } else if (level.isClientSide()) {
                     return true;
                  } else {
                     stack = player.getItemInHand(hand);
                     if (stack.getItem() instanceof CoasterTrackItem) {
                        CoasterTrackItem.clearPendingConnection(stack);
                        player.setItemInHand(hand, stack);
                        if (player instanceof ServerPlayer sp) {
                           CoasterTrackHotbarSelectionSync.syncMainHand(sp, hand, null);
                        }
                     }

                     return true;
                  }
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public static boolean commitSecondAnchorOnServer(ServerPlayer player, InteractionHand hand, BlockPos firstAnchor, BlockPos secondAnchor) {
      if (firstAnchor != null && !firstAnchor.equals(BlockPos.ZERO) && !firstAnchor.equals(secondAnchor)) {
         ServerLevel level = player.serverLevel();
         ItemStack stack = player.getItemInHand(hand);
         if (!(stack.getItem() instanceof CoasterTrackItem)) {
            return false;
         } else if (anchorsAlreadyLinked(level, firstAnchor, secondAnchor)) {
            clearServerTrackSelection(player, hand);
            return true;
         } else if (CoasterTrackPlacementCommitDeferral.isEdgeCommitPending(level, firstAnchor, secondAnchor)) {
            clearServerTrackSelection(player, hand);
            return true;
         } else {
            BlockPos existing = readAnchorFirstSelection(stack);
            if (existing == null || !existing.equals(firstAnchor)) {
               applyAnchorFirstSelection(level, stack, firstAnchor, player, hand);
               stack = player.getItemInHand(hand);
            }

            boolean ok = tryConnectSecondAnchorFromCurve(level, player, stack, secondAnchor, hand);
            if (!ok) {
               clearServerTrackSelection(player, hand);
            }

            return ok;
         }
      } else {
         return false;
      }
   }

   public static void clearServerTrackSelection(ServerPlayer player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.getItem() instanceof CoasterTrackItem) {
         CoasterTrackItem.clearPendingConnection(stack);
         player.setItemInHand(hand, stack);
         CoasterTrackHotbarSelectionSync.syncMainHand(player, hand, null);
      }
   }

   private static boolean anchorsAlreadyLinked(Level level, BlockPos anchorA, BlockPos anchorB) {
      return level.getBlockEntity(anchorA) instanceof CoasterAnchorpointBlockEntity beA ? beA.getAnchorPeerCurvesView().containsKey(anchorB) : false;
   }

   private static boolean suppressNeighborSmoothing(Level level, Player player) {
      if (level.isClientSide()) {
         return CoasterTrackPlacementNeighborSmoothSuppressClient.isSuppressNeighborSmoothing();
      } else {
         return player instanceof ServerPlayer sp ? CoasterTrackPlacementNeighborSmoothSuppress.isSuppress(sp.getUUID()) : false;
      }
   }

   public static CoasterTrackPlacement.PlacementInfo tryConnect(
      Level level, Player player, BlockPos pos2, BlockState state2, ItemStack stack, boolean girder, boolean maximiseTurn, boolean updatePlacementCache
   ) {
      Vec3 lookVec = player.getLookAngle();
      int lookAngle = (int)(22.5 + (double)(AngleHelper.deg(Mth.atan2(lookVec.z, lookVec.x)) % 360.0F)) / 8;
      boolean suppressNeighborSmooth = suppressNeighborSmoothing(level, player);
      boolean touchCache = level.isClientSide() && updatePlacementCache;
      if (touchCache
         && cached != null
         && pos2.equals(hoveringPos)
         && stack.equals(lastItem)
         && hoveringMaxed == maximiseTurn
         && lookAngle == hoveringAngle
         && hoveringSuppressNeighborSmooth == suppressNeighborSmooth) {
         return cached;
      } else {
         if (stack.getItem() instanceof CoasterTrackItem) {
            if (isCreateTrainTrack(state2.getBlock())) {
               return denyCoasterCrossPreview(level, pos2, stack, maximiseTurn, lookAngle, suppressNeighborSmooth);
            }

            ConnectingFrom from = (ConnectingFrom)stack.get(AllDataComponents.TRACK_CONNECTING_FROM);
            if (from != null && isCreateTrainTrack(level.getBlockState(from.pos()).getBlock())) {
               return denyCoasterCrossPreview(level, pos2, stack, maximiseTurn, lookAngle, suppressNeighborSmooth);
            }
         }

         CoasterTrackPlacement.PlacementInfo info = new CoasterTrackPlacement.PlacementInfo(CoasterTrackMaterials.COASTER);
         if (touchCache) {
            hoveringMaxed = maximiseTurn;
            hoveringAngle = lookAngle;
            hoveringSuppressNeighborSmooth = suppressNeighborSmooth;
            hoveringPos = pos2;
            lastItem = stack;
            cached = info;
         }

         BlockPos anchorFirst = stack.getItem() instanceof CoasterTrackItem ? readAnchorFirstSelection(stack) : null;
         if (stack.getItem() instanceof CoasterTrackItem) {
            if (!(state2.getBlock() instanceof CoasterAnchorpointBlock)) {
               return denyCoasterAnchorsOnlyPlacement(info);
            } else if (anchorFirst == null) {
               return info.withMessage("second_point").tooJumbly();
            } else {
               return anchorFirst.equals(pos2)
                  ? info.withMessage("second_point").tooJumbly()
                  : tryConnectAnchorToAnchor(level, player, anchorFirst, pos2, state2, stack, girder, info);
            }
         } else {
            return info.withMessage("second_point").tooJumbly();
         }
      }
   }

   private static CoasterTrackPlacement.PlacementInfo tryConnectAnchorToAnchor(
      Level level,
      Player player,
      BlockPos anchorFirstPos,
      BlockPos anchorSecondPos,
      BlockState anchorSecondState,
      ItemStack stack,
      boolean girder,
      CoasterTrackPlacement.PlacementInfo info
   ) {
      if (!anchorSecondState.is((Block)SimulatedCoastersBlocks.COASTER_ANCHORPOINT.get())) {
         return info.withMessage("second_point").tooJumbly();
      } else if (anchorFirstPos.equals(anchorSecondPos)) {
         return info.withMessage("second_point").tooJumbly();
      } else {
         populatePreviewTrackRequirements(level, player, stack, anchorFirstPos, anchorSecondPos, info);
         if (anchorConnectionAcrossSubLevels(level, anchorFirstPos, anchorSecondPos)) {
            return info.withModTranslationKey("simulatedcoasters.track.anchor_sublevel_connection").tooJumbly();
         } else if (anchorConnectionExceedsMaxSpan(level, anchorFirstPos, anchorSecondPos)) {
            return info.withMessage("too_far").tooJumbly();
         } else {
            if (!(level.getBlockEntity(anchorFirstPos) instanceof CoasterAnchorpointBlockEntity beA)
               || !(level.getBlockEntity(anchorSecondPos) instanceof CoasterAnchorpointBlockEntity beB)) {
               return info.withMessage("original_missing").tooJumbly();
            }

            if (beA.canAcceptAnchorPeer(anchorSecondPos) && beB.canAcceptAnchorPeer(anchorFirstPos)) {
               BlockState anchorFirstState = level.getBlockState(anchorFirstPos);
               if (CoasterAnchorpointBlock.haveOppositeFacing(anchorFirstState, anchorSecondState)) {
                  return info.withModTranslationKey("simulatedcoasters.track.anchor_opposite_facing").tooJumbly();
               } else if (CoasterAnchorpointBlock.haveSameFacingCollinearAlongFacing(anchorFirstState, anchorSecondState, anchorFirstPos, anchorSecondPos)) {
                  return info.withModTranslationKey("simulatedcoasters.track.anchor_same_facing_collinear").tooJumbly();
               } else if (CoasterAnchorpointBlock.connectionHasImpossibleBend(anchorFirstState, anchorSecondState, anchorFirstPos, anchorSecondPos)) {
                  return info.withModTranslationKey("simulatedcoasters.track.anchor_impossible_bend").tooJumbly();
               } else if (CoasterAnchorpointBlock.connectionReversesOnSameFacingCoplanarPlane(level, anchorFirstPos, anchorSecondPos)) {
                  return info.withModTranslationKey("simulatedcoasters.track.anchor_coplanar_plane_reversal").tooJumbly();
               } else {
                  tryConnectAnchorToAnchorAttempt(level, player, anchorFirstPos, anchorSecondPos, stack, girder, info);
                  return info;
               }
            } else {
               return info.withModTranslationKey("simulatedcoasters.track.anchor_full").tooJumbly();
            }
         }
      }
   }

   private static void populatePreviewTrackRequirements(
      Level level, Player player, ItemStack stack, BlockPos anchorA, BlockPos anchorB, CoasterTrackPlacement.PlacementInfo info
   ) {
      info.requiredTracks = CoasterTrackCurveDrops.trackItemCostForAnchorPair(level, anchorA, anchorB);
      info.hasRequiredTracks = player.isCreative() || countCoasterTracksInInventory(player, stack) >= info.requiredTracks;
   }

   private static int countCoasterTracksInInventory(Player player, ItemStack trackStack) {
      int found = 0;
      Inventory inv = player.getInventory();
      int size = inv.items.size();

      for (int j = 0; j <= size + 1; j++) {
         int i = j;
         boolean offhand = j == size + 1;
         if (j == size) {
            i = inv.selected;
         } else if (offhand) {
            i = 0;
         } else if (j == inv.selected) {
            continue;
         }

         ItemStack stackInSlot = (ItemStack)(offhand ? inv.offhand : inv.items).get(i);
         if (stackInSlot.is(trackStack.getItem())) {
            found += stackInSlot.getCount();
         }
      }

      return found;
   }

   @OnlyIn(Dist.CLIENT)
   private static void updateTrackCostOverlay(Minecraft mc, Player player, ItemStack stack, int requiredTracks, boolean hasRequiredTracks, boolean validPreview) {
      CoasterTrackPlacementOverlayRenderer.updateFromPlacementPreview(mc, requiredTracks, hasRequiredTracks, validPreview);
   }

   @OnlyIn(Dist.CLIENT)
   private static void updateTrackCostOverlayForPreviewEndpoints(
      Minecraft mc, Level level, Player player, ItemStack stack, BlockPos fromAnchor, BlockPos previewEnd
   ) {
      int required = CoasterTrackCurveDrops.trackItemCostForPreviewEndpoints(level, fromAnchor, previewEnd);
      boolean hasEnough = player.isCreative() || countCoasterTracksInInventory(player, stack) >= required;
      updateTrackCostOverlay(mc, player, stack, required, hasEnough, false);
   }

   public static boolean anchorConnectionAcrossSubLevels(Level level, BlockPos anchorA, BlockPos anchorB) {
      SubLevel subA = Sable.HELPER.getContaining(level, anchorA);
      SubLevel subB = Sable.HELPER.getContaining(level, anchorB);
      return subA != subB;
   }

   public static boolean anchorConnectionExceedsMaxSpan(Level level, BlockPos anchorA, BlockPos anchorB) {
      long maxSq = CoasterTrackGauge.maxCoasterCurveEndpointSpanBlocksSq();
      return anchorA.distSqr(anchorB) > (double)maxSq
         ? true
         : CoasterAnchorpointBlockEntity.worldCenter(level, anchorA).distanceToSqr(CoasterAnchorpointBlockEntity.worldCenter(level, anchorB)) > (double)maxSq;
   }

   public static boolean anchorConnectionTooFar(Level level, BlockPos anchorA, BlockPos anchorB) {
      return anchorConnectionAcrossSubLevels(level, anchorA, anchorB) || anchorConnectionExceedsMaxSpan(level, anchorA, anchorB);
   }

   static boolean placementHasNeighborsToCoSmooth(Level level, BlockPos anchorA, BlockPos anchorB) {
      return anchorHasExistingLegsBesides(level, anchorA, anchorB) || anchorHasExistingLegsBesides(level, anchorB, anchorA);
   }

   private static boolean anchorHasExistingLegsBesides(Level level, BlockPos anchor, BlockPos excludePeer) {
      if (level.getBlockEntity(anchor) instanceof CoasterAnchorpointBlockEntity be) {
         for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
            if (!peer.equals(excludePeer)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static void tryConnectAnchorToAnchorAttempt(
      Level level, Player player, BlockPos anchorFirstPos, BlockPos anchorSecondPos, ItemStack stack, boolean girder, CoasterTrackPlacement.PlacementInfo info
   ) {
      info.valid = false;
      if (!anchorConnectionTooFar(level, anchorFirstPos, anchorSecondPos)) {
         boolean suppressNeighborSmooth = suppressNeighborSmoothing(level, player);
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
            placement = CoasterAnchorBezierOptimizer.buildAnchorAnchorPlacement(
               level, anchorFirstPos, anchorSecondPos, CoasterTrackMaterials.COASTER, girder, suppressNeighborSmooth
            );
         }

         if (placement == null) {
            info.withMessage("too_sharp").tooJumbly();
         } else if (!CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(placement.primary())) {
            info.withMessage("too_sharp").tooJumbly();
         } else if (suppressNeighborSmooth && !CoasterAnchorBezierOptimizer.meetsAltSuppressMinCurveRadius(placement.primary())) {
            info.withMessage("too_sharp").tooJumbly();
         } else if (CoasterAnchorpointBlock.connectionBelowMinLegAngle(level, anchorFirstPos, anchorSecondPos, placement.primary())) {
            info.withModTranslationKey("simulatedcoasters.track.anchor_leg_angle_too_sharp").tooJumbly();
         } else {
            info.curve = placement.primary();
            info.anchorAnchorBuildResult = placement;
            info.pos1 = anchorFirstPos;
            info.pos2 = anchorSecondPos;
            info.anchorToAnchor = true;
            info.valid = true;
            finalizeAnchorToAnchorConnection(level, player, anchorFirstPos, anchorSecondPos, stack, info);
         }
      }
   }

   private static CoasterTrackPlacement.PlacementInfo finalizeAnchorToAnchorConnection(
      Level level, Player player, BlockPos anchorA, BlockPos anchorB, ItemStack stack, CoasterTrackPlacement.PlacementInfo info
   ) {
      info.requiredTracks = CoasterTrackCurveDrops.trackItemCostForAnchorPair(level, anchorA, anchorB);
      info.hasRequiredTracks = true;
      if (!player.isCreative()) {
         for (boolean simulate : Iterate.trueAndFalse) {
            if (level.isClientSide() && !simulate) {
               break;
            }

            int tracks = info.requiredTracks;
            int foundTracks = 0;
            Inventory inv = player.getInventory();
            int size = inv.items.size();

            for (int j = 0; j <= size + 1; j++) {
               int i = j;
               boolean offhand = j == size + 1;
               if (j == size) {
                  i = inv.selected;
               } else if (offhand) {
                  i = 0;
               } else if (j == inv.selected) {
                  continue;
               }

               ItemStack stackInSlot = (ItemStack)(offhand ? inv.offhand : inv.items).get(i);
               if (stackInSlot.is(stack.getItem()) && foundTracks < tracks) {
                  int count = stackInSlot.getCount();
                  if (!simulate) {
                     int remainingItems = count - Math.min(tracks - foundTracks, count);
                     if (i == inv.selected) {
                        stackInSlot.remove(AllDataComponents.TRACK_CONNECTING_FROM);
                        if (stackInSlot.getItem() instanceof CoasterTrackItem) {
                           clearAnchorFirstSelection(stackInSlot);
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
            }

            if (simulate && foundTracks < tracks) {
               info.valid = false;
               info.tooJumbly();
               info.hasRequiredTracks = false;
               return info.withMessage("not_enough_tracks");
            }
         }

         if (!level.isClientSide()) {
            ItemStack selected = player.getInventory().getItem(player.getInventory().selected);
            player.setItemInHand(InteractionHand.MAIN_HAND, selected);
         }
      }

      if (level.isClientSide()) {
         if (info.curve != null && info.valid) {
            long aL = anchorA.asLong();
            long bL = anchorB.asLong();
            BlockPos lo = aL <= bL ? anchorA : anchorB;
            BlockPos hi = aL <= bL ? anchorB : anchorA;
            CoasterTrackPlacement.PlacementSmoothingBatch batch = finalizePlacementSmoothingBatch(level, lo, hi, info.curve, info.anchorAnchorBuildResult);
            BezierConnection bc = batch.primary();
            info.curve = bc;
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
      } else if (level instanceof ServerLevel sl) {
         long var24 = anchorA.asLong();
         long var26 = anchorB.asLong();
         BlockPos lo = var24 <= var26 ? anchorA : anchorB;
         BlockPos hi = var24 <= var26 ? anchorB : anchorA;
         if (level.getBlockEntity(lo) instanceof CoasterAnchorpointBlockEntity beLo && level.getBlockEntity(hi) instanceof CoasterAnchorpointBlockEntity) {
            CoasterTrackPlacementCommitDeferral.scheduleGeometryCommit(sl, lo, hi, () -> CoasterTrackPropagator.runBatchUpdate(sl, () -> {
                  CoasterTrackPlacement.PlacementSmoothingBatch batch = finalizePlacementSmoothingBatch(sl, lo, hi, info.curve, info.anchorAnchorBuildResult);
                  beLo.putAnchorPeerCurve(sl, hi, batch.primary());
                  putPlacementNeighborCurves(sl, batch.neighbors());
                  info.anchorAnchorBuildResult = null;
               }));
            if (player instanceof ServerPlayer sp) {
               CoasterTrackHotbarSelectionSync.syncMainHand(sp, InteractionHand.MAIN_HAND, null);
            }

            if (info.unlockLockedAnchorPeersAfterCommit) {
               info.unlockLockedAnchorPeersAfterCommit = false;
            }

            if (info.valid) {
               displayAnchorWrenchHintAfterPlacement(player, info.trackMaterial);
            }

            return info;
         }

         return info;
      } else {
         return info;
      }
   }

   private static void displayAnchorWrenchHintAfterPlacement(Player player, TrackMaterial material) {
      if (player != null && material.id.equals(CoasterTrackMaterials.COASTER.id)) {
         player.displayClientMessage(Component.translatable("simulatedcoasters.track.anchor_wrench_adjust_curve"), true);
      }
   }

   @Nullable
   public static BezierConnection buildAnchorAnchorBezier(Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder) {
      return CoasterAnchorBezierOptimizer.buildAnchorAnchorBezier(level, anchorA, anchorB, material, girder);
   }

   private static CoasterTrackPlacement.PlacementSmoothingBatch finalizePlacementSmoothingBatch(
      Level level,
      BlockPos placementLo,
      BlockPos placementHi,
      BezierConnection primary,
      @Nullable CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult buildResult
   ) {
      if (buildResult != null && buildResult.circularLoopNeighbors() != null) {
         List<BezierConnection> liftBatch = new ArrayList<>(buildResult.circularLoopNeighbors().size() + 1);
         liftBatch.add(primary);
         liftBatch.addAll(buildResult.circularLoopNeighbors());
         CoasterBezierHandleEdit.repinLiftedStartsOnBatchAnchors(level, liftBatch);
         return new CoasterTrackPlacement.PlacementSmoothingBatch(liftBatch.get(0), List.copyOf(liftBatch.subList(1, liftBatch.size())));
      } else {
         BezierConnection syncedPrimary = syncPlacementCurveHubEndpoints(level, primary, placementLo, placementHi);
         List<BezierConnection> neighbors = List.of();
         if (buildResult != null && !buildResult.neighborJoinWrites().isEmpty()) {
            neighbors = buildPlacementNeighborCurves(level, buildResult.neighborJoinWrites(), placementLo, placementHi);
         }

         CoasterTrackPlacement.PlacementSmoothingBatch pinned = pinSharedPlacementJunctionFrames(level, placementLo, placementHi, syncedPrimary, neighbors);
         List<BezierConnection> liftBatch = new ArrayList<>(pinned.neighbors().size() + 1);
         liftBatch.add(pinned.primary());
         liftBatch.addAll(pinned.neighbors());
         CoasterBezierHandleEdit.repinLiftedStartsOnBatchAnchors(level, liftBatch);
         return new CoasterTrackPlacement.PlacementSmoothingBatch(liftBatch.get(0), List.copyOf(liftBatch.subList(1, liftBatch.size())));
      }
   }

   private static void putPlacementNeighborCurves(ServerLevel level, List<BezierConnection> neighbors) {
      if (!neighbors.isEmpty()) {
         Set<String> done = new HashSet<>();

         for (BezierConnection refreshed : neighbors) {
            String k = CoasterBezierHandleEdit.edgeKey(refreshed);
            if (done.add(k)) {
               commitExistingAnchorPeerCurve(level, refreshed);
            }
         }
      }
   }

   private static void commitExistingAnchorPeerCurve(ServerLevel level, BezierConnection primaryCurve) {
      BlockPos a = (BlockPos)primaryCurve.bePositions.getFirst();
      BlockPos b = (BlockPos)primaryCurve.bePositions.getSecond();

      for (BlockPos host : List.of(a, b)) {
         if (level.getBlockEntity(host) instanceof CoasterAnchorpointBlockEntity ape) {
            BlockPos peer = host.equals(a) ? b : a;
            if (ape.getAnchorPeerCurvesView().containsKey(peer)) {
               ape.putAnchorPeerCurve(level, peer, primaryCurve);
               return;
            }
         }
      }

      long aL = a.asLong();
      long bL = b.asLong();
      BlockPos lo = aL <= bL ? a : b;
      BlockPos hi = aL <= bL ? b : a;
      if (level.getBlockEntity(lo) instanceof CoasterAnchorpointBlockEntity loBe) {
         loBe.putAnchorPeerCurve(level, hi, primaryCurve);
      }
   }

   private static CoasterTrackPlacement.PlacementSmoothingBatch pinSharedPlacementJunctionFrames(
      Level level, BlockPos placementLo, BlockPos placementHi, BezierConnection primary, List<BezierConnection> neighbors
   ) {
      ArrayList<BezierConnection> all = new ArrayList<>(neighbors.size() + 1);
      all.add(primary);
      all.addAll(neighbors);

      for (BlockPos hub : new BlockPos[]{placementLo, placementHi}) {
         BlockPos placementPeer = placementPeerAtHub(hub, placementLo, placementHi);
         Vec3 sharedStart = resolvePlacementStartAtHub(level, hub, placementPeer, CoasterAnchorpointBlockEntity.worldCenter(level, hub));

         for (int i = 0; i < all.size(); i++) {
            BezierConnection c = all.get(i);
            if (((BlockPos)c.bePositions.getFirst()).equals(hub) || ((BlockPos)c.bePositions.getSecond()).equals(hub)) {
               Vec3 axis = hub.equals(c.bePositions.getFirst()) ? (Vec3)c.axes.getFirst() : (Vec3)c.axes.getSecond();
               if (!(axis.lengthSqr() < 1.0E-12)) {
                  axis = axis.normalize();
                  Vec3 normal = resolvePlacementNormalAtHub(level, hub, placementPeer, axis);
                  BezierConnection updated = withStartAndFrameAtEndpoint(level, c, hub, sharedStart, axis, normal);
                  CoasterPeerCurveHandleLengths.carryLengthsToReplacement(c, updated);
                  all.set(i, updated);
               }
            }
         }
      }

      return new CoasterTrackPlacement.PlacementSmoothingBatch(all.get(0), List.copyOf(all.subList(1, all.size())));
   }

   private static List<BezierConnection> buildPlacementNeighborCurves(
      Level level, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite> writes, BlockPos placementLo, BlockPos placementHi
   ) {
      if (writes.isEmpty()) {
         return List.of();
      } else {
         LinkedHashMap<String, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite>> byCurve = neighborJoinWritesByCurve(writes);
         List<BezierConnection> out = new ArrayList<>(byCurve.size());

         for (List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite> curveWrites : byCurve.values()) {
            BezierConnection preview = curveFromNeighborJoinWrites(level, curveWrites, placementLo, placementHi);
            if (preview != null) {
               out.add(preview);
            }
         }

         return out;
      }
   }

   @Nullable
   private static BezierConnection refreshedCurveFromNeighborJoinWrite(Level level, CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite w, Set<String> seen) {
      BlockPos shared = w.sharedAnchor();
      BlockPos remote = w.remotePos();
      Vec3 axis = w.axisIntoCurveAtSharedAnchor();
      if (axis.lengthSqr() < 1.0E-12) {
         return null;
      } else if (!seen.add(shared.asLong() + ":" + remote.asLong())) {
         return null;
      } else {
         BezierConnection raw = null;
         if (level.getBlockEntity(shared) instanceof CoasterAnchorpointBlockEntity ape) {
            raw = ape.getAnchorPeerCurvesView().get(remote);
         }

         if (raw == null) {
            return null;
         } else {
            BezierConnection p = raw.isPrimary() ? raw : raw.secondary();
            if (!p.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
               return null;
            } else {
               boolean atShared = ((BlockPos)p.bePositions.getFirst()).equals(shared) || ((BlockPos)p.bePositions.getSecond()).equals(shared);
               if (!atShared) {
                  return null;
               } else {
                  BezierConnection refreshed = curveWithAxisAtEndpointPreservingPeerForJoin(level, p, shared, axis);
                  CoasterPeerCurveHandleLengths.copyEdgeLengths(p, refreshed);
                  return refreshed;
               }
            }
         }
      }
   }

   private static String canonicalAnchorPairKey(BlockPos a, BlockPos b) {
      long al = a.asLong();
      long bl = b.asLong();
      return al <= bl ? al + ":" + bl : bl + ":" + al;
   }

   private static LinkedHashMap<String, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite>> neighborJoinWritesByCurve(
      List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite> writes
   ) {
      LinkedHashMap<String, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite>> byCurve = new LinkedHashMap<>();
      Set<String> seenWrite = new HashSet<>();

      for (CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite w : writes) {
         if (!(w.axisIntoCurveAtSharedAnchor().lengthSqr() < 1.0E-12)) {
            String writeKey = w.sharedAnchor().asLong() + ":" + w.remotePos().asLong();
            if (seenWrite.add(writeKey)) {
               byCurve.computeIfAbsent(canonicalAnchorPairKey(w.sharedAnchor(), w.remotePos()), k -> new ArrayList<>()).add(w);
            }
         }
      }

      return byCurve;
   }

   @Nullable
   private static BezierConnection fetchCommittedNeighborCurve(Level level, BlockPos anchor, BlockPos peer) {
      if (!(level.getBlockEntity(anchor) instanceof CoasterAnchorpointBlockEntity ape)) {
         return null;
      } else {
         BezierConnection raw = ape.getAnchorPeerCurvesView().get(peer);
         if (raw == null) {
            return null;
         } else {
            BezierConnection p = raw.isPrimary() ? raw : raw.secondary();
            if (!p.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
               return null;
            } else {
               boolean atAnchor = ((BlockPos)p.bePositions.getFirst()).equals(anchor) || ((BlockPos)p.bePositions.getSecond()).equals(anchor);
               return atAnchor ? p : null;
            }
         }
      }
   }

   @Nullable
   private static BezierConnection curveFromNeighborJoinWrites(
      Level level, List<CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite> writes, BlockPos placementLo, BlockPos placementHi
   ) {
      if (writes.isEmpty()) {
         return null;
      } else {
         BezierConnection current = fetchCommittedNeighborCurve(level, writes.get(0).sharedAnchor(), writes.get(0).remotePos());
         if (current == null) {
            return null;
         } else {
            for (CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite w : writes) {
               Vec3 axis = w.axisIntoCurveAtSharedAnchor();
               if (!(axis.lengthSqr() < 1.0E-12)) {
                  BlockPos shared = w.sharedAnchor();
                  if (((BlockPos)current.bePositions.getFirst()).equals(shared) || ((BlockPos)current.bePositions.getSecond()).equals(shared)) {
                     BezierConnection before = current;
                     current = applyPlacementNeighborJoinWrite(level, current, w, placementLo, placementHi);
                     CoasterPeerCurveHandleLengths.copyEdgeLengths(before, current);
                  }
               }
            }

            return current;
         }
      }
   }

   private static BezierConnection applyPlacementNeighborJoinWrite(
      Level level, BezierConnection bc, CoasterAnchorBezierOptimizer.NeighborJoinAxisWrite write, BlockPos placementLo, BlockPos placementHi
   ) {
      BlockPos shared = write.sharedAnchor();
      Vec3 axis = write.axisIntoCurveAtSharedAnchor();
      if (isPlacementSpanEndpoint(shared, placementLo, placementHi)) {
         BlockPos placementPeer = placementPeerAtHub(shared, placementLo, placementHi);
         return syncPlacementHubEndpointFrame(level, bc, shared, placementPeer, axis);
      } else {
         CoasterBezierEndpointData at = CoasterBezierEndpointData.at(bc, shared);
         if (at == null) {
            return bc;
         } else {
            Vec3 normal = anchorFaceNormalForAxis(level, shared, axis);
            return withStartAndFrameAtEndpoint(level, bc, shared, at.start(), axis, normal);
         }
      }
   }

   private static boolean isPlacementSpanEndpoint(BlockPos anchor, BlockPos placementLo, BlockPos placementHi) {
      return anchor.equals(placementLo) || anchor.equals(placementHi);
   }

   @OnlyIn(Dist.CLIENT)
   private static void drawNeighborSmoothPreviewCurves(Level level, List<BezierConnection> curves, int railColor) {
      clearNeighborCurvePreviewOutlines();
      if (!curves.isEmpty()) {
         double previewLift = (double)CoasterTrackGauge.coasterPreviewSpineVerticalBumpBlocks();
         lastNeighborLineCounts = new int[curves.size()];

         for (int i = 0; i < curves.size(); i++) {
            String key = "curve_neighbor" + i;
            lastNeighborLineCounts[i] = drawCurvePreview(level, curves.get(i), key, railColor, previewLift, 0);
         }
      }
   }

   private static BlockPos placementPeerAtHub(BlockPos hub, BlockPos placementLo, BlockPos placementHi) {
      return hub.equals(placementLo) ? placementHi : placementLo;
   }

   private static BezierConnection syncPlacementHubEndpointFrame(Level level, BezierConnection bc, BlockPos hub, BlockPos placementPeer, Vec3 axisIntoCurve) {
      if (!bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
         return bc;
      } else if (!((BlockPos)bc.bePositions.getFirst()).equals(hub) && !((BlockPos)bc.bePositions.getSecond()).equals(hub)) {
         return bc;
      } else {
         Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-12 ? axisIntoCurve : axisIntoCurve.normalize();
         Vec3 start = resolvePlacementStartForSmoothedLegAtHub(level, bc, hub, placementPeer);
         Vec3 normal = resolvePlacementNormalForSmoothedLegAtHub(level, bc, hub, placementPeer, axis);
         return withStartAndFrameAtEndpoint(level, bc, hub, start, axis, normal);
      }
   }

   private static Vec3 resolvePlacementStartForSmoothedLegAtHub(Level level, BezierConnection bc, BlockPos hub, BlockPos placementPeer) {
      CoasterBezierEndpointData at = CoasterBezierEndpointData.at(bc, hub);
      return at != null ? at.start() : resolvePlacementStartAtHub(level, hub, placementPeer, CoasterAnchorpointBlockEntity.worldCenter(level, hub));
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

      return resolvePlacementNormalAtHub(level, hub, placementPeer, axisIntoCurve);
   }

   private static BezierConnection syncPlacementCurveHubEndpoints(Level level, BezierConnection bc, BlockPos placementLo, BlockPos placementHi) {
      if (!bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
         return bc;
      } else {
         Vec3 axisLo = (Vec3)bc.axes.getFirst();
         Vec3 axisHi = (Vec3)bc.axes.getSecond();
         bc = syncPlacementHubEndpointFrame(level, bc, placementLo, placementHi, axisLo);
         return syncPlacementHubEndpointFrame(level, bc, placementHi, placementLo, axisHi);
      }
   }

   private static BezierConnection withAxisAtEndpoint(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newAxis, boolean preservePeerEndpoint) {
      if (bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id) && newAxis.lengthSqr() > 1.0E-12) {
         newAxis = newAxis.normalize();
      }

      if (preservePeerEndpoint && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
         Vec3 s1 = (Vec3)bc.starts.getFirst();
         Vec3 s2 = (Vec3)bc.starts.getSecond();
         Vec3 a1 = (Vec3)bc.axes.getFirst();
         Vec3 a2 = (Vec3)bc.axes.getSecond();
         Vec3 n1 = (Vec3)bc.normals.getFirst();
         Vec3 n2 = (Vec3)bc.normals.getSecond();
         if (((BlockPos)bc.bePositions.getFirst()).equals(endpoint)) {
            a1 = newAxis;
            n1 = orthonormalFaceNormalForAxis(level, endpoint, n1, newAxis);
         }

         if (((BlockPos)bc.bePositions.getSecond()).equals(endpoint)) {
            a2 = newAxis;
            n2 = orthonormalFaceNormalForAxis(level, endpoint, n2, newAxis);
         }

         return new BezierConnection(
            Couple.create((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond()),
            Couple.create(s1, s2),
            Couple.create(a1, a2),
            Couple.create(n1, n2),
            bc.isPrimary(),
            bc.hasGirder,
            bc.getMaterial()
         );
      } else {
         Vec3 s1x = (Vec3)bc.starts.getFirst();
         Vec3 s2x = (Vec3)bc.starts.getSecond();
         Vec3 a1x = (Vec3)bc.axes.getFirst();
         Vec3 a2x = (Vec3)bc.axes.getSecond();
         Vec3 n1x = (Vec3)bc.normals.getFirst();
         Vec3 n2x = (Vec3)bc.normals.getSecond();
         if (((BlockPos)bc.bePositions.getFirst()).equals(endpoint)) {
            a1x = newAxis;
         }

         if (((BlockPos)bc.bePositions.getSecond()).equals(endpoint)) {
            a2x = newAxis;
         }

         if (((BlockPos)bc.bePositions.getFirst()).equals(endpoint) && level.getBlockState(endpoint).getBlock() instanceof CoasterAnchorpointBlock) {
            s1x = CoasterAnchorpointBlockEntity.worldCenter(level, endpoint);
            n1x = anchorEndpointNormal(level, endpoint, newAxis);
         } else if (((BlockPos)bc.bePositions.getSecond()).equals(endpoint) && level.getBlockState(endpoint).getBlock() instanceof CoasterAnchorpointBlock) {
            s2x = CoasterAnchorpointBlockEntity.worldCenter(level, endpoint);
            n2x = anchorEndpointNormal(level, endpoint, newAxis);
         }

         return new BezierConnection(
            Couple.create((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond()),
            Couple.create(s1x, s2x),
            Couple.create(a1x, a2x),
            Couple.create(n1x, n2x),
            bc.isPrimary(),
            bc.hasGirder,
            bc.getMaterial()
         );
      }
   }

   public static Vec3 anchorGeometricFaceNormal(Level level, BlockPos anchorPos, Vec3 axisIntoCurve) {
      return anchorEndpointNormalGeometric(level, anchorPos, axisIntoCurve);
   }

   public static Vec3 anchorFaceNormalForAxis(Level level, BlockPos anchorPos, Vec3 axisIntoCurve) {
      return anchorEndpointNormal(level, anchorPos, axisIntoCurve);
   }

   public static Vec3 resolvePlacementStartAtHub(Level level, BlockPos hub, BlockPos newPeer, Vec3 fallbackWorldCenter) {
      CoasterBezierEndpointData incident = incidentHubEndpointFromExistingLeg(level, hub, newPeer);
      return incident != null ? CoasterBezierHandleEdit.liftedWorldCenterFromCanonicalTangent(level, hub, incident.axis()) : fallbackWorldCenter;
   }

   public static Vec3 resolvePlacementNormalAtHub(Level level, BlockPos hub, BlockPos newPeer, Vec3 axisIntoNewLeg) {
      CoasterBezierEndpointData incident = incidentHubEndpointFromExistingLeg(level, hub, newPeer);
      if (incident != null && incident.normal().lengthSqr() > 1.0E-12) {
         Vec3 axis = axisIntoNewLeg.lengthSqr() < 1.0E-12 ? axisIntoNewLeg : axisIntoNewLeg.normalize();
         Vec3 ref = incident.normal().normalize();
         Vec3 inPlane = ref.subtract(axis.scale(ref.dot(axis)));
         if (inPlane.lengthSqr() > 1.0E-12) {
            return inPlane.normalize();
         } else {
            Vec3 fallback = anchorFaceNormalForAxis(level, hub, axis);
            return fallback.lengthSqr() > 1.0E-12 && fallback.normalize().dot(ref) < 0.0 ? fallback.scale(-1.0) : fallback;
         }
      } else {
         return anchorFaceNormalForAxis(level, hub, axisIntoNewLeg);
      }
   }

   @Nullable
   public static Vec3 storedHubStartFromIncidentCurve(Level level, BlockPos hub, BlockPos excludePeer) {
      CoasterBezierEndpointData at = incidentHubEndpointFromExistingLeg(level, hub, excludePeer);
      return at == null ? null : at.start();
   }

   @Nullable
   static CoasterBezierEndpointData incidentHubEndpointFromExistingLeg(Level level, BlockPos hub, BlockPos excludePeer) {
      if (!(level.getBlockEntity(hub) instanceof CoasterAnchorpointBlockEntity be)) {
         return null;
      } else {
         BlockPos var10 = null;
         BezierConnection best = null;

         for (Entry<BlockPos, BezierConnection> e : be.getAnchorPeerCurvesView().entrySet()) {
            BlockPos peer = e.getKey();
            if (!peer.equals(excludePeer)) {
               BezierConnection c = e.getValue();
               if (c != null && (var10 == null || peer.asLong() < var10.asLong())) {
                  var10 = peer.immutable();
                  best = c;
               }
            }
         }

         if (best == null) {
            return null;
         } else {
            BezierConnection p = best.isPrimary() ? best : best.secondary();
            return CoasterBezierEndpointData.at(p, hub);
         }
      }
   }

   public static Vec3 anchorMountingPlaneReference(Level level, BlockPos anchorPos) {
      BlockState state = level.getBlockState(anchorPos);
      Direction facing = state.hasProperty(CoasterAnchorpointBlock.FACING) ? (Direction)state.getValue(CoasterAnchorpointBlock.FACING) : Direction.UP;
      Direction refDir = facing.getAxis() == Axis.Y ? facing.getClockWise(Axis.Y) : Direction.UP;
      Vec3 localUp = CoasterAnchorpointBlockEntity.localUp(level, anchorPos);
      Vec3 ref = Vec3.atLowerCornerOf(refDir.getNormal());
      ref = ref.subtract(localUp.scale(localUp.dot(ref)));
      if (ref.lengthSqr() < 1.0E-10) {
         refDir = facing.getAxis() == Axis.Y ? facing.getCounterClockWise(Axis.Y) : facing.getClockWise(Axis.Y);
         ref = Vec3.atLowerCornerOf(refDir.getNormal());
         ref = ref.subtract(localUp.scale(localUp.dot(ref)));
      }

      return ref.lengthSqr() < 1.0E-10 ? new Vec3(1.0, 0.0, 0.0) : ref.normalize();
   }

   public static Vec3 anchorFacingPerpendicularToAxis(Level level, BlockPos anchorPos, Vec3 axisUnit) {
      Vec3 ref = anchorMountingPlaneReference(level, anchorPos);
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
      Vec3 localUp = CoasterAnchorpointBlockEntity.localUp(level, anchorPos);
      Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axisIntoCurve.normalize();
      Vec3 n = localUp.subtract(axis.scale(axis.dot(localUp)));
      if (n.lengthSqr() < 1.0E-10) {
         Vec3 side = anchorFacingPerpendicularToAxis(level, anchorPos, axis);
         n = axis.cross(side);
         if (n.lengthSqr() < 1.0E-10) {
            n = side.cross(axis);
         }
      }

      return n.normalize();
   }

   private static Vec3 anchorEndpointNormal(Level level, BlockPos anchorPos, Vec3 axisIntoCurve) {
      Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axisIntoCurve.normalize();
      Vec3 n0 = anchorEndpointNormalGeometric(level, anchorPos, axis);
      return level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be
         ? CoasterBezierHandleEdit.jointBankFaceNormalAfterGeometric(level, anchorPos, axis, n0, (double)be.getJointBankRadians())
         : orthonormalToAxis(n0, axis);
   }

   public static BezierConnection curveWithReferenceNormalAtAnchorEndpoint(Level level, BezierConnection bc, BlockPos anchorPos, Vec3 referenceNormal) {
      Vec3 axis = ((BlockPos)bc.bePositions.getFirst()).equals(anchorPos) ? (Vec3)bc.axes.getFirst() : (Vec3)bc.axes.getSecond();
      return withFrameAtEndpoint(level, bc, anchorPos, axis, referenceNormal);
   }

   private static BezierConnection withFrameAtEndpoint(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newAxis, Vec3 newNormal) {
      Vec3 a1 = (Vec3)bc.axes.getFirst();
      Vec3 a2 = (Vec3)bc.axes.getSecond();
      Vec3 n1 = (Vec3)bc.normals.getFirst();
      Vec3 n2 = (Vec3)bc.normals.getSecond();
      if (((BlockPos)bc.bePositions.getFirst()).equals(endpoint)) {
         a1 = newAxis;
         n1 = orthonormalToAxis(newNormal, newAxis, level, endpoint);
      }

      if (((BlockPos)bc.bePositions.getSecond()).equals(endpoint)) {
         a2 = newAxis;
         n2 = orthonormalToAxis(newNormal, newAxis, level, endpoint);
      }

      return new BezierConnection(
         Couple.create((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond()),
         Couple.create((Vec3)bc.starts.getFirst(), (Vec3)bc.starts.getSecond()),
         Couple.create(a1, a2),
         Couple.create(n1, n2),
         bc.isPrimary(),
         bc.hasGirder,
         bc.getMaterial()
      );
   }

   private static BezierConnection withStartAndFrameAtEndpoint(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newStart, Vec3 newAxis, Vec3 newNormal) {
      Vec3 s1 = (Vec3)bc.starts.getFirst();
      Vec3 s2 = (Vec3)bc.starts.getSecond();
      Vec3 a1 = (Vec3)bc.axes.getFirst();
      Vec3 a2 = (Vec3)bc.axes.getSecond();
      Vec3 n1 = (Vec3)bc.normals.getFirst();
      Vec3 n2 = (Vec3)bc.normals.getSecond();
      if (((BlockPos)bc.bePositions.getFirst()).equals(endpoint)) {
         s1 = newStart;
         a1 = newAxis;
         n1 = orthonormalToAxis(newNormal, newAxis, level, endpoint);
      }

      if (((BlockPos)bc.bePositions.getSecond()).equals(endpoint)) {
         s2 = newStart;
         a2 = newAxis;
         n2 = orthonormalToAxis(newNormal, newAxis, level, endpoint);
      }

      return new BezierConnection(
         Couple.create((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond()),
         Couple.create(s1, s2),
         Couple.create(a1, a2),
         Couple.create(n1, n2),
         bc.isPrimary(),
         bc.hasGirder,
         bc.getMaterial()
      );
   }

   private static Vec3 orthonormalToAxis(Vec3 normal, Vec3 axis) {
      return orthonormalToAxis(normal, axis, null, null);
   }

   private static Vec3 orthonormalToAxis(Vec3 normal, Vec3 axis, @Nullable Level level, @Nullable BlockPos anchorPos) {
      Vec3 a = axis.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axis.normalize();
      Vec3 n = normal.subtract(a.scale(a.dot(normal)));
      if (n.lengthSqr() < 1.0E-10) {
         if (level != null && anchorPos != null && level.getBlockState(anchorPos).getBlock() instanceof CoasterAnchorpointBlock) {
            n = anchorEndpointNormalGeometric(level, anchorPos, a);
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
      return orthonormalToAxis(referenceNormal, axisIntoCurve);
   }

   public static Vec3 orthonormalFaceNormalForAxis(Level level, BlockPos anchorPos, Vec3 referenceNormal, Vec3 axisIntoCurve) {
      return orthonormalToAxis(referenceNormal, axisIntoCurve, level, anchorPos);
   }

   public static Component placementErrorMessage(String translationKey) {
      return Component.translatable(translationKey).withStyle(ChatFormatting.RED);
   }

   @OnlyIn(Dist.CLIENT)
   private static void clearNeighborCurvePreviewOutlines() {
      for (int n = 0; n < lastNeighborLineCounts.length; n++) {
         clearCurvePreviewOutlinesForKey("curve_neighbor" + n, lastNeighborLineCounts[n]);
      }

      lastNeighborLineCounts = new int[0];
   }

   @OnlyIn(Dist.CLIENT)
   private static void clearCoasterTrackPlacementPreviewOutlines() {
      Outliner o = Outliner.getInstance();

      for (int id = 1; id <= 4; id++) {
         o.remove(Pair.of("start", id));
      }

      for (int k = 2; k <= 4096; k++) {
         o.remove(Pair.of("curve", k));
      }

      lastLineCount = 0;
      clearNeighborCurvePreviewOutlines();
   }

   @OnlyIn(Dist.CLIENT)
   private static void drawAnchorSelectionOutlines(Minecraft mc, ItemStack stack, int outlineColor, @Nullable BlockPos hoverOverride) {
      BlockPos first = readAnchorFirstSelection(stack);
      if (first == null) {
         Outliner.getInstance().remove("anchor_first");
         Outliner.getInstance().remove("anchor_hover");
      } else {
         Level level = mc.level;
         showAnchorBlockOutline(level, first, "anchor_first", outlineColor);
         BlockPos hover = hoverOverride;
         if (hoverOverride == null) {
            HitResult hit = mc.hitResult;
            if (hit instanceof BlockHitResult bhr && hit.getType() == Type.BLOCK) {
               hover = bhr.getBlockPos();
            }
         }

         if (hover != null && !first.equals(hover) && level.getBlockState(hover).getBlock() instanceof CoasterAnchorpointBlock) {
            showAnchorBlockOutline(level, hover, "anchor_hover", outlineColor);
         } else {
            Outliner.getInstance().remove("anchor_hover");
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Nullable
   private static BlockPos resolvePlacementLookBlock(Minecraft mc, ItemStack stack) {
      HitResult hitResult = mc.hitResult;
      if (hitResult instanceof BlockHitResult && hitResult.getType() == Type.BLOCK) {
         return ((BlockHitResult)hitResult).getBlockPos();
      } else {
         return hasAnchorFirstSelection(stack) ? CoasterTrackOpenEndpointOutlineClient.resolveClosestOpenAnchor(mc) : null;
      }
   }

   private static void displayPlacementActionBar(Player player, CoasterTrackPlacement.PlacementInfo info) {
      if (info.message != null) {
         if (info.valid) {
            player.displayClientMessage(CreateLang.translateDirect("track.valid_connection", new Object[0]).withStyle(ChatFormatting.WHITE), true);
         } else {
            MutableComponent line = info.modTranslationKey ? Component.translatable(info.message) : CreateLang.translateDirect(info.message, new Object[0]);
            player.displayClientMessage(line.withStyle(info.message.equals("track.second_point") ? ChatFormatting.WHITE : ChatFormatting.RED), true);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void showAnchorBlockOutline(Level level, BlockPos pos, String outlinerKey, int color) {
      BlockState state = level.getBlockState(pos);
      if (!(state.getBlock() instanceof CoasterAnchorpointBlock)) {
         Outliner.getInstance().remove(outlinerKey);
      } else {
         VoxelShape shape = state.getShape(level, pos);
         if (shape.isEmpty()) {
            Outliner.getInstance().remove(outlinerKey);
         } else {
            Outliner.getInstance().showAABB(outlinerKey, shape.bounds().move(pos)).colored(color).lineWidth(0.0625F);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void showTargetBlockOutline(Level level, BlockPos pos, String outlinerKey, int color) {
      BlockState state = level.getBlockState(pos);
      VoxelShape shape = state.getShape(level, pos);
      AABB box = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
      Outliner.getInstance().showAABB(outlinerKey, box).colored(color).lineWidth(0.0625F);
   }

   @OnlyIn(Dist.CLIENT)
   private static boolean drawInvalidHoverPreview(Minecraft mc, LocalPlayer player, ItemStack stack, BlockPos lookBlock, int restoreWarmup) {
      if (!hasAnchorFirstSelection(stack)) {
         return false;
      } else if (lookBlock == null) {
         return false;
      } else {
         Level level = player.level();
         if (level.getBlockState(lookBlock).getBlock() instanceof CoasterAnchorpointBlock) {
            return false;
         } else {
            BlockPos anchorFirst = readAnchorFirstSelection(stack);
            if (anchorFirst != null && !anchorFirst.equals(lookBlock)) {
               BlockPos virtualSecond = lookBlock.above();
               if (virtualSecond.equals(anchorFirst)) {
                  return false;
               } else {
                  updateTrackCostOverlayForPreviewEndpoints(mc, level, player, stack, anchorFirst, virtualSecond);
                  clearCoasterTrackPlacementPreviewOutlines();
                  Outliner.getInstance().remove("anchor_hover");
                  showTargetBlockOutline(level, lookBlock, "invalid_target", 15359019);
                  extraTipWarmup = restoreWarmup;
                  extraTipWarmup = 0;
                  player.displayClientMessage(Component.translatable("simulatedcoasters.track.must_attach_to_anchors").withStyle(ChatFormatting.RED), true);
                  if (shouldSkipAnchorPreviewDraw(level, anchorFirst, virtualSecond)) {
                     return true;
                  } else {
                     TrackMaterial material = TrackMaterial.fromItem(stack.getItem());
                     CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult placement = CoasterAnchorBezierOptimizer.buildAnchorToVirtualPeerPlacement(
                        level, anchorFirst, virtualSecond, material, false, true
                     );
                     if (placement == null) {
                        return true;
                     } else {
                        BezierConnection bc = placement.primary();
                        if (shouldSkipHeavyCurvePreview(bc)) {
                           return true;
                        } else {
                           CoasterTrackPlacement.PlacementInfo info = new CoasterTrackPlacement.PlacementInfo(material);
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
                           animation.chase(0.0, 0.25, Chaser.EXP);
                           animation.tickChaser();
                           drawClientPlacementPreview(level, info, 15359019);
                           return true;
                        }
                     }
                  }
               }
            } else {
               return false;
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static boolean shouldSkipAnchorPreviewDraw(Level level, BlockPos anchorA, BlockPos anchorB) {
      return anchorConnectionTooFar(level, anchorA, anchorB) ? true : !CoasterAnchorClientSpace.anchorHostsWithinPreviewSpan(level, anchorA, anchorB);
   }

   @OnlyIn(Dist.CLIENT)
   private static boolean shouldSkipHeavyCurvePreview(@Nullable BezierConnection bc) {
      if (bc == null) {
         return true;
      } else {
         double length = bc.getLength();
         if (Double.isFinite(length) && !(length <= 0.0)) {
            double maxLen = (double)CoasterTrackGauge.maxCoasterCurveEndpointSpanBlocks() * 1.5;
            return length > maxLen ? true : !AnchorPeerCurveOutlineSupport.hasSaneOutlineBounds(bc);
         } else {
            return true;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void drawClientPlacementPreview(Level level, CoasterTrackPlacement.PlacementInfo info, int railColor) {
      if (info.end1 != null && info.axis1 != null && info.end2 != null && info.axis2 != null && info.normal1 != null && info.normal2 != null) {
         if (info.pos1 != null && info.pos2 != null && shouldSkipAnchorPreviewDraw(level, info.pos1, info.pos2)) {
            clearCurvePreviewOutlines(lastLineCount);
            lastLineCount = 0;
         } else if (shouldSkipHeavyCurvePreview(info.curve)) {
            clearCurvePreviewOutlines(lastLineCount);
            lastLineCount = 0;
         } else {
            BezierConnection bc = info.curve;
            double previewLift = (double)CoasterTrackGauge.coasterPreviewSpineVerticalBumpBlocks();
            double previewLiftAlongFaceUp = (double)CoasterTrackGauge.coasterPreviewCurveLiftAlongFaceUp();
            double previewLiftWorldY = (double)(
               CoasterTrackGauge.coasterPreviewRailWorldYOffsetBlocks() + previewExtraWorldYForAnchors(bc, info.pos1, info.pos2)
            );
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

            Vec3 a1 = previewAxis1.normalize();
            Vec3 n1 = BezierRailSpread.faceCrossTangentOrFallback(previewNormal1, a1).normalize().scale((double)previewHalf);
            Vec3 lift1 = (previewNormal1.lengthSqr() < 1.0E-10 ? Vec3.ZERO : previewNormal1.normalize().scale(previewLiftAlongFaceUp))
               .add(0.0, previewLiftWorldY, 0.0);
            Vec3 o1 = a1.scale(0.125);
            Vec3 ex1 = a1.scale((double)(info.end1Extent - (bc == null && info.end1Extent > 0 ? 2 : 0)) * previewAxis1.length());
            line(1, previewEnd1.add(n1).add(lift1), o1, ex1, railColor);
            line(2, previewEnd1.subtract(n1).add(lift1), o1, ex1, railColor);
            Vec3 a2 = previewAxis2.normalize();
            Vec3 n2 = BezierRailSpread.faceCrossTangentOrFallback(previewNormal2, a2).normalize().scale((double)previewHalf);
            Vec3 lift2 = (previewNormal2.lengthSqr() < 1.0E-10 ? Vec3.ZERO : previewNormal2.normalize().scale(previewLiftAlongFaceUp))
               .add(0.0, previewLiftWorldY, 0.0);
            Vec3 o2 = a2.scale(0.125);
            Vec3 ex2 = a2.scale((double)info.end2Extent * previewAxis2.length());
            line(3, previewEnd2.add(n2).add(lift2), o2, ex2, railColor);
            line(4, previewEnd2.subtract(n2).add(lift2), o2, ex2, railColor);
            if (bc != null) {
               lastLineCount = drawCurvePreview(level, bc, "curve", railColor, previewLift, lastLineCount);
            }

            if (info.placementSmoothingNeighbors != null && !info.placementSmoothingNeighbors.isEmpty()) {
               drawNeighborSmoothPreviewCurves(level, info.placementSmoothingNeighbors, railColor);
            } else {
               clearNeighborCurvePreviewOutlines();
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @Nullable
   private static ItemStack findCoasterTrackStack(LocalPlayer player) {
      ItemStack main = player.getMainHandItem();
      if (main.getItem() instanceof CoasterTrackItem) {
         return main;
      } else {
         ItemStack off = player.getOffhandItem();
         return off.getItem() instanceof CoasterTrackItem ? off : null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void clientTick() {
      CoasterTrackPlacementSmoothHud.setHintActive(false);
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null && mc.level != null) {
         ItemStack coasterStack = findCoasterTrackStack(player);
         int anchorOutlineColor = 1940687;
         ItemStack stack = findCoasterTrackStack(player);
         if (stack == null) {
            CoasterTrackPlacementOverlayRenderer.clear();
            clearCoasterTrackPlacementPreviewOutlines();
         } else if (!stack.hasFoil() && !hasAnchorFirstSelection(stack)) {
            CoasterTrackPlacementOverlayRenderer.clear();
            clearCoasterTrackPlacementPreviewOutlines();
            Outliner.getInstance().remove("invalid_target");
         } else {
            HitResult hitResult = mc.hitResult;
            int restoreWarmup = extraTipWarmup;
            extraTipWarmup = 0;
            BlockPos lookBlock = resolvePlacementLookBlock(mc, stack);
            if (lookBlock == null) {
               CoasterTrackPlacementOverlayRenderer.clear();
               clearCoasterTrackPlacementPreviewOutlines();
               if (coasterStack != null && hasAnchorFirstSelection(coasterStack)) {
                  drawAnchorSelectionOutlines(mc, coasterStack, anchorOutlineColor, null);
               }
            } else {
               try {
                  if (hitResult instanceof BlockHitResult
                     && hitResult.getType() == Type.BLOCK
                     && drawInvalidHoverPreview(mc, player, stack, lookBlock, restoreWarmup)) {
                     if (coasterStack != null && hasAnchorFirstSelection(coasterStack)) {
                        drawAnchorSelectionOutlines(mc, coasterStack, anchorOutlineColor, lookBlock);
                     }

                     return;
                  }

                  Outliner.getInstance().remove("invalid_target");
                  Level level = player.level();
                  BlockState hitState = level.getBlockState(lookBlock);
                  clearCoasterTrackPlacementPreviewOutlines();
                  extraTipWarmup = restoreWarmup;
                  boolean maxTurns = true;
                  CoasterTrackPlacement.PlacementInfo info = tryConnect(level, player, lookBlock, hitState, stack, false, maxTurns, true);
                  applyCoasterAnchorsOnlyPreviewInvalidMarks(level, info, lookBlock, hitState, stack);
                  BlockPos anchorFirst = readAnchorFirstSelection(stack);
                  if (anchorFirst != null && !anchorFirst.equals(lookBlock) && hitState.getBlock() instanceof CoasterAnchorpointBlock && !info.valid) {
                     anchorOutlineColor = 15359019;
                  }

                  if (extraTipWarmup < 20) {
                     extraTipWarmup++;
                  }

                  if (!info.valid || !hoveringMaxed && (info.end1Extent == 0 || info.end2Extent == 0)) {
                     extraTipWarmup = 0;
                  }

                  CoasterTrackPlacementOverlayRenderer.updateFromPlacementPreview(mc, info.requiredTracks, info.hasRequiredTracks, info.valid);
                  if (info.valid) {
                     player.displayClientMessage(CreateLang.translateDirect("track.valid_connection", new Object[0]).withStyle(ChatFormatting.WHITE), true);
                     if (anchorFirst != null
                        && hitState.getBlock() instanceof CoasterAnchorpointBlock
                        && !anchorFirst.equals(lookBlock)
                        && placementHasNeighborsToCoSmooth(level, anchorFirst, lookBlock)) {
                        CoasterTrackPlacementSmoothHud.setHintActive(true);
                     }
                  } else if (info.message != null) {
                     MutableComponent line = info.modTranslationKey
                        ? Component.translatable(info.message)
                        : CreateLang.translateDirect(info.message, new Object[0]);
                     player.displayClientMessage(line.withStyle(info.message.equals("track.second_point") ? ChatFormatting.WHITE : ChatFormatting.RED), true);
                  }

                  boolean skipCurvePreview = anchorFirst != null
                     && hitState.getBlock() instanceof CoasterAnchorpointBlock
                     && !anchorFirst.equals(lookBlock)
                     && shouldSkipAnchorPreviewDraw(level, anchorFirst, lookBlock);
                  if (!skipCurvePreview
                     && info.end1 != null
                     && info.axis1 != null
                     && info.end2 != null
                     && info.axis2 != null
                     && info.normal1 != null
                     && info.normal2 != null) {
                     animation.chase(info.valid ? 1.0 : 0.0, 0.25, Chaser.EXP);
                     animation.tickChaser();
                     if (!info.valid) {
                        info.end1Extent = 0;
                        info.end2Extent = 0;
                     }

                     int color = Color.mixColors(15359019, 1940687, animation.getValue());
                     drawClientPlacementPreview(level, info, color);
                  }
               } catch (RuntimeException var16) {
                  CoasterTrackPlacementOverlayRenderer.clear();
                  clearCoasterTrackPlacementPreviewOutlines();
                  SimulatedCoasters.LOGGER.warn("Coaster track placement preview failed (skipped this tick)", var16);
               }

               if (coasterStack != null && hasAnchorFirstSelection(coasterStack)) {
                  drawAnchorSelectionOutlines(mc, coasterStack, anchorOutlineColor, lookBlock);
               } else {
                  Outliner.getInstance().remove("anchor_first");
                  Outliner.getInstance().remove("anchor_hover");
               }
            }
         }
      } else {
         CoasterTrackPlacementOverlayRenderer.clear();
         clearCoasterTrackPlacementPreviewOutlines();
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void clearCurvePreviewOutlines(int prevSegmentCount) {
      if (prevSegmentCount > 0) {
         for (int i = 1; i <= prevSegmentCount; i++) {
            Outliner.getInstance().remove(Pair.of("curve", i * 2));
            Outliner.getInstance().remove(Pair.of("curve", i * 2 + 1));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static int drawCurvePreview(Level level, BezierConnection bc, String key, int railcolor, double previewLift, int prevCount) {
      float s = animation.getValue() * 7.0F / 8.0F + 0.125F;
      float lw = animation.getValue() * 1.0F / 16.0F + 0.0625F;
      return drawCurvePreviewInner(level, bc, key, railcolor, previewLift, prevCount, s, lw, false);
   }

   @OnlyIn(Dist.CLIENT)
   public static int drawCoasterCurveOutlinePreview(BezierConnection bc, String outlinerKey, int railColor, double previewLift, int prevSegmentCount) {
      Minecraft mc = Minecraft.getInstance();
      Level level = mc != null ? mc.level : null;
      return level == null ? prevSegmentCount : drawCurvePreviewInner(level, bc, outlinerKey, railColor, previewLift, prevSegmentCount, 1.0F, 0.125F, true);
   }

   @OnlyIn(Dist.CLIENT)
   private static void clearCurvePreviewOutlinesForKey(String outlinerKey, int prevSegmentCount) {
      if (prevSegmentCount > 0) {
         for (int i = 1; i <= prevSegmentCount; i++) {
            Outliner.getInstance().remove(Pair.of(outlinerKey, i * 2));
            Outliner.getInstance().remove(Pair.of(outlinerKey, i * 2 + 1));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void clearCoasterCurveOutlinePreview(String outlinerKey, int prevSegmentCount) {
      clearCurvePreviewOutlinesForKey(outlinerKey, prevSegmentCount);
   }

   @OnlyIn(Dist.CLIENT)
   private static float previewExtraWorldYForAnchors(@Nullable BezierConnection bc, @Nullable BlockPos pos1, @Nullable BlockPos pos2) {
      Level level = BezierBankBlendLevel.tryGetClientLevel();
      if (level == null) {
         return 0.0F;
      } else if (bc != null) {
         for (BlockPos p : bc.bePositions) {
            if (p != null && anchorFacingIsNotUp(level, p)) {
               return 0.0F;
            }
         }

         return 0.0F;
      } else if (pos1 != null && anchorFacingIsNotUp(level, pos1)) {
         return 0.0F;
      } else {
         return pos2 != null && anchorFacingIsNotUp(level, pos2) ? 0.0F : 0.0F;
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static boolean anchorFacingIsNotUp(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return !(state.getBlock() instanceof CoasterAnchorpointBlock) ? false : state.getValue(CoasterAnchorpointBlock.FACING) != Direction.UP;
   }

   @OnlyIn(Dist.CLIENT)
   private static Vec3 iteratorSpineSampleToWorld(BezierConnection bc, Vec3 iteratorPosition) {
      return iteratorPosition.add(Vec3.atLowerCornerOf((Vec3i)bc.bePositions.getFirst())).add(0.0, -0.1875, 0.0);
   }

   @OnlyIn(Dist.CLIENT)
   private static int drawCurvePreviewInner(
      Level level,
      BezierConnection bc,
      String key,
      int railcolor,
      double previewLift,
      int prevCount,
      float smoothBlend,
      float lineWidth,
      boolean drawOpenEndExtensions
   ) {
      BlockPos p1 = (BlockPos)bc.bePositions.getFirst();
      BlockPos p2 = (BlockPos)bc.bePositions.getSecond();
      if (shouldSkipAnchorPreviewDraw(level, p1, p2)) {
         clearCurvePreviewOutlinesForKey(key, prevCount);
         return 0;
      } else if (shouldSkipHeavyCurvePreview(bc)) {
         clearCurvePreviewOutlinesForKey(key, prevCount);
         return 0;
      } else {
         int actualSegCount = bc.getSegmentCount();
         boolean useIteratorParameterization = actualSegCount >= 16;
         int segCount = useIteratorParameterization ? actualSegCount : 16;
         Vec3 end1 = (Vec3)bc.starts.getFirst();
         Vec3 end2 = (Vec3)bc.starts.getSecond();
         Vec3 finish1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
         Vec3 finish2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
         double iteratorWorldY = 0.1875;
         double liftAlongFaceUp = previewLift - iteratorWorldY;
         double railHalf = (double)CoasterTrackGauge.coasterPreviewRailHalfGauge();
         float extraWorldY = previewExtraWorldYForAnchors(bc, null, null);
         Vec3 worldYOffset = new Vec3(0.0, (double)(CoasterTrackGauge.coasterPreviewRailWorldYOffsetBlocks() + extraWorldY), 0.0);
         BlockPos sublevelHost = previewSublevelHost(level, p1, p2);
         float partialTick = level.isClientSide() ? AnimationTickHolder.getPartialTicks(level) : 0.0F;
         Vec3[] spine = new Vec3[segCount + 1];
         Vec3[] lateralUnit = new Vec3[segCount + 1];
         Vec3[] tangentUnit = new Vec3[segCount + 1];
         Level previewLevel = BezierBankBlendLevel.tryGetClientLevel();
         boolean filledFromIterator = false;
         if (useIteratorParameterization) {
            Vec3 prevLat = null;
            int idx = 0;

            for (Segment segment : bc) {
               float segT = bc.getSegmentT(segment.index);
               Vec3 lat = CoasterBezierRailFrames.lateralAt(bc, segT, segment.derivative, previewLevel);
               if (prevLat != null && lat.dot(prevLat) < 0.0) {
                  lat = lat.scale(-1.0);
               }

               Vec3 tan = segment.derivative;
               if (tan.lengthSqr() < 1.0E-14) {
                  tan = CoasterBezierRailFrames.unitTangentAt(bc, segT);
               } else {
                  tan = tan.normalize();
               }

               spine[idx] = iteratorSpineSampleToWorld(bc, segment.position);
               lateralUnit[idx] = lat;
               tangentUnit[idx] = tan;
               prevLat = lat;
               if (++idx > segCount + 1) {
                  break;
               }
            }

            filledFromIterator = idx == segCount + 1;
         }

         if (!filledFromIterator) {
            Vec3 prevLat = null;

            for (int i = 0; i <= segCount; i++) {
               float t = useIteratorParameterization ? bc.getSegmentT(i) : (float)i / (float)segCount;
               spine[i] = VecHelper.bezier(end1, end2, finish1, finish2, t);
               Vec3 tanx = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t);
               if (tanx.lengthSqr() < 1.0E-12) {
                  tanx = CoasterBezierRailFrames.unitTangentAt(bc, t);
               } else {
                  tanx = tanx.normalize();
               }

               Vec3 latx = CoasterBezierRailFrames.lateralAt(bc, t, tanx, previewLevel);
               if (prevLat != null && latx.dot(prevLat) < 0.0) {
                  latx = latx.scale(-1.0);
               }

               lateralUnit[i] = latx;
               tangentUnit[i] = tanx;
               prevLat = latx;
            }
         }

         Vec3 prevRail1 = spine[0].add(lateralUnit[0].scale(railHalf));
         Vec3 prevRail2 = spine[0].subtract(lateralUnit[0].scale(railHalf));

         for (int i = 1; i <= segCount; i++) {
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
            prevW1 = previewPointToRenderWorld(level, sublevelHost, prevW1, partialTick);
            currW1 = previewPointToRenderWorld(level, sublevelHost, currW1, partialTick);
            prevW2 = previewPointToRenderWorld(level, sublevelHost, prevW2, partialTick);
            currW2 = previewPointToRenderWorld(level, sublevelHost, currW2, partialTick);
            Vec3 middle1 = prevW1.add(currW1).scale(0.5);
            Vec3 middle2 = prevW2.add(currW2).scale(0.5);
            Outliner.getInstance()
               .showLine(Pair.of(key, i * 2), VecHelper.lerp(smoothBlend, middle1, prevW1), VecHelper.lerp(smoothBlend, middle1, currW1))
               .colored(railcolor)
               .disableLineNormals()
               .lineWidth(lineWidth);
            Outliner.getInstance()
               .showLine(Pair.of(key, i * 2 + 1), VecHelper.lerp(smoothBlend, middle2, prevW2), VecHelper.lerp(smoothBlend, middle2, currW2))
               .colored(railcolor)
               .disableLineNormals()
               .lineWidth(lineWidth);
            prevRail1 = currRail1;
            prevRail2 = currRail2;
         }

         int outlineSegments = segCount;
         if (drawOpenEndExtensions) {
            if (CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, true)) {
               outlineSegments = segCount + 1;
               float ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, true);
               drawOpenEndExtensionOutlineSegment(
                  level,
                  bc,
                  key,
                  outlineSegments,
                  railcolor,
                  smoothBlend,
                  lineWidth,
                  spine[0],
                  lateralUnit[0],
                  tangentUnit[0],
                  true,
                  ext,
                  railHalf,
                  liftAlongFaceUp,
                  worldYOffset,
                  sublevelHost,
                  partialTick
               );
            }

            if (CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, false)) {
               outlineSegments++;
               float ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, false);
               drawOpenEndExtensionOutlineSegment(
                  level,
                  bc,
                  key,
                  outlineSegments,
                  railcolor,
                  smoothBlend,
                  lineWidth,
                  spine[segCount],
                  lateralUnit[segCount],
                  tangentUnit[segCount],
                  false,
                  ext,
                  railHalf,
                  liftAlongFaceUp,
                  worldYOffset,
                  sublevelHost,
                  partialTick
               );
            }
         }

         for (int i = outlineSegments + 1; i <= prevCount; i++) {
            Outliner.getInstance().remove(Pair.of(key, i * 2));
            Outliner.getInstance().remove(Pair.of(key, i * 2 + 1));
         }

         return outlineSegments;
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void drawOpenEndExtensionOutlineSegment(
      Level level,
      BezierConnection bc,
      String key,
      int outlinePairIndex,
      int railColor,
      float smoothBlend,
      float lineWidth,
      Vec3 junctionSpine,
      Vec3 lat,
      Vec3 tan,
      boolean atStart,
      float extLen,
      double railHalf,
      double liftAlongFaceUp,
      Vec3 worldYOffset,
      @Nullable BlockPos sublevelHost,
      float partialTick
   ) {
      if (!(extLen < 1.0E-6F)) {
         Vec3 latN = lat.lengthSqr() < 1.0E-14 ? new Vec3(1.0, 0.0, 0.0) : lat.normalize();
         Vec3 tanN = tan.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : tan.normalize();
         Vec3 junctionRail1 = junctionSpine.add(latN.scale(railHalf));
         Vec3 junctionRail2 = junctionSpine.subtract(latN.scale(railHalf));
         Vec3 tipRail1 = atStart ? junctionRail1.subtract(tanN.scale((double)extLen)) : junctionRail1.add(tanN.scale((double)extLen));
         Vec3 tipRail2 = atStart ? junctionRail2.subtract(tanN.scale((double)extLen)) : junctionRail2.add(tanN.scale((double)extLen));
         if (atStart) {
            drawOutlineRailLine(
               level,
               key,
               outlinePairIndex,
               true,
               railColor,
               smoothBlend,
               lineWidth,
               tipRail1,
               junctionRail1,
               latN,
               liftAlongFaceUp,
               worldYOffset,
               sublevelHost,
               partialTick
            );
            drawOutlineRailLine(
               level,
               key,
               outlinePairIndex,
               false,
               railColor,
               smoothBlend,
               lineWidth,
               tipRail2,
               junctionRail2,
               latN,
               liftAlongFaceUp,
               worldYOffset,
               sublevelHost,
               partialTick
            );
         } else {
            drawOutlineRailLine(
               level,
               key,
               outlinePairIndex,
               true,
               railColor,
               smoothBlend,
               lineWidth,
               junctionRail1,
               tipRail1,
               latN,
               liftAlongFaceUp,
               worldYOffset,
               sublevelHost,
               partialTick
            );
            drawOutlineRailLine(
               level,
               key,
               outlinePairIndex,
               false,
               railColor,
               smoothBlend,
               lineWidth,
               junctionRail2,
               tipRail2,
               latN,
               liftAlongFaceUp,
               worldYOffset,
               sublevelHost,
               partialTick
            );
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void drawOutlineRailLine(
      Level level,
      String key,
      int outlinePairIndex,
      boolean firstRail,
      int railColor,
      float smoothBlend,
      float lineWidth,
      Vec3 fromRail,
      Vec3 toRail,
      Vec3 latI,
      double liftAlongFaceUp,
      Vec3 worldYOffset,
      @Nullable BlockPos sublevelHost,
      float partialTick
   ) {
      Vec3 diff = toRail.subtract(fromRail);
      Vec3 chordFaceUpRaw = diff.cross(latI);
      Vec3 chordFaceUp = chordFaceUpRaw.lengthSqr() < 1.0E-12 ? Vec3.ZERO : chordFaceUpRaw.normalize();
      Vec3 lift = chordFaceUp.scale(liftAlongFaceUp).add(worldYOffset);
      Vec3 fromW = previewPointToRenderWorld(level, sublevelHost, fromRail.add(lift), partialTick);
      Vec3 toW = previewPointToRenderWorld(level, sublevelHost, toRail.add(lift), partialTick);
      Vec3 middle = fromW.add(toW).scale(0.5);
      int lineId = outlinePairIndex * 2 + (firstRail ? 0 : 1);
      Outliner.getInstance()
         .showLine(Pair.of(key, lineId), VecHelper.lerp(smoothBlend, middle, fromW), VecHelper.lerp(smoothBlend, middle, toW))
         .colored(railColor)
         .disableLineNormals()
         .lineWidth(lineWidth);
   }

   @OnlyIn(Dist.CLIENT)
   @Nullable
   private static BlockPos previewSublevelHost(Level level, BlockPos p1, BlockPos p2) {
      if (Sable.HELPER.getContaining(level, p1) != null) {
         return p1;
      } else {
         return Sable.HELPER.getContaining(level, p2) != null ? p2 : null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static Vec3 previewPointToRenderWorld(Level level, @Nullable BlockPos sublevelHost, Vec3 plotLocal, float partialTick) {
      return sublevelHost == null ? plotLocal : CoasterAnchorClientSpace.toRenderWorld(level, sublevelHost, plotLocal, partialTick);
   }

   @OnlyIn(Dist.CLIENT)
   private static void line(int id, Vec3 v1, Vec3 o1, Vec3 ex) {
      int color = Color.mixColors(15359019, 1940687, animation.getValue());
      line(id, v1, o1, ex, color);
   }

   @OnlyIn(Dist.CLIENT)
   private static void line(int id, Vec3 v1, Vec3 o1, Vec3 ex, int color) {
      Outliner.getInstance().showLine(Pair.of("start", id), v1.subtract(o1), v1.add(ex)).lineWidth(0.125F).disableLineNormals().colored(color);
   }

   private static boolean isCreateTrainTrack(Block block) {
      return block instanceof TrackBlock;
   }

   private static CoasterTrackPlacement.PlacementInfo denyCoasterAnchorsOnlyPlacement(CoasterTrackPlacement.PlacementInfo info) {
      return info.withModTranslationKey("simulatedcoasters.track.must_attach_to_anchors").tooJumbly();
   }

   private static boolean coasterAnchorsOnlyPlacementForbidden(@Nullable BlockPos anchorFirst, BlockState state2) {
      boolean secondIsAnchor = state2.getBlock() instanceof CoasterAnchorpointBlock;
      return !secondIsAnchor || anchorFirst == null;
   }

   public static void applyCoasterAnchorsOnlyPreviewInvalidMarks(
      Level level, CoasterTrackPlacement.PlacementInfo info, BlockPos pos2, BlockState state2, ItemStack stack
   ) {
      if (level.isClientSide()) {
         if (stack.getItem() instanceof CoasterTrackItem) {
            if (info.trackMaterial.id.equals(CoasterTrackMaterials.COASTER.id)) {
               BlockPos anchorFirst = readAnchorFirstSelection(stack);
               if (coasterAnchorsOnlyPlacementForbidden(anchorFirst, state2)) {
                  if (info.end1 != null && info.axis1 != null && info.end2 != null && info.axis2 != null && info.normal1 != null && info.normal2 != null) {
                     info.valid = false;
                     info.message = null;
                  }
               }
            }
         }
      }
   }

   private static CoasterTrackPlacement.PlacementInfo denyCoasterCrossPreview(
      Level level, BlockPos pos2, ItemStack stack, boolean maximiseTurn, int lookAngle, boolean suppressNeighborSmooth
   ) {
      CoasterTrackPlacement.PlacementInfo bad = new CoasterTrackPlacement.PlacementInfo(CoasterTrackMaterials.COASTER).tooJumbly();
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
      return validateEditedCoasterCurve(level, bc, null);
   }

   public static boolean validateEditedCoasterCurve(Level level, BezierConnection bc, @Nullable List<BezierConnection> previewBatch) {
      return bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
   }

   public static boolean validateCoasterCurveForPlacement(Level level, BezierConnection bc, @Nullable List<BezierConnection> previewBatch) {
      return validateEditedCoasterCurve(level, bc, previewBatch);
   }

   public static BezierConnection curveWithAxisAtEndpointForEdit(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newAxisIntoCurve) {
      return withAxisAtEndpoint(level, bc, endpoint, newAxisIntoCurve, false);
   }

   public static BezierConnection curveWithAxisAtEndpointPreservingPeerForJoin(Level level, BezierConnection bc, BlockPos endpoint, Vec3 newAxisIntoCurve) {
      return withAxisAtEndpoint(level, bc, endpoint, newAxisIntoCurve, true);
   }

   public static BezierConnection curveWithStartAtAnchorEndpoint(Level level, BezierConnection bc, BlockPos anchorPos, Vec3 newStart) {
      if (((BlockPos)bc.bePositions.getFirst()).equals(anchorPos)) {
         return withStartAndFrameAtEndpoint(level, bc, anchorPos, newStart, (Vec3)bc.axes.getFirst(), (Vec3)bc.normals.getFirst());
      } else {
         return ((BlockPos)bc.bePositions.getSecond()).equals(anchorPos)
            ? withStartAndFrameAtEndpoint(level, bc, anchorPos, newStart, (Vec3)bc.axes.getSecond(), (Vec3)bc.normals.getSecond())
            : bc;
      }
   }

   public static BezierConnection curveWithStartAndAxisAtEndpointForEdit(
      Level level, BezierConnection bc, BlockPos endpoint, Vec3 newStart, Vec3 newAxisIntoCurve
   ) {
      if (bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id) && newAxisIntoCurve.lengthSqr() > 1.0E-12) {
         newAxisIntoCurve = newAxisIntoCurve.normalize();
      }

      Vec3 normal = anchorEndpointNormal(level, endpoint, newAxisIntoCurve);
      return withStartAndFrameAtEndpoint(level, bc, endpoint, newStart, newAxisIntoCurve, normal);
   }

   public static BezierConnection curveWithStartAndReferenceNormalAtAnchorEndpoint(
      Level level, BezierConnection bc, BlockPos anchorPos, Vec3 newStart, Vec3 referenceNormal
   ) {
      Vec3 axis = ((BlockPos)bc.bePositions.getFirst()).equals(anchorPos) ? (Vec3)bc.axes.getFirst() : (Vec3)bc.axes.getSecond();
      return withStartAndFrameAtEndpoint(level, bc, anchorPos, newStart, axis, referenceNormal);
   }

   private static record CoasterSnapGeometryBackup(
      BlockPos pos1,
      BlockPos pos2,
      BlockState state1,
      BlockState state2,
      Vec3 axis1,
      Vec3 axis2,
      Vec3 normedAxis1,
      Vec3 normedAxis2,
      Vec3 end1,
      Vec3 end2,
      Vec3 normal1,
      Vec3 normal2,
      boolean skipCurve,
      boolean parallel,
      double[] intersect,
      double dist,
      int end1Extent,
      int end2Extent
   ) {
      static CoasterTrackPlacement.CoasterSnapGeometryBackup capture(
         BlockPos pos1,
         BlockPos pos2,
         BlockState state1,
         BlockState state2,
         Vec3 axis1,
         Vec3 axis2,
         Vec3 normedAxis1,
         Vec3 normedAxis2,
         Vec3 end1,
         Vec3 end2,
         Vec3 normal1,
         Vec3 normal2,
         boolean skipCurve,
         boolean parallel,
         double[] intersect,
         double dist,
         int end1Extent,
         int end2Extent
      ) {
         return new CoasterTrackPlacement.CoasterSnapGeometryBackup(
            pos1,
            pos2,
            state1,
            state2,
            axis1,
            axis2,
            normedAxis1,
            normedAxis2,
            end1,
            end2,
            normal1,
            normal2,
            skipCurve,
            parallel,
            intersect == null ? null : (double[])intersect.clone(),
            dist,
            end1Extent,
            end2Extent
         );
      }
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

      public CoasterTrackPlacement.PlacementInfo withMessage(String message) {
         this.message = "track." + message;
         this.modTranslationKey = false;
         return this;
      }

      public CoasterTrackPlacement.PlacementInfo withModTranslationKey(String translationKey) {
         this.message = translationKey;
         this.modTranslationKey = true;
         return this;
      }

      public CoasterTrackPlacement.PlacementInfo tooJumbly() {
         this.curve = null;
         this.valid = false;
         this.anchorAnchorBuildResult = null;
         this.placementSmoothingNeighbors = null;
         return this;
      }
   }

   private static record PlacementSmoothingBatch(BezierConnection primary, List<BezierConnection> neighbors) {
   }
}
