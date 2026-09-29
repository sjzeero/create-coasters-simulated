package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.assembly.CoasterBezierAssemblyRotationFix;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftCommittedSegment;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftDriveDirection;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftEndRef;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftShaftAxis;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpan;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterTrackPropagator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CoasterAnchorpointBlockEntity extends KineticBlockEntity {
   private static final int FAKE_TRACK_KEEPALIVE_INTERVAL_TICKS = 20;
   private final Map<BlockPos, BezierConnection> anchorPeerCurves = new HashMap<>();
   private final Map<BlockPos, Integer> anchorPeerCurveRailDiffuseRgb = new HashMap<>();
   private final Map<BlockPos, Integer> anchorPeerCurveBeamDiffuseRgb = new HashMap<>();
   public static final float DEFAULT_JOINT_BANK_RADIANS = 0.0F;
   private float jointBankRadians = 0.0F;
   private float liftBlocks = Float.NaN;
   private float openEndExtensionBlocks = Float.NaN;
   private int fakeTrackKeepAliveTicks = 0;
   @Nullable
   private Axis chainLiftShaftAxis;
   @Nullable
   private ChainLiftEndRef chainLiftEndRef;
   private double chainLiftCommittedLength;
   private BlockPos chainLiftFirstHop = BlockPos.ZERO;
   private List<ChainLiftCommittedSegment> chainLiftCommittedSegments = List.of();
   private int chainLiftTravelSign;

   public CoasterAnchorpointBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)SimulatedCoastersBlocks.COASTER_ANCHORPOINT_BE.get(), pos, blockState);
      this.jointBankRadians = 0.0F;
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, CoasterAnchorpointBlockEntity be) {
      be.tick();
      if (level instanceof ServerLevel sl) {
         be.runServerKeepAlive(sl);
      }
   }

   public boolean hasChainLiftShaft() {
      return this.chainLiftShaftAxis != null;
   }

   public Axis getChainLiftShaftAxis() {
      return this.chainLiftShaftAxis;
   }

   public void setChainLiftShaft(Axis axis) {
      if (this.chainLiftShaftAxis != axis) {
         if (this.level != null && !this.level.isClientSide() && this.chainLiftShaftAxis != null) {
            this.detachKinetics();
            this.clearKineticInformation();
         }

         this.chainLiftShaftAxis = axis;
         this.setChanged();
         this.notifyClientSync();
         this.syncAnchorVisualBlockState();
         if (this.level != null) {
            VisualizationHelper.queueUpdate(this);
         }

         if (this.level != null) {
            ChainLiftClientIndex.refreshMembership(this);
         }

         if (this.level != null && !this.level.isClientSide()) {
            this.attachKinetics();
         }
      }
   }

   public boolean cycleChainLiftShaftAxis() {
      if (this.hasChainLiftShaft() && this.chainLiftShaftAxis != null && this.level != null && !this.level.isClientSide()) {
         Direction facing = (Direction)this.getBlockState().getValue(CoasterAnchorpointBlock.FACING);
         Axis next = ChainLiftShaftAxis.alternateAxis(this.chainLiftShaftAxis, facing);
         this.setChainLiftShaft(next);
         CoasterPathGraphSpans.SpanPath path = this.resolveChainLiftSpanPath(0.0);
         if (path != null) {
            this.chainLiftTravelSign = ChainLiftDriveDirection.computeTravelSign(this, path, 0.0);
            this.setChanged();
            this.notifyClientSync();
         }

         return true;
      } else {
         return false;
      }
   }

   public void clearChainLiftShaft() {
      if (this.chainLiftShaftAxis != null || this.chainLiftEndRef != null) {
         if (this.level != null && !this.level.isClientSide()) {
            this.detachKinetics();
            this.clearKineticInformation();
         }

         this.chainLiftShaftAxis = null;
         this.clearChainLiftSpanData();
         this.setChanged();
         this.notifyClientSync();
         this.syncAnchorVisualBlockState();
         if (this.level != null) {
            VisualizationHelper.queueUpdate(this);
         }

         if (this.level != null) {
            ChainLiftClientIndex.refreshMembership(this);
         }
      }
   }

   public boolean hasChainLiftSpan() {
      return this.chainLiftEndRef != null && this.chainLiftCommittedLength > 0.0;
   }

   @Nullable
   public ChainLiftEndRef getChainLiftEndRef() {
      return this.chainLiftEndRef;
   }

   public double getChainLiftCommittedLength() {
      return this.chainLiftCommittedLength;
   }

   public int getChainLiftTravelSign() {
      return this.chainLiftTravelSign;
   }

   @Nullable
   public CoasterPathGraphSpans.SpanPath resolveChainLiftSpanPath(double partialTickForRender) {
      if (this.level != null && this.chainLiftEndRef != null && !(this.chainLiftCommittedLength <= 0.0)) {
         CoasterPathGraphSpans.SpanPath rebound = ChainLiftSpan.rebindCommittedSpan(
            this.level, this.chainLiftCommittedSegments, this.chainLiftCommittedLength, partialTickForRender
         );
         if (rebound != null) {
            return rebound;
         } else {
            CoasterPathGraphSpans.SpanPath effective = ChainLiftSpan.resolveEffective(
               this.level, this.getBlockPos(), this.chainLiftEndRef, this.chainLiftCommittedLength, this.chainLiftFirstHop, partialTickForRender
            );
            if (effective != null && this.chainLiftCommittedSegments.isEmpty() && this.level instanceof ServerLevel) {
               this.chainLiftCommittedSegments = ChainLiftSpan.segmentsFromPath(effective);
               this.setChanged();
            }

            return effective;
         }
      } else {
         return null;
      }
   }

   public List<ChainLiftCommittedSegment> getChainLiftCommittedSegments() {
      return this.chainLiftCommittedSegments;
   }

   public void commitChainLift(Axis shaftAxis, ChainLiftEndRef endRef, CoasterPathGraphSpans.SpanPath committedPath) {
      this.chainLiftEndRef = endRef;
      this.chainLiftCommittedLength = committedPath.totalLength();
      this.chainLiftCommittedSegments = ChainLiftSpan.segmentsFromPath(committedPath);
      this.chainLiftFirstHop = ChainLiftSpan.firstHopNode(committedPath, this.getBlockPos()).immutable();
      this.setChainLiftShaft(shaftAxis);
      this.chainLiftTravelSign = ChainLiftDriveDirection.computeTravelSign(this, committedPath, 0.0);
      this.setChanged();
      this.notifyClientSync();
      ChainLiftClientIndex.refreshMembership(this);
      ChainLiftSpanPathCache.registerResolved(this, committedPath);
   }

   private void clearChainLiftSpanData() {
      this.chainLiftEndRef = null;
      this.chainLiftCommittedLength = 0.0;
      this.chainLiftCommittedSegments = List.of();
      this.chainLiftFirstHop = BlockPos.ZERO;
      this.chainLiftTravelSign = 0;
      if (this.level != null) {
         ChainLiftClientIndex.refreshMembership(this);
      }
   }

   private void migrateChainLiftTravelSignIfNeeded() {
      if (this.chainLiftTravelSign == 0 && this.hasChainLiftSpan()) {
         CoasterPathGraphSpans.SpanPath path = this.resolveChainLiftSpanPath(0.0);
         if (path != null) {
            this.chainLiftTravelSign = ChainLiftDriveDirection.computeTravelSign(this, path, 0.0);
            this.setChanged();
         }
      }
   }

   private void syncAnchorVisualBlockState() {
      if (this.level != null && !this.level.isClientSide()) {
         BlockPos pos = this.getBlockPos();
         BlockState current = this.level.getBlockState(pos);
         if (current.hasProperty(CoasterAnchorpointBlock.CHAIN_LIFT_SHAFT) && current.hasProperty(CoasterAnchorpointBlock.TRACK_CONNECTED)) {
            boolean wantsShaft = this.hasChainLiftShaft();
            boolean wantsConnected = this.legCount() >= 1;
            if ((Boolean)current.getValue(CoasterAnchorpointBlock.CHAIN_LIFT_SHAFT) != wantsShaft
               || (Boolean)current.getValue(CoasterAnchorpointBlock.TRACK_CONNECTED) != wantsConnected) {
               this.level
                  .setBlock(
                     pos,
                     (BlockState)((BlockState)current.setValue(CoasterAnchorpointBlock.CHAIN_LIFT_SHAFT, wantsShaft))
                        .setValue(CoasterAnchorpointBlock.TRACK_CONNECTED, wantsConnected),
                     2
                  );
            }
         }
      }
   }

   public void onLoad() {
      super.onLoad();
      AnchorPeerCurveClientIndex.refreshMembership(this);
      if (this.level instanceof ServerLevel sl) {
         for (BezierConnection bc : this.anchorPeerCurves.values()) {
            AnchorPeerFakeTracks.manageFakeTracksAlong(sl, bc, false);
         }

         if (this.hasChainLiftShaft()) {
            this.attachKinetics();
         }

         this.migrateChainLiftTravelSignIfNeeded();
         this.syncAnchorVisualBlockState();
      }

      if (this.level != null && this.level.isClientSide()) {
         AnchorJunctionVisualRefresh.refreshAround(this.level, this.getBlockPos());
      }

      if (this.level != null) {
         ChainLiftClientIndex.refreshMembership(this);
      }
   }

   private void runServerKeepAlive(ServerLevel level) {
      if (!this.anchorPeerCurves.isEmpty()) {
         if (++this.fakeTrackKeepAliveTicks >= 20) {
            this.fakeTrackKeepAliveTicks = 0;

            for (BezierConnection bc : this.anchorPeerCurves.values()) {
               AnchorPeerFakeTracks.keepAliveAlong(level, bc);
            }
         }
      }
   }

   public void remove() {
      AnchorPeerCurveClientIndex.remove(this);
      ChainLiftClientIndex.remove(this);
      super.remove();
   }

   public int legCount() {
      return this.anchorPeerCurves.size();
   }

   public boolean canAcceptAnchorPeer(BlockPos peerAnchor) {
      return peerAnchor.equals(this.getBlockPos()) ? false : !this.anchorPeerCurves.containsKey(peerAnchor) && this.legCount() < 2;
   }

   public Map<BlockPos, BezierConnection> getAnchorPeerCurvesView() {
      return Map.copyOf(this.anchorPeerCurves);
   }

   public Map<BlockPos, BezierConnection> viewAnchorPeerCurvesSnapshot() {
      return Map.copyOf(this.anchorPeerCurves);
   }

   public void rewriteNeighborPositionsAfterAssemblyTransform(AssemblyTransform asm) {
      if (this.level instanceof ServerLevel) {
         this.rewriteChainLiftAfterAssemblyTransform(asm);
         if (!this.anchorPeerCurves.isEmpty() || !this.anchorPeerCurveRailDiffuseRgb.isEmpty() || !this.anchorPeerCurveBeamDiffuseRgb.isEmpty()) {
            Map<BlockPos, BezierConnection> snap = new HashMap<>(this.anchorPeerCurves);
            this.anchorPeerCurves.clear();

            for (Entry<BlockPos, BezierConnection> e : snap.entrySet()) {
               this.anchorPeerCurves.put(asm.apply(e.getKey()).immutable(), e.getValue());
            }

            Map<BlockPos, Integer> tintSnapRail = new HashMap<>(this.anchorPeerCurveRailDiffuseRgb);
            Map<BlockPos, Integer> tintSnapBeam = new HashMap<>(this.anchorPeerCurveBeamDiffuseRgb);
            this.anchorPeerCurveRailDiffuseRgb.clear();
            this.anchorPeerCurveBeamDiffuseRgb.clear();

            for (Entry<BlockPos, Integer> e : tintSnapRail.entrySet()) {
               this.anchorPeerCurveRailDiffuseRgb.put(asm.apply(e.getKey()).immutable(), e.getValue());
            }

            for (Entry<BlockPos, Integer> e : tintSnapBeam.entrySet()) {
               this.anchorPeerCurveBeamDiffuseRgb.put(asm.apply(e.getKey()).immutable(), e.getValue());
            }

            this.setChanged();
            this.notifyClientSync();
         }
      }
   }

   private void rewriteChainLiftAfterAssemblyTransform(AssemblyTransform asm) {
      if (this.chainLiftShaftAxis != null || this.chainLiftEndRef != null) {
         boolean changed = false;
         if (this.chainLiftEndRef != null) {
            this.chainLiftEndRef = this.chainLiftEndRef.applyAssemblyTransform(asm);
            if (!this.chainLiftFirstHop.equals(BlockPos.ZERO)) {
               this.chainLiftFirstHop = asm.apply(this.chainLiftFirstHop).immutable();
            }

            changed = true;
         }

         if (this.chainLiftShaftAxis != null) {
            StructureTransform structure = CoasterBezierAssemblyRotationFix.yAxisStructureRotation(asm);
            if (structure != null) {
               Axis rotated = ChainLiftShaftAxis.rotateStoredAxis(structure, this.chainLiftShaftAxis);
               if (rotated != this.chainLiftShaftAxis) {
                  this.chainLiftShaftAxis = rotated;
                  changed = true;
               }
            }
         }

         if (changed) {
            this.setChanged();
            this.notifyClientSync();
         }

         if (this.hasChainLiftShaft()) {
            this.reattachKineticAfterAssemblyMove();
         }
      }
   }

   public void prepareKineticForAssemblyMove() {
      if (this.level != null && !this.level.isClientSide() && this.hasChainLiftShaft()) {
         this.detachKinetics();
         this.clearKineticInformation();
         this.warnOfMovement();
      }
   }

   public void reattachKineticAfterAssemblyMove() {
      if (this.level != null && !this.level.isClientSide() && this.hasChainLiftShaft()) {
         this.detachKinetics();
         this.clearKineticInformation();
         this.attachKinetics();
         this.setChanged();
         this.notifyClientSync();
      }
   }

   public int getPeerCurveRailDiffuseRgb(BlockPos peerEndpoint) {
      return this.anchorPeerCurveRailDiffuseRgb.getOrDefault(peerEndpoint, 0);
   }

   public int getPeerCurveBeamDiffuseRgb(BlockPos peerEndpoint) {
      return this.anchorPeerCurveBeamDiffuseRgb.getOrDefault(peerEndpoint, 0);
   }

   public boolean setPeerCurveTieredTints(BlockPos peerEndpoint, int railRgb, int beamRgb) {
      BlockPos key = peerEndpoint.immutable();
      int rail = railRgb & 16777215;
      int beam = beamRgb & 16777215;
      int prevRail = this.anchorPeerCurveRailDiffuseRgb.getOrDefault(key, 0);
      int prevBeam = this.anchorPeerCurveBeamDiffuseRgb.getOrDefault(key, 0);
      if (prevRail == rail && prevBeam == beam) {
         return false;
      } else {
         if (rail == 0) {
            this.anchorPeerCurveRailDiffuseRgb.remove(key);
         } else {
            this.anchorPeerCurveRailDiffuseRgb.put(key, rail);
         }

         if (beam == 0) {
            this.anchorPeerCurveBeamDiffuseRgb.remove(key);
         } else {
            this.anchorPeerCurveBeamDiffuseRgb.put(key, beam);
         }

         this.setChanged();
         this.notifyClientSync();
         return true;
      }
   }

   public long anchorCurveTintSignature() {
      if (this.anchorPeerCurveRailDiffuseRgb.isEmpty() && this.anchorPeerCurveBeamDiffuseRgb.isEmpty()) {
         return 0L;
      } else {
         long h = -3750763034362895579L;
         HashSet<BlockPos> peerSet = new HashSet<>();
         peerSet.addAll(this.anchorPeerCurveRailDiffuseRgb.keySet());
         peerSet.addAll(this.anchorPeerCurveBeamDiffuseRgb.keySet());
         ArrayList<BlockPos> peers = new ArrayList<>(peerSet);
         peers.sort(Comparator.comparingLong(BlockPos::asLong));

         for (BlockPos p : peers) {
            h ^= p.asLong();
            h *= 1099511628211L;
            h ^= (long)this.anchorPeerCurveRailDiffuseRgb.getOrDefault(p, 0).intValue();
            h *= 1099511628211L;
            h ^= (long)this.anchorPeerCurveBeamDiffuseRgb.getOrDefault(p, 0).intValue();
            h *= 1099511628211L;
         }

         return h;
      }
   }

   public boolean hasAnchorPeer(BlockPos peerAnchor) {
      return this.anchorPeerCurves.containsKey(peerAnchor);
   }

   public float getJointBankRadians() {
      return this.jointBankRadians;
   }

   public void setJointBankRadians(float radians) {
      this.jointBankRadians = radians;
      this.setChanged();
      this.notifyClientSync();
   }

   public void resetJointBank() {
      this.setJointBankRadians(0.0F);
   }

   public boolean hasLift() {
      return !Float.isNaN(this.liftBlocks);
   }

   public float getStoredLiftBlocks() {
      return this.liftBlocks;
   }

   public float getLiftBlocks() {
      return Float.isNaN(this.liftBlocks) ? 0.5F : this.liftBlocks;
   }

   public void setLiftBlocks(float blocks) {
      this.liftBlocks = blocks;
      this.setChanged();
      this.notifyClientSync();
   }

   public void clearLift() {
      if (!Float.isNaN(this.liftBlocks)) {
         this.liftBlocks = Float.NaN;
         this.setChanged();
         this.notifyClientSync();
      }
   }

   public boolean hasStoredOpenEndExtension() {
      return !Float.isNaN(this.openEndExtensionBlocks);
   }

   public float getStoredOpenEndExtensionBlocks() {
      return this.openEndExtensionBlocks;
   }

   public float getOpenEndExtensionBlocks() {
      return Float.isNaN(this.openEndExtensionBlocks) ? 0.5F : this.openEndExtensionBlocks;
   }

   public void setOpenEndExtensionBlocks(float blocks) {
      this.openEndExtensionBlocks = CoasterOpenEndExtension.clampExtensionBlocks(blocks);
      this.setChanged();
      this.notifyClientSync();
   }

   public void clearOpenEndExtension() {
      if (!Float.isNaN(this.openEndExtensionBlocks)) {
         this.openEndExtensionBlocks = Float.NaN;
         this.setChanged();
         this.notifyClientSync();
      }
   }

   private void reconcileLegDerivedState() {
      if (this.legCount() == 0) {
         this.clearChainLiftShaft();
         this.resetJointBank();
         this.clearLift();
         this.clearOpenEndExtension();
      } else {
         if (this.legCount() != 1) {
            this.clearOpenEndExtension();
         }
      }
   }

   public void dropReciprocalPeerCurve(BlockPos formerPeer) {
      this.anchorPeerCurves.remove(formerPeer);
      this.anchorPeerCurveRailDiffuseRgb.remove(formerPeer);
      this.anchorPeerCurveBeamDiffuseRgb.remove(formerPeer);
      this.reconcileLegDerivedState();
      this.syncAnchorVisualBlockState();
      this.setChanged();
      this.notifyClientSync();
   }

   public void detachAllConnections(ServerLevel level) {
      this.clearChainLiftShaft();
      Set<BlockPos> anchorPeers = new HashSet<>(this.anchorPeerCurves.keySet());

      for (BezierConnection bc : this.anchorPeerCurves.values()) {
         if (CoasterPeerCurveHandleLengths.isCoaster(bc)) {
            CoasterPeerCurveHandleLengths.removeEdge(bc.isPrimary() ? bc : bc.secondary());
         }

         AnchorPeerFakeTracks.manageFakeTracksAlong(level, bc, true);
      }

      this.anchorPeerCurves.clear();
      this.anchorPeerCurveRailDiffuseRgb.clear();
      this.anchorPeerCurveBeamDiffuseRgb.clear();
      this.reconcileLegDerivedState();

      for (BlockPos peer : anchorPeers) {
         if (level.getBlockEntity(peer) instanceof CoasterAnchorpointBlockEntity p) {
            p.dropReciprocalPeerCurve(this.getBlockPos());
         }
      }

      List<BlockPos> touch = new ArrayList<>();
      touch.add(this.getBlockPos());
      touch.addAll(anchorPeers);
      CoasterTrackPropagator.touch(level, touch);
      this.syncAnchorVisualBlockState();
      this.setChanged();
      this.notifyClientSync();
   }

   public void overwriteReciprocalForPeerCurve(BlockPos anchorHostingPrimaryTowardHere, BezierConnection primaryTowardThis) {
      if (primaryTowardThis.isPrimary()) {
         this.anchorPeerCurves.put(anchorHostingPrimaryTowardHere.immutable(), primaryTowardThis.secondary());
         this.setChanged();
         this.notifyClientSync();
         if (this.level instanceof ServerLevel sl && sl.getBlockEntity(anchorHostingPrimaryTowardHere) instanceof CoasterAnchorpointBlockEntity owner) {
            owner.setChanged();
            owner.notifyClientSync();
         }
      }
   }

   public void putAnchorPeerCurve(ServerLevel level, BlockPos peerAnchor, BezierConnection curveTowardPeer) {
      BlockPos peerKey = peerAnchor.immutable();
      BezierConnection previous = this.anchorPeerCurves.get(peerKey);
      this.anchorPeerCurves.put(peerKey, curveTowardPeer);
      if (level.getBlockEntity(peerAnchor) instanceof CoasterAnchorpointBlockEntity peer) {
         BezierConnection reciprocal = curveTowardPeer.isPrimary() ? curveTowardPeer.secondary() : curveTowardPeer;
         peer.anchorPeerCurves.put(this.getBlockPos().immutable(), reciprocal);
         peer.setChanged();
         peer.notifyClientSync();
         peer.syncAnchorVisualBlockStateDeferred();
      }

      BezierConnection primary = curveTowardPeer.isPrimary() ? curveTowardPeer : curveTowardPeer.secondary();
      if (previous != null) {
         BezierConnection oldPrimary = previous.isPrimary() ? previous : previous.secondary();
         CoasterTrackPropagator.manageFakeTracks(level, oldPrimary, true);
      }

      CoasterTrackPropagator.manageFakeTracks(level, primary, false);
      this.setChanged();
      this.notifyClientSync();
      this.syncAnchorVisualBlockStateDeferred();
      if (CoasterPeerCurveHandleLengths.isCoaster(curveTowardPeer)) {
         BezierConnection p = curveTowardPeer.isPrimary() ? curveTowardPeer : curveTowardPeer.secondary();
         CoasterPeerCurveHandleLengths.putSymmetricFromRuntime(p);
      }

      CoasterTrackPropagator.touch(level, List.of(this.getBlockPos(), peerAnchor));
      this.queueJunctionVisualRefresh(level);
   }

   public void syncAnchorVisualBlockStateDeferred() {
      if (!CoasterTrackPropagator.scheduleVisualBlockStateSync(this.getBlockPos())) {
         this.syncAnchorVisualBlockState();
      }
   }

   private void queueJunctionVisualRefresh(ServerLevel level) {
      this.queueJunctionVisualRefresh(level, BlockPos.ZERO);
   }

   private void queueJunctionVisualRefresh(ServerLevel level, BlockPos... extraAnchorEndpoints) {
      if (level.isClientSide()) {
         Set<BlockPos> anchorEndpoints = new LinkedHashSet<>();
         anchorEndpoints.add(this.getBlockPos());

         for (BlockPos extra : extraAnchorEndpoints) {
            if (extra != null && !extra.equals(BlockPos.ZERO)) {
               anchorEndpoints.add(extra.immutable());
            }
         }

         for (BezierConnection bc : this.anchorPeerCurves.values()) {
            if (bc != null) {
               BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
               anchorEndpoints.add((BlockPos)p.bePositions.getFirst());
               anchorEndpoints.add((BlockPos)p.bePositions.getSecond());
            }
         }

         AnchorJunctionVisualRefresh.refreshAround(level, anchorEndpoints.toArray(BlockPos[]::new));
      }
   }

   public void removeAnchorPeerCurve(ServerLevel level, BlockPos peerAnchor) {
      this.anchorPeerCurveRailDiffuseRgb.remove(peerAnchor);
      this.anchorPeerCurveBeamDiffuseRgb.remove(peerAnchor);
      BezierConnection removed = this.anchorPeerCurves.remove(peerAnchor);
      Set<BlockPos> junctionRefreshSeeds = new LinkedHashSet<>();
      junctionRefreshSeeds.add(this.getBlockPos());
      junctionRefreshSeeds.add(peerAnchor);
      if (removed != null) {
         BezierConnection p = removed.isPrimary() ? removed : removed.secondary();
         junctionRefreshSeeds.add((BlockPos)p.bePositions.getFirst());
         junctionRefreshSeeds.add((BlockPos)p.bePositions.getSecond());
      }

      for (BezierConnection bc : this.anchorPeerCurves.values()) {
         if (bc != null) {
            BezierConnection rem = bc.isPrimary() ? bc : bc.secondary();
            junctionRefreshSeeds.add((BlockPos)rem.bePositions.getFirst());
            junctionRefreshSeeds.add((BlockPos)rem.bePositions.getSecond());
         }
      }

      this.reconcileLegDerivedState();
      if (level.getBlockEntity(peerAnchor) instanceof CoasterAnchorpointBlockEntity peer) {
         peer.dropReciprocalPeerCurve(this.getBlockPos());
      }

      if (removed != null) {
         CoasterPeerCurveHandleLengths.removeEdge(removed.isPrimary() ? removed : removed.secondary());
         AnchorPeerFakeTracks.manageFakeTracksAlong(level, removed.isPrimary() ? removed : removed.secondary(), true);
      }

      this.setChanged();
      this.notifyClientSync();
      this.syncAnchorVisualBlockState();
      CoasterTrackPropagator.touch(level, List.of(this.getBlockPos(), peerAnchor));
      this.queueJunctionVisualRefresh(level, junctionRefreshSeeds.toArray(BlockPos[]::new));
   }

   public static Vec3 worldCenter(Level level, BlockPos anchorPos) {
      Vec3 base = worldCenterBase(level, anchorPos);
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         float lift = be.getLiftBlocks();
         if (Math.abs(lift) < 1.0E-9F) {
            return base;
         } else {
            Vec3 dir = anchorLiftDirection(level, anchorPos);
            return base.add(dir.scale((double)lift));
         }
      } else {
         return base;
      }
   }

   public static Vec3 worldCenterBase(Level level, BlockPos anchorPos) {
      BlockState state = level.getBlockState(anchorPos);
      Direction facing = state.hasProperty(CoasterAnchorpointBlock.FACING) ? (Direction)state.getValue(CoasterAnchorpointBlock.FACING) : Direction.UP;

      Vec3 local = switch (facing) {
         case UP -> new Vec3(0.5, 0.25, 0.5);
         case DOWN -> new Vec3(0.5, 0.75, 0.5);
         case NORTH -> new Vec3(0.5, 0.5, 0.75);
         case SOUTH -> new Vec3(0.5, 0.5, 0.25);
         case WEST -> new Vec3(0.75, 0.5, 0.5);
         case EAST -> new Vec3(0.25, 0.5, 0.5);
         default -> throw new MatchException(null, null);
      };
      return Vec3.atLowerCornerOf(anchorPos).add(local).add(0.0, 0.0, 0.0);
   }

   public static Vec3 worldCenterWithLift(Level level, BlockPos anchorPos, float liftBlocks) {
      Vec3 base = worldCenterBase(level, anchorPos);
      return Math.abs(liftBlocks) < 1.0E-9F ? base : base.add(anchorLiftDirection(level, anchorPos).scale((double)liftBlocks));
   }

   public static Vec3 anchorLiftDirection(Level level, BlockPos anchorPos) {
      Vec3 fallback = localUp(level, anchorPos);
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
         if (incident.isEmpty()) {
            return fallback;
         } else {
            Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), anchorPos);
            if (t == null) {
               return fallback;
            } else {
               Vec3 face = CoasterTrackPlacement.anchorFaceNormalForAxis(level, anchorPos, t);
               return face.lengthSqr() < 1.0E-12 ? fallback : face.normalize();
            }
         }
      } else {
         return fallback;
      }
   }

   public static Vec3 localUp(Level level, BlockPos anchorPos) {
      BlockState state = level.getBlockState(anchorPos);
      Direction facing = state.hasProperty(CoasterAnchorpointBlock.FACING) ? (Direction)state.getValue(CoasterAnchorpointBlock.FACING) : Direction.UP;
      return Vec3.atLowerCornerOf(facing.getNormal()).normalize();
   }

   @Nullable
   public CoasterAnchorpointBlockEntity.AnchorEndpointTowardPeer endpointTowardPeer(Level level, BlockPos peerAnchor) {
      return endpointTowardPeerAt(level, this.getBlockPos(), peerAnchor);
   }

   @Nullable
   public static CoasterAnchorpointBlockEntity.AnchorEndpointTowardPeer endpointTowardPeerAt(Level level, BlockPos selfPos, BlockPos peerAnchor) {
      Vec3 centerSelf = worldCenter(level, selfPos);
      Vec3 towardPeer = worldCenter(level, peerAnchor).subtract(centerSelf);
      if (towardPeer.lengthSqr() < 1.0E-12) {
         return null;
      } else {
         Vec3 axisInto = towardPeer.normalize();
         Vec3 anchorNormal = CoasterTrackPlacement.anchorFaceNormalForAxis(level, selfPos, axisInto);
         return new CoasterAnchorpointBlockEntity.AnchorEndpointTowardPeer(centerSelf, axisInto, anchorNormal);
      }
   }

   private static Vec3 projectPerpendicular(Vec3 v, Vec3 axis) {
      Vec3 a = axis.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axis.normalize();
      return v.subtract(a.scale(a.dot(v)));
   }

   private static Vec3 orthonormalToAxis(Vec3 normal, Vec3 axis) {
      Vec3 n = projectPerpendicular(normal, axis);
      if (n.lengthSqr() < 1.0E-10) {
         n = projectPerpendicular(new Vec3(1.0, 0.0, 0.0), axis);
      }

      if (n.lengthSqr() < 1.0E-10) {
         n = projectPerpendicular(new Vec3(0.0, 0.0, 1.0), axis);
      }

      return n.normalize();
   }

   private void notifyClientSync() {
      if (this.level instanceof ServerLevel) {
         if (!CoasterTrackPropagator.scheduleBlockSyncIfBatching(this.getBlockPos())) {
            this.notifyUpdate();
         }
      }
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      ListTag anchorCurveList = new ListTag();

      for (Entry<BlockPos, BezierConnection> e : this.anchorPeerCurves.entrySet()) {
         CompoundTag c = new CompoundTag();
         c.putLong("Peer", e.getKey().asLong());
         c.put("Bezier", e.getValue().write(this.getBlockPos()));
         BezierConnection raw = e.getValue();
         BezierConnection p = raw.isPrimary() ? raw : raw.secondary();
         if (CoasterTrackMaterials.COASTER.id.equals(p.getMaterial().id)) {
            Couple<Double> hl = CoasterPeerCurveHandleLengths.getCanonicalLengths(p);
            if (hl != null) {
               c.putDouble("HLo", (Double)hl.getFirst());
               c.putDouble("HHi", (Double)hl.getSecond());
            }
         }

         anchorCurveList.add(c);
      }

      tag.put("AnchorPeerCurves", anchorCurveList);
      ListTag tintList = new ListTag();
      HashSet<BlockPos> tintPeers = new HashSet<>();
      tintPeers.addAll(this.anchorPeerCurveRailDiffuseRgb.keySet());
      tintPeers.addAll(this.anchorPeerCurveBeamDiffuseRgb.keySet());

      for (BlockPos peer : tintPeers) {
         int r = this.anchorPeerCurveRailDiffuseRgb.getOrDefault(peer, 0);
         int b = this.anchorPeerCurveBeamDiffuseRgb.getOrDefault(peer, 0);
         if (r != 0 || b != 0) {
            CompoundTag c = new CompoundTag();
            c.putLong("Peer", peer.asLong());
            c.putInt("RailRGB", r);
            c.putInt("BeamRGB", b);
            tintList.add(c);
         }
      }

      tag.put("AnchorPeerCurveTints", tintList);
      tag.putFloat("JointBank", this.jointBankRadians);
      if (!Float.isNaN(this.liftBlocks)) {
         tag.putFloat("Lift", this.liftBlocks);
      }

      if (!Float.isNaN(this.openEndExtensionBlocks)) {
         tag.putFloat("OpenEndExtension", this.openEndExtensionBlocks);
      }

      if (this.chainLiftShaftAxis != null) {
         tag.putString("ChainLiftShaftAxis", this.chainLiftShaftAxis.getName());
      }

      if (this.chainLiftEndRef != null) {
         CompoundTag span = new CompoundTag();
         this.chainLiftEndRef.write(span);
         tag.put("ChainLiftSpan", span);
         tag.putDouble("ChainLiftCommittedLength", this.chainLiftCommittedLength);
         tag.putLong("ChainLiftFirstHop", this.chainLiftFirstHop.asLong());
         if (!this.chainLiftCommittedSegments.isEmpty()) {
            tag.put("ChainLiftCommittedSegments", ChainLiftCommittedSegment.writeList(this.chainLiftCommittedSegments));
         }

         if (this.chainLiftTravelSign != 0) {
            tag.putInt("ChainLiftTravelSign", this.chainLiftTravelSign);
         }
      }

      super.write(tag, registries, clientPacket);
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      this.anchorPeerCurves.clear();

      for (Tag value : tag.getList("AnchorPeerCurves", 10)) {
         if (value instanceof CompoundTag) {
            CompoundTag c = (CompoundTag)value;
            if (c.contains("Peer", 4) && c.contains("Bezier", 10)) {
               BlockPos peer = BlockPos.of(c.getLong("Peer"));
               BezierConnection bc = new BezierConnection(c.getCompound("Bezier"), this.getBlockPos());
               this.anchorPeerCurves.put(peer, bc);
               if (c.contains("HLo", 6) && c.contains("HHi", 6)) {
                  BezierConnection pri = bc.isPrimary() ? bc : bc.secondary();
                  CoasterPeerCurveHandleLengths.putCanonical(pri, c.getDouble("HLo"), c.getDouble("HHi"));
               }
            }
         }
      }

      this.jointBankRadians = tag.contains("JointBank", 5) ? tag.getFloat("JointBank") : 0.0F;
      this.liftBlocks = tag.contains("Lift", 5) ? tag.getFloat("Lift") : Float.NaN;
      this.openEndExtensionBlocks = tag.contains("OpenEndExtension", 5) ? tag.getFloat("OpenEndExtension") : Float.NaN;
      this.chainLiftShaftAxis = tag.contains("ChainLiftShaftAxis", 8) ? Axis.byName(tag.getString("ChainLiftShaftAxis")) : null;
      if (tag.contains("ChainLiftSpan", 10)) {
         this.chainLiftEndRef = ChainLiftEndRef.read(tag.getCompound("ChainLiftSpan"));
         this.chainLiftCommittedLength = tag.contains("ChainLiftCommittedLength", 6) ? tag.getDouble("ChainLiftCommittedLength") : 0.0;
         this.chainLiftFirstHop = tag.contains("ChainLiftFirstHop", 4) ? BlockPos.of(tag.getLong("ChainLiftFirstHop")) : BlockPos.ZERO;
         this.chainLiftCommittedSegments = tag.contains("ChainLiftCommittedSegments", 9)
            ? ChainLiftCommittedSegment.readList(tag.getList("ChainLiftCommittedSegments", 10))
            : List.of();
         this.chainLiftTravelSign = tag.contains("ChainLiftTravelSign", 3) ? tag.getInt("ChainLiftTravelSign") : 0;
      } else {
         this.clearChainLiftSpanData();
      }

      if (this.level != null) {
         ChainLiftClientIndex.refreshMembership(this);
      }

      this.anchorPeerCurveRailDiffuseRgb.clear();
      this.anchorPeerCurveBeamDiffuseRgb.clear();

      for (Tag valuex : tag.getList("AnchorPeerCurveTints", 10)) {
         if (valuex instanceof CompoundTag) {
            CompoundTag c = (CompoundTag)valuex;
            if (c.contains("Peer", 4)) {
               BlockPos peer = BlockPos.of(c.getLong("Peer"));
               int beam;
               int rail;
               if (!c.contains("RailRGB", 3) && !c.contains("BeamRGB", 3)) {
                  if (!c.contains("RGB", 3)) {
                     continue;
                  }

                  rail = c.getInt("RGB") & 16777215;
                  beam = 0;
               } else {
                  rail = c.getInt("RailRGB") & 16777215;
                  beam = c.getInt("BeamRGB") & 16777215;
               }

               if (rail != 0) {
                  this.anchorPeerCurveRailDiffuseRgb.put(peer, rail);
               }

               if (beam != 0) {
                  this.anchorPeerCurveBeamDiffuseRgb.put(peer, beam);
               }
            }
         }
      }

      AnchorPeerCurveClientIndex.refreshMembership(this);
      super.read(tag, registries, clientPacket);
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void reloadCurveDataForPonder(CompoundTag tag, Provider registries) {
      this.read(tag, registries, false);
   }

   public void repairAnchorPeerCurveKeys() {
      if (!this.anchorPeerCurves.isEmpty()) {
         Map<BlockPos, BezierConnection> rebuilt = new HashMap<>();
         Map<BlockPos, BlockPos> peerRemap = new HashMap<>();
         BlockPos self = this.getBlockPos();

         for (Entry<BlockPos, BezierConnection> e : this.anchorPeerCurves.entrySet()) {
            BezierConnection bc = e.getValue();
            if (bc != null) {
               BlockPos oldPeer = e.getKey();
               BlockPos peer = peerFromBezierEndpoints(self, oldPeer, bc);
               rebuilt.put(peer.immutable(), bc);
               if (!oldPeer.equals(peer)) {
                  peerRemap.put(oldPeer.immutable(), peer.immutable());
               }
            }
         }

         this.anchorPeerCurves.clear();
         this.anchorPeerCurves.putAll(rebuilt);
         this.repairAnchorPeerCurveTintKeys(peerRemap);
      }
   }

   private void repairAnchorPeerCurveTintKeys(Map<BlockPos, BlockPos> peerRemap) {
      if (!this.anchorPeerCurveRailDiffuseRgb.isEmpty()) {
         Map<BlockPos, Integer> rebuilt = new HashMap<>();

         for (Entry<BlockPos, Integer> e : new HashMap<>(this.anchorPeerCurveRailDiffuseRgb).entrySet()) {
            BlockPos key = peerRemap.getOrDefault(e.getKey(), e.getKey());
            if (this.anchorPeerCurves.containsKey(key)) {
               rebuilt.put(key.immutable(), e.getValue());
            }
         }

         this.anchorPeerCurveRailDiffuseRgb.clear();
         this.anchorPeerCurveRailDiffuseRgb.putAll(rebuilt);
      }

      if (!this.anchorPeerCurveBeamDiffuseRgb.isEmpty()) {
         Map<BlockPos, Integer> rebuilt = new HashMap<>();

         for (Entry<BlockPos, Integer> ex : new HashMap<>(this.anchorPeerCurveBeamDiffuseRgb).entrySet()) {
            BlockPos key = peerRemap.getOrDefault(ex.getKey(), ex.getKey());
            if (this.anchorPeerCurves.containsKey(key)) {
               rebuilt.put(key.immutable(), ex.getValue());
            }
         }

         this.anchorPeerCurveBeamDiffuseRgb.clear();
         this.anchorPeerCurveBeamDiffuseRgb.putAll(rebuilt);
      }
   }

   private static BlockPos peerFromBezierEndpoints(BlockPos self, BlockPos peerFromTag, BezierConnection bc) {
      BlockPos p0 = (BlockPos)bc.bePositions.getFirst();
      BlockPos p1 = (BlockPos)bc.bePositions.getSecond();
      BlockPos peerFromGeometry;
      if (self.equals(p0)) {
         peerFromGeometry = p1;
      } else if (self.equals(p1)) {
         peerFromGeometry = p0;
      } else {
         peerFromGeometry = null;
      }

      if (peerFromGeometry == null) {
         return peerFromTag;
      } else if (!peerFromTag.equals(p0) && !peerFromTag.equals(p1)) {
         return peerFromGeometry;
      } else {
         return peerFromTag.equals(self) ? peerFromGeometry : peerFromTag;
      }
   }

   public static record AnchorEndpointTowardPeer(Vec3 worldStart, Vec3 axisIntoCurve, Vec3 normal) {
   }
}
