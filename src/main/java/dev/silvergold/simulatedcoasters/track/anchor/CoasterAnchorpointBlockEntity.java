/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.contraptions.StructureTransform
 *  com.simibubi.create.content.kinetics.base.KineticBlockEntity
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
 *  dev.engine_room.flywheel.lib.visualization.VisualizationHelper
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  net.createmod.catnip.data.Couple
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorJunctionVisualRefresh;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
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
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CoasterAnchorpointBlockEntity
extends KineticBlockEntity {
    private static final int FAKE_TRACK_KEEPALIVE_INTERVAL_TICKS = 20;
    private final Map<BlockPos, BezierConnection> anchorPeerCurves = new HashMap<BlockPos, BezierConnection>();
    private final Map<BlockPos, Integer> anchorPeerCurveRailDiffuseRgb = new HashMap<BlockPos, Integer>();
    private final Map<BlockPos, Integer> anchorPeerCurveBeamDiffuseRgb = new HashMap<BlockPos, Integer>();
    public static final float DEFAULT_JOINT_BANK_RADIANS = 0.0f;
    private float jointBankRadians = 0.0f;
    private float liftBlocks = Float.NaN;
    private float openEndExtensionBlocks = Float.NaN;
    private int fakeTrackKeepAliveTicks = 0;
    @Nullable
    private Direction.Axis chainLiftShaftAxis;
    @Nullable
    private ChainLiftEndRef chainLiftEndRef;
    private double chainLiftCommittedLength;
    private BlockPos chainLiftFirstHop = BlockPos.ZERO;
    private List<ChainLiftCommittedSegment> chainLiftCommittedSegments = List.of();
    private int chainLiftTravelSign;

    public CoasterAnchorpointBlockEntity(BlockPos pos, BlockState blockState) {
        super((BlockEntityType)SimulatedCoastersBlocks.COASTER_ANCHORPOINT_BE.get(), pos, blockState);
    }

    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoasterAnchorpointBlockEntity be) {
        be.tick();
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)level;
            be.runServerKeepAlive(sl);
        }
    }

    public boolean hasChainLiftShaft() {
        return this.chainLiftShaftAxis != null;
    }

    public Direction.Axis getChainLiftShaftAxis() {
        return this.chainLiftShaftAxis;
    }

    public void setChainLiftShaft(Direction.Axis axis) {
        if (this.chainLiftShaftAxis == axis) {
            return;
        }
        if (this.level != null && !this.level.isClientSide() && this.chainLiftShaftAxis != null) {
            this.detachKinetics();
            this.clearKineticInformation();
        }
        this.chainLiftShaftAxis = axis;
        this.setChanged();
        this.notifyClientSync();
        this.syncAnchorVisualBlockState();
        if (this.level != null) {
            VisualizationHelper.queueUpdate((BlockEntity)this);
        }
        if (this.level != null) {
            ChainLiftClientIndex.refreshMembership(this);
        }
        if (this.level != null && !this.level.isClientSide()) {
            this.attachKinetics();
        }
    }

    public boolean cycleChainLiftShaftAxis() {
        if (!this.hasChainLiftShaft() || this.chainLiftShaftAxis == null || this.level == null || this.level.isClientSide()) {
            return false;
        }
        Direction facing = (Direction)this.getBlockState().getValue((Property)CoasterAnchorpointBlock.FACING);
        Direction.Axis next = ChainLiftShaftAxis.alternateAxis(this.chainLiftShaftAxis, facing);
        this.setChainLiftShaft(next);
         @Nullable CoasterPathGraphSpans.SpanPath path = this.resolveChainLiftSpanPath(0.0);
        if (path != null) {
            this.chainLiftTravelSign = ChainLiftDriveDirection.computeTravelSign(this, path, 0.0);
            this.setChanged();
            this.notifyClientSync();
        }
        return true;
    }

    public void clearChainLiftShaft() {
        if (this.chainLiftShaftAxis == null && this.chainLiftEndRef == null) {
            return;
        }
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
            VisualizationHelper.queueUpdate((BlockEntity)this);
        }
        if (this.level != null) {
            ChainLiftClientIndex.refreshMembership(this);
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
        if (this.level == null || this.chainLiftEndRef == null || this.chainLiftCommittedLength <= 0.0) {
            return null;
        }
         @Nullable CoasterPathGraphSpans.SpanPath rebound = ChainLiftSpan.rebindCommittedSpan(this.level, this.chainLiftCommittedSegments, this.chainLiftCommittedLength, partialTickForRender);
        if (rebound != null) {
            return rebound;
        }
         @Nullable CoasterPathGraphSpans.SpanPath effective = ChainLiftSpan.resolveEffective(this.level, this.getBlockPos(), this.chainLiftEndRef, this.chainLiftCommittedLength, this.chainLiftFirstHop, partialTickForRender);
        if (effective != null && this.chainLiftCommittedSegments.isEmpty() && this.level instanceof ServerLevel) {
            this.chainLiftCommittedSegments = ChainLiftSpan.segmentsFromPath(effective);
            this.setChanged();
        }
        return effective;
    }

    public List<ChainLiftCommittedSegment> getChainLiftCommittedSegments() {
        return this.chainLiftCommittedSegments;
    }

    public void commitChainLift(Direction.Axis shaftAxis, ChainLiftEndRef endRef, CoasterPathGraphSpans.SpanPath committedPath) {
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
        if (this.chainLiftTravelSign != 0 || !this.hasChainLiftSpan()) {
            return;
        }
         @Nullable CoasterPathGraphSpans.SpanPath path = this.resolveChainLiftSpanPath(0.0);
        if (path == null) {
            return;
        }
        this.chainLiftTravelSign = ChainLiftDriveDirection.computeTravelSign(this, path, 0.0);
        this.setChanged();
    }

    private void syncAnchorVisualBlockState() {
        boolean wantsConnected;
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        BlockPos pos = this.getBlockPos();
        BlockState current = this.level.getBlockState(pos);
        if (!current.hasProperty((Property)CoasterAnchorpointBlock.CHAIN_LIFT_SHAFT) || !current.hasProperty((Property)CoasterAnchorpointBlock.TRACK_CONNECTED)) {
            return;
        }
        boolean wantsShaft = this.hasChainLiftShaft();
        boolean bl = wantsConnected = this.legCount() >= 1;
        if ((Boolean)current.getValue((Property)CoasterAnchorpointBlock.CHAIN_LIFT_SHAFT) == wantsShaft && (Boolean)current.getValue((Property)CoasterAnchorpointBlock.TRACK_CONNECTED) == wantsConnected) {
            return;
        }
        this.level.setBlock(pos, (BlockState)((BlockState)current.setValue((Property)CoasterAnchorpointBlock.CHAIN_LIFT_SHAFT, (Comparable)Boolean.valueOf(wantsShaft))).setValue((Property)CoasterAnchorpointBlock.TRACK_CONNECTED, (Comparable)Boolean.valueOf(wantsConnected)), 2);
    }

    public void onLoad() {
        super.onLoad();
        AnchorPeerCurveClientIndex.refreshMembership(this);
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)level;
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
        if (this.anchorPeerCurves.isEmpty()) {
            return;
        }
        if (++this.fakeTrackKeepAliveTicks < 20) {
            return;
        }
        this.fakeTrackKeepAliveTicks = 0;
        for (BezierConnection bc : this.anchorPeerCurves.values()) {
            AnchorPeerFakeTracks.keepAliveAlong(level, bc);
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
        if (peerAnchor.equals((Object)this.getBlockPos())) {
            return false;
        }
        return !this.anchorPeerCurves.containsKey(peerAnchor) && this.legCount() < 2;
    }

    public Map<BlockPos, BezierConnection> getAnchorPeerCurvesView() {
        return Map.copyOf(this.anchorPeerCurves);
    }

    public Map<BlockPos, BezierConnection> viewAnchorPeerCurvesSnapshot() {
        return Map.copyOf(this.anchorPeerCurves);
    }

    public void rewriteNeighborPositionsAfterAssemblyTransform(SubLevelAssemblyHelper.AssemblyTransform asm) {
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        this.rewriteChainLiftAfterAssemblyTransform(asm);
        if (this.anchorPeerCurves.isEmpty() && this.anchorPeerCurveRailDiffuseRgb.isEmpty() && this.anchorPeerCurveBeamDiffuseRgb.isEmpty()) {
            return;
        }
        HashMap<BlockPos, BezierConnection> snap = new HashMap<BlockPos, BezierConnection>(this.anchorPeerCurves);
        this.anchorPeerCurves.clear();
        for (Map.Entry e : snap.entrySet()) {
            this.anchorPeerCurves.put(asm.apply((BlockPos)e.getKey()).immutable(), (BezierConnection)e.getValue());
        }
        HashMap<BlockPos, Integer> tintSnapRail = new HashMap<BlockPos, Integer>(this.anchorPeerCurveRailDiffuseRgb);
        HashMap<BlockPos, Integer> tintSnapBeam = new HashMap<BlockPos, Integer>(this.anchorPeerCurveBeamDiffuseRgb);
        this.anchorPeerCurveRailDiffuseRgb.clear();
        this.anchorPeerCurveBeamDiffuseRgb.clear();
        for (Map.Entry e : tintSnapRail.entrySet()) {
            this.anchorPeerCurveRailDiffuseRgb.put(asm.apply((BlockPos)e.getKey()).immutable(), (Integer)e.getValue());
        }
        for (Map.Entry e : tintSnapBeam.entrySet()) {
            this.anchorPeerCurveBeamDiffuseRgb.put(asm.apply((BlockPos)e.getKey()).immutable(), (Integer)e.getValue());
        }
        this.setChanged();
        this.notifyClientSync();
    }

    private void rewriteChainLiftAfterAssemblyTransform(SubLevelAssemblyHelper.AssemblyTransform asm) {
        Direction.Axis rotated;
        StructureTransform structure;
        if (this.chainLiftShaftAxis == null && this.chainLiftEndRef == null) {
            return;
        }
        boolean changed = false;
        if (this.chainLiftEndRef != null) {
            this.chainLiftEndRef = this.chainLiftEndRef.applyAssemblyTransform(asm);
            if (!this.chainLiftFirstHop.equals((Object)BlockPos.ZERO)) {
                this.chainLiftFirstHop = asm.apply(this.chainLiftFirstHop).immutable();
            }
            changed = true;
        }
        if (this.chainLiftShaftAxis != null && (structure = CoasterBezierAssemblyRotationFix.yAxisStructureRotation(asm)) != null && (rotated = ChainLiftShaftAxis.rotateStoredAxis(structure, this.chainLiftShaftAxis)) != this.chainLiftShaftAxis) {
            this.chainLiftShaftAxis = rotated;
            changed = true;
        }
        if (changed) {
            this.setChanged();
            this.notifyClientSync();
        }
        if (this.hasChainLiftShaft()) {
            this.reattachKineticAfterAssemblyMove();
        }
    }

    public void prepareKineticForAssemblyMove() {
        if (this.level == null || this.level.isClientSide() || !this.hasChainLiftShaft()) {
            return;
        }
        this.detachKinetics();
        this.clearKineticInformation();
        this.warnOfMovement();
    }

    public void reattachKineticAfterAssemblyMove() {
        if (this.level == null || this.level.isClientSide() || !this.hasChainLiftShaft()) {
            return;
        }
        this.detachKinetics();
        this.clearKineticInformation();
        this.attachKinetics();
        this.setChanged();
        this.notifyClientSync();
    }

    public int getPeerCurveRailDiffuseRgb(BlockPos peerEndpoint) {
        return this.anchorPeerCurveRailDiffuseRgb.getOrDefault(peerEndpoint, 0);
    }

    public int getPeerCurveBeamDiffuseRgb(BlockPos peerEndpoint) {
        return this.anchorPeerCurveBeamDiffuseRgb.getOrDefault(peerEndpoint, 0);
    }

    public boolean setPeerCurveTieredTints(BlockPos peerEndpoint, int railRgb, int beamRgb) {
        BlockPos key = peerEndpoint.immutable();
        int rail = railRgb & 0xFFFFFF;
        int beam = beamRgb & 0xFFFFFF;
        int prevRail = this.anchorPeerCurveRailDiffuseRgb.getOrDefault(key, 0);
        int prevBeam = this.anchorPeerCurveBeamDiffuseRgb.getOrDefault(key, 0);
        if (prevRail == rail && prevBeam == beam) {
            return false;
        }
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

    public long anchorCurveTintSignature() {
        if (this.anchorPeerCurveRailDiffuseRgb.isEmpty() && this.anchorPeerCurveBeamDiffuseRgb.isEmpty()) {
            return 0L;
        }
        long h = -3750763034362895579L;
        HashSet<BlockPos> peerSet = new HashSet<BlockPos>();
        peerSet.addAll(this.anchorPeerCurveRailDiffuseRgb.keySet());
        peerSet.addAll(this.anchorPeerCurveBeamDiffuseRgb.keySet());
        ArrayList<BlockPos> peers = new ArrayList<BlockPos>(peerSet);
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
        this.setJointBankRadians(0.0f);
    }

    public boolean hasLift() {
        return !Float.isNaN(this.liftBlocks);
    }

    public float getStoredLiftBlocks() {
        return this.liftBlocks;
    }

    public float getLiftBlocks() {
        return Float.isNaN(this.liftBlocks) ? 0.5f : this.liftBlocks;
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
        return Float.isNaN(this.openEndExtensionBlocks) ? 0.5f : this.openEndExtensionBlocks;
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
            return;
        }
        if (this.legCount() != 1) {
            this.clearOpenEndExtension();
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
        HashSet<BlockPos> anchorPeers = new HashSet<BlockPos>(this.anchorPeerCurves.keySet());
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
            BlockEntity blockEntity = level.getBlockEntity(peer);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity p = (CoasterAnchorpointBlockEntity)blockEntity;
            p.dropReciprocalPeerCurve(this.getBlockPos());
        }
        ArrayList<BlockPos> touch = new ArrayList<BlockPos>();
        touch.add(this.getBlockPos());
        touch.addAll(anchorPeers);
        CoasterTrackPropagator.touch(level, touch);
        this.syncAnchorVisualBlockState();
        this.setChanged();
        this.notifyClientSync();
    }

    public void overwriteReciprocalForPeerCurve(BlockPos anchorHostingPrimaryTowardHere, BezierConnection primaryTowardThis) {
        ServerLevel sl;
        if (!primaryTowardThis.isPrimary()) {
            return;
        }
        this.anchorPeerCurves.put(anchorHostingPrimaryTowardHere.immutable(), primaryTowardThis.secondary());
        this.setChanged();
        this.notifyClientSync();
        Level level = this.level;
        if (level instanceof ServerLevel && (level = (sl = (ServerLevel)level).getBlockEntity(anchorHostingPrimaryTowardHere)) instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity owner = (CoasterAnchorpointBlockEntity)level;
            owner.setChanged();
            owner.notifyClientSync();
        }
    }

    public void putAnchorPeerCurve(ServerLevel level, BlockPos peerAnchor, BezierConnection curveTowardPeer) {
        BezierConnection primary;
        BlockPos peerKey = peerAnchor.immutable();
        BezierConnection previous = this.anchorPeerCurves.get(peerKey);
        this.anchorPeerCurves.put(peerKey, curveTowardPeer);
        BlockEntity blockEntity = level.getBlockEntity(peerAnchor);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity peer = (CoasterAnchorpointBlockEntity)blockEntity;
            BezierConnection reciprocal = curveTowardPeer.isPrimary() ? curveTowardPeer.secondary() : curveTowardPeer;
            peer.anchorPeerCurves.put(this.getBlockPos().immutable(), reciprocal);
            peer.setChanged();
            peer.notifyClientSync();
            peer.syncAnchorVisualBlockStateDeferred();
        }
        BezierConnection bezierConnection = primary = curveTowardPeer.isPrimary() ? curveTowardPeer : curveTowardPeer.secondary();
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
        if (CoasterTrackPropagator.scheduleVisualBlockStateSync(this.getBlockPos())) {
            return;
        }
        this.syncAnchorVisualBlockState();
    }

    private void queueJunctionVisualRefresh(ServerLevel level) {
        this.queueJunctionVisualRefresh(level, BlockPos.ZERO);
    }

    private void queueJunctionVisualRefresh(ServerLevel level, BlockPos ... extraAnchorEndpoints) {
        if (!level.isClientSide()) {
            return;
        }
        LinkedHashSet<BlockPos> anchorEndpoints = new LinkedHashSet<BlockPos>();
        anchorEndpoints.add(this.getBlockPos());
        for (BlockPos extra : extraAnchorEndpoints) {
            if (extra == null || extra.equals((Object)BlockPos.ZERO)) continue;
            anchorEndpoints.add(extra.immutable());
        }
        for (BezierConnection bc : this.anchorPeerCurves.values()) {
            if (bc == null) continue;
            BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
            anchorEndpoints.add((BlockPos)p.bePositions.getFirst());
            anchorEndpoints.add((BlockPos)p.bePositions.getSecond());
        }
        AnchorJunctionVisualRefresh.refreshAround((Level)level, (BlockPos[])anchorEndpoints.toArray(BlockPos[]::new));
    }

    public void removeAnchorPeerCurve(ServerLevel level, BlockPos peerAnchor) {
        this.anchorPeerCurveRailDiffuseRgb.remove(peerAnchor);
        this.anchorPeerCurveBeamDiffuseRgb.remove(peerAnchor);
        BezierConnection removed = this.anchorPeerCurves.remove(peerAnchor);
        LinkedHashSet<BlockPos> junctionRefreshSeeds = new LinkedHashSet<BlockPos>();
        junctionRefreshSeeds.add(this.getBlockPos());
        junctionRefreshSeeds.add(peerAnchor);
        if (removed != null) {
            BezierConnection p = removed.isPrimary() ? removed : removed.secondary();
            junctionRefreshSeeds.add((BlockPos)p.bePositions.getFirst());
            junctionRefreshSeeds.add((BlockPos)p.bePositions.getSecond());
        }
        for (BezierConnection bc : this.anchorPeerCurves.values()) {
            if (bc == null) continue;
            BezierConnection rem = bc.isPrimary() ? bc : bc.secondary();
            junctionRefreshSeeds.add((BlockPos)rem.bePositions.getFirst());
            junctionRefreshSeeds.add((BlockPos)rem.bePositions.getSecond());
        }
        this.reconcileLegDerivedState();
        BlockEntity blockEntity = level.getBlockEntity(peerAnchor);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity peer = (CoasterAnchorpointBlockEntity)blockEntity;
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
        this.queueJunctionVisualRefresh(level, (BlockPos[])junctionRefreshSeeds.toArray(BlockPos[]::new));
    }

    public static Vec3 worldCenter(Level level, BlockPos anchorPos) {
        Vec3 base = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return base;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        float lift = be.getLiftBlocks();
        if (Math.abs(lift) < 1.0E-9f) {
            return base;
        }
        Vec3 dir = CoasterAnchorpointBlockEntity.anchorLiftDirection(level, anchorPos);
        return base.add(dir.scale((double)lift));
    }

    public static Vec3 worldCenterBase(Level level, BlockPos anchorPos) {
        BlockState state = level.getBlockState(anchorPos);
        Direction facing = state.hasProperty((Property)CoasterAnchorpointBlock.FACING) ? (Direction)state.getValue((Property)CoasterAnchorpointBlock.FACING) : Direction.UP;
        Vec3 local = switch (facing) {
            default -> throw new MatchException(null, null);
            case Direction.UP -> new Vec3(0.5, 0.25, 0.5);
            case Direction.DOWN -> new Vec3(0.5, 0.75, 0.5);
            case Direction.NORTH -> new Vec3(0.5, 0.5, 0.75);
            case Direction.SOUTH -> new Vec3(0.5, 0.5, 0.25);
            case Direction.WEST -> new Vec3(0.75, 0.5, 0.5);
            case Direction.EAST -> new Vec3(0.25, 0.5, 0.5);
        };
        return Vec3.atLowerCornerOf((Vec3i)anchorPos).add(local).add(0.0, 0.0, 0.0);
    }

    public static Vec3 worldCenterWithLift(Level level, BlockPos anchorPos, float liftBlocks) {
        Vec3 base = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
        if (Math.abs(liftBlocks) < 1.0E-9f) {
            return base;
        }
        return base.add(CoasterAnchorpointBlockEntity.anchorLiftDirection(level, anchorPos).scale((double)liftBlocks));
    }

    public static Vec3 anchorLiftDirection(Level level, BlockPos anchorPos) {
        Vec3 fallback = CoasterAnchorpointBlockEntity.localUp(level, anchorPos);
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return fallback;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
        if (incident.isEmpty()) {
            return fallback;
        }
        Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), anchorPos);
        if (t == null) {
            return fallback;
        }
        Vec3 face = CoasterTrackPlacement.anchorFaceNormalForAxis(level, anchorPos, t);
        if (face.lengthSqr() < 1.0E-12) {
            return fallback;
        }
        return face.normalize();
    }

    public static Vec3 localUp(Level level, BlockPos anchorPos) {
        BlockState state = level.getBlockState(anchorPos);
        Direction facing = state.hasProperty((Property)CoasterAnchorpointBlock.FACING) ? (Direction)state.getValue((Property)CoasterAnchorpointBlock.FACING) : Direction.UP;
        return Vec3.atLowerCornerOf((Vec3i)facing.getNormal()).normalize();
    }

    @Nullable
    public AnchorEndpointTowardPeer endpointTowardPeer(Level level, BlockPos peerAnchor) {
        return CoasterAnchorpointBlockEntity.endpointTowardPeerAt(level, this.getBlockPos(), peerAnchor);
    }

    @Nullable
    public static AnchorEndpointTowardPeer endpointTowardPeerAt(Level level, BlockPos selfPos, BlockPos peerAnchor) {
        Vec3 centerSelf = CoasterAnchorpointBlockEntity.worldCenter(level, selfPos);
        Vec3 towardPeer = CoasterAnchorpointBlockEntity.worldCenter(level, peerAnchor).subtract(centerSelf);
        if (towardPeer.lengthSqr() < 1.0E-12) {
            return null;
        }
        Vec3 axisInto = towardPeer.normalize();
        Vec3 anchorNormal = CoasterTrackPlacement.anchorFaceNormalForAxis(level, selfPos, axisInto);
        return new AnchorEndpointTowardPeer(centerSelf, axisInto, anchorNormal);
    }

    private static Vec3 projectPerpendicular(Vec3 v, Vec3 axis) {
        Vec3 a = axis.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axis.normalize();
        return v.subtract(a.scale(a.dot(v)));
    }

    private static Vec3 orthonormalToAxis(Vec3 normal, Vec3 axis) {
        Vec3 n = CoasterAnchorpointBlockEntity.projectPerpendicular(normal, axis);
        if (n.lengthSqr() < 1.0E-10) {
            n = CoasterAnchorpointBlockEntity.projectPerpendicular(new Vec3(1.0, 0.0, 0.0), axis);
        }
        if (n.lengthSqr() < 1.0E-10) {
            n = CoasterAnchorpointBlockEntity.projectPerpendicular(new Vec3(0.0, 0.0, 1.0), axis);
        }
        return n.normalize();
    }

    private void notifyClientSync() {
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        if (CoasterTrackPropagator.scheduleBlockSyncIfBatching(this.getBlockPos())) {
            return;
        }
        this.notifyUpdate();
    }

    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        ListTag anchorCurveList = new ListTag();
        for (Map.Entry<BlockPos, BezierConnection> e : this.anchorPeerCurves.entrySet()) {
            Couple<Double> hl;
            BezierConnection p;
            CompoundTag c = new CompoundTag();
            c.putLong("Peer", e.getKey().asLong());
            c.put("Bezier", (Tag)e.getValue().write(this.getBlockPos()));
            BezierConnection raw = e.getValue();
            BezierConnection bezierConnection = p = raw.isPrimary() ? raw : raw.secondary();
            if (CoasterTrackMaterials.COASTER.id.equals((Object)p.getMaterial().id) && (hl = CoasterPeerCurveHandleLengths.getCanonicalLengths(p)) != null) {
                c.putDouble("HLo", ((Double)hl.getFirst()).doubleValue());
                c.putDouble("HHi", ((Double)hl.getSecond()).doubleValue());
            }
            anchorCurveList.add((Object)c);
        }
        tag.put("AnchorPeerCurves", (Tag)anchorCurveList);
        ListTag tintList = new ListTag();
        HashSet<BlockPos> tintPeers = new HashSet<BlockPos>();
        tintPeers.addAll(this.anchorPeerCurveRailDiffuseRgb.keySet());
        tintPeers.addAll(this.anchorPeerCurveBeamDiffuseRgb.keySet());
        for (BlockPos peer : tintPeers) {
            int r = this.anchorPeerCurveRailDiffuseRgb.getOrDefault(peer, 0);
            int b = this.anchorPeerCurveBeamDiffuseRgb.getOrDefault(peer, 0);
            if (r == 0 && b == 0) continue;
            CompoundTag c = new CompoundTag();
            c.putLong("Peer", peer.asLong());
            c.putInt("RailRGB", r);
            c.putInt("BeamRGB", b);
            tintList.add((Object)c);
        }
        tag.put("AnchorPeerCurveTints", (Tag)tintList);
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
            tag.put("ChainLiftSpan", (Tag)span);
            tag.putDouble("ChainLiftCommittedLength", this.chainLiftCommittedLength);
            tag.putLong("ChainLiftFirstHop", this.chainLiftFirstHop.asLong());
            if (!this.chainLiftCommittedSegments.isEmpty()) {
                tag.put("ChainLiftCommittedSegments", (Tag)ChainLiftCommittedSegment.writeList(this.chainLiftCommittedSegments));
            }
            if (this.chainLiftTravelSign != 0) {
                tag.putInt("ChainLiftTravelSign", this.chainLiftTravelSign);
            }
        }
        super.write(tag, registries, clientPacket);
    }

    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        this.anchorPeerCurves.clear();
        ListTag anchorCurveList = tag.getList("AnchorPeerCurves", 10);
        for (Tag value : anchorCurveList) {
            CompoundTag c;
            if (!(value instanceof CompoundTag) || !(c = (CompoundTag)value).contains("Peer", 4) || !c.contains("Bezier", 10)) continue;
            BlockPos peer = BlockPos.of((long)c.getLong("Peer"));
            BezierConnection bc = new BezierConnection(c.getCompound("Bezier"), this.getBlockPos());
            this.anchorPeerCurves.put(peer, bc);
            if (!c.contains("HLo", 6) || !c.contains("HHi", 6)) continue;
            BezierConnection pri = bc.isPrimary() ? bc : bc.secondary();
            CoasterPeerCurveHandleLengths.putCanonical(pri, c.getDouble("HLo"), c.getDouble("HHi"));
        }
        this.jointBankRadians = tag.contains("JointBank", 5) ? tag.getFloat("JointBank") : 0.0f;
        this.liftBlocks = tag.contains("Lift", 5) ? tag.getFloat("Lift") : Float.NaN;
        this.openEndExtensionBlocks = tag.contains("OpenEndExtension", 5) ? tag.getFloat("OpenEndExtension") : Float.NaN;
        Direction.Axis axis = this.chainLiftShaftAxis = tag.contains("ChainLiftShaftAxis", 8) ? Direction.Axis.byName((String)tag.getString("ChainLiftShaftAxis")) : null;
        if (tag.contains("ChainLiftSpan", 10)) {
            this.chainLiftEndRef = ChainLiftEndRef.read(tag.getCompound("ChainLiftSpan"));
            this.chainLiftCommittedLength = tag.contains("ChainLiftCommittedLength", 6) ? tag.getDouble("ChainLiftCommittedLength") : 0.0;
            this.chainLiftFirstHop = tag.contains("ChainLiftFirstHop", 4) ? BlockPos.of((long)tag.getLong("ChainLiftFirstHop")) : BlockPos.ZERO;
            this.chainLiftCommittedSegments = tag.contains("ChainLiftCommittedSegments", 9) ? ChainLiftCommittedSegment.readList(tag.getList("ChainLiftCommittedSegments", 10)) : List.of();
            this.chainLiftTravelSign = tag.contains("ChainLiftTravelSign", 3) ? tag.getInt("ChainLiftTravelSign") : 0;
        } else {
            this.clearChainLiftSpanData();
        }
        if (this.level != null) {
            ChainLiftClientIndex.refreshMembership(this);
        }
        this.anchorPeerCurveRailDiffuseRgb.clear();
        this.anchorPeerCurveBeamDiffuseRgb.clear();
        ListTag tintList = tag.getList("AnchorPeerCurveTints", 10);
        for (Tag value : tintList) {
            int beam;
            int rail;
            CompoundTag c;
            if (!(value instanceof CompoundTag) || !(c = (CompoundTag)value).contains("Peer", 4)) continue;
            BlockPos peer = BlockPos.of((long)c.getLong("Peer"));
            if (c.contains("RailRGB", 3) || c.contains("BeamRGB", 3)) {
                rail = c.getInt("RailRGB") & 0xFFFFFF;
                beam = c.getInt("BeamRGB") & 0xFFFFFF;
            } else {
                if (!c.contains("RGB", 3)) continue;
                rail = c.getInt("RGB") & 0xFFFFFF;
                beam = 0;
            }
            if (rail != 0) {
                this.anchorPeerCurveRailDiffuseRgb.put(peer, rail);
            }
            if (beam == 0) continue;
            this.anchorPeerCurveBeamDiffuseRgb.put(peer, beam);
        }
        AnchorPeerCurveClientIndex.refreshMembership(this);
        super.read(tag, registries, clientPacket);
    }

    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public void reloadCurveDataForPonder(CompoundTag tag, HolderLookup.Provider registries) {
        this.read(tag, registries, false);
    }

    public void repairAnchorPeerCurveKeys() {
        if (this.anchorPeerCurves.isEmpty()) {
            return;
        }
        HashMap<BlockPos, BezierConnection> rebuilt = new HashMap<BlockPos, BezierConnection>();
        HashMap<BlockPos, BlockPos> peerRemap = new HashMap<BlockPos, BlockPos>();
        BlockPos self = this.getBlockPos();
        for (Map.Entry<BlockPos, BezierConnection> e : this.anchorPeerCurves.entrySet()) {
            BezierConnection bc = e.getValue();
            if (bc == null) continue;
            BlockPos oldPeer = e.getKey();
            BlockPos peer = CoasterAnchorpointBlockEntity.peerFromBezierEndpoints(self, oldPeer, bc);
            rebuilt.put(peer.immutable(), bc);
            if (oldPeer.equals((Object)peer)) continue;
            peerRemap.put(oldPeer.immutable(), peer.immutable());
        }
        this.anchorPeerCurves.clear();
        this.anchorPeerCurves.putAll(rebuilt);
        this.repairAnchorPeerCurveTintKeys(peerRemap);
    }

    private void repairAnchorPeerCurveTintKeys(Map<BlockPos, BlockPos> peerRemap) {
        BlockPos key;
        HashMap<BlockPos, Integer> rebuilt;
        if (!this.anchorPeerCurveRailDiffuseRgb.isEmpty()) {
            rebuilt = new HashMap<BlockPos, Integer>();
            for (Map.Entry<BlockPos, Integer> e : new HashMap<BlockPos, Integer>(this.anchorPeerCurveRailDiffuseRgb).entrySet()) {
                key = peerRemap.getOrDefault(e.getKey(), e.getKey());
                if (!this.anchorPeerCurves.containsKey(key)) continue;
                rebuilt.put(key.immutable(), e.getValue());
            }
            this.anchorPeerCurveRailDiffuseRgb.clear();
            this.anchorPeerCurveRailDiffuseRgb.putAll(rebuilt);
        }
        if (!this.anchorPeerCurveBeamDiffuseRgb.isEmpty()) {
            rebuilt = new HashMap();
            for (Map.Entry<BlockPos, Integer> e : new HashMap<BlockPos, Integer>(this.anchorPeerCurveBeamDiffuseRgb).entrySet()) {
                key = peerRemap.getOrDefault(e.getKey(), e.getKey());
                if (!this.anchorPeerCurves.containsKey(key)) continue;
                rebuilt.put(key.immutable(), e.getValue());
            }
            this.anchorPeerCurveBeamDiffuseRgb.clear();
            this.anchorPeerCurveBeamDiffuseRgb.putAll(rebuilt);
        }
    }

    private static BlockPos peerFromBezierEndpoints(BlockPos self, BlockPos peerFromTag, BezierConnection bc) {
        BlockPos p0 = (BlockPos)bc.bePositions.getFirst();
        BlockPos p1 = (BlockPos)bc.bePositions.getSecond();
        Object peerFromGeometry = self.equals((Object)p0) ? p1 : (self.equals((Object)p1) ? p0 : null);
        if (peerFromGeometry == null) {
            return peerFromTag;
        }
        if (!peerFromTag.equals((Object)p0) && !peerFromTag.equals((Object)p1)) {
            return peerFromGeometry;
        }
        if (peerFromTag.equals((Object)self)) {
            return peerFromGeometry;
        }
        return peerFromTag;
    }

    public record AnchorEndpointTowardPeer(Vec3 worldStart, Vec3 axisIntoCurve, Vec3 normal) {
    }
}

