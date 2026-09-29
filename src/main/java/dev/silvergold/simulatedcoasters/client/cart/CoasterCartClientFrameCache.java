package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.compat.PhysicsStaffClientDragProbe;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartRailSnapEligibility;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackSnap;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartClientFrameCache {
   private static long tickScopeGameTime = Long.MIN_VALUE;
   private static final Map<UUID, CoasterPathTrackFrame.GraphHit> ENGAGEMENT_HIT_BY_SUB = new HashMap<>();
   private static final Map<UUID, Boolean> SUBLEVEL_HOSTED_TRACK = new HashMap<>();
   private static final Map<UUID, CoasterCartClientFrameCache.SoundFrame> SOUND_BY_SUB = new HashMap<>();
   private static long renderScopeGameTime = Long.MIN_VALUE;
   private static int renderScopePartialBits = Integer.MIN_VALUE;
   private static final Map<UUID, CoasterCartClientFrameCache.RenderFrame> RENDER_BY_SUB = new HashMap<>();

   private CoasterCartClientFrameCache() {
   }

   static void clearSubLevelTrackAffinity(UUID id) {
      SUBLEVEL_HOSTED_TRACK.remove(id);
   }

   public static void beginClientTick(ClientLevel level) {
      long tick = level.getGameTime();
      if (tick != tickScopeGameTime) {
         tickScopeGameTime = tick;
         ENGAGEMENT_HIT_BY_SUB.clear();
         SOUND_BY_SUB.clear();
         RENDER_BY_SUB.clear();
         renderScopeGameTime = Long.MIN_VALUE;
         renderScopePartialBits = Integer.MIN_VALUE;
      }
   }

   public static void beginRenderFrame(ClientLevel level, float partialTick) {
      long tick = level.getGameTime();
      int ptBits = Float.floatToIntBits(partialTick);
      if (tick != renderScopeGameTime || ptBits != renderScopePartialBits) {
         renderScopeGameTime = tick;
         renderScopePartialBits = ptBits;
         RENDER_BY_SUB.clear();
      }
   }

   @Nullable
   public static CoasterCartClientFrameCache.SoundFrame resolveSound(
      ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs
   ) {
      if (sub.isRemoved()) {
         CoasterCartClientEngagement.clearSubState(sub.getUniqueId());
         CoasterCartStaffStretchRenderGate.clearSub(sub.getUniqueId());
         return null;
      } else {
         BlockPos rep = CoasterCartTrackSnap.representativeBearingPlotPos(sub);
         return rep == null ? null : SOUND_BY_SUB.computeIfAbsent(sub.getUniqueId(), id -> computeSound(level, sub, pathGraph, openEndRefs, rep));
      }
   }

   @Nullable
   public static CoasterCartClientFrameCache.RenderFrame resolveRender(
      ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs, float partialTick
   ) {
      if (sub.isRemoved()) {
         CoasterCartClientEngagement.clearSubState(sub.getUniqueId());
         CoasterCartStaffStretchRenderGate.clearSub(sub.getUniqueId());
         return null;
      } else {
         BlockPos rep = CoasterCartTrackSnap.representativeBearingPlotPos(sub);
         return rep == null
            ? null
            : RENDER_BY_SUB.computeIfAbsent(sub.getUniqueId(), id -> computeRender(level, sub, pathGraph, openEndRefs, rep, partialTick));
      }
   }

   private static CoasterCartClientFrameCache.SoundFrame computeSound(
      ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs, BlockPos rep
   ) {
      Vec3 bearingWorld = sub.logicalPose().transformPosition(Vec3.atCenterOf(rep));
      if (PhysicsStaffClientDragProbe.isPhysicsStaffLocked(level, sub)) {
         CoasterCartClientEngagement.forceDisengaged(sub.getUniqueId());
         return new CoasterCartClientFrameCache.SoundFrame(bearingWorld, false);
      } else if (CoasterCartClientEngagement.isSnapEngagedBlock(level, rep)) {
         return new CoasterCartClientFrameCache.SoundFrame(bearingWorld, true);
      } else {
         CoasterPathTrackFrame.GraphHit hit = engagementHitForTick(level, sub, pathGraph, bearingWorld);
         CoasterCartClientEngagement.applyEngagementState(level, pathGraph, sub, sub.getUniqueId(), hit, openEndRefs);
         boolean onRail = hit != null && !CoasterCartTrackSnap.shouldDisengageAtOpenEndHit(level, pathGraph, hit, openEndRefs);
         return new CoasterCartClientFrameCache.SoundFrame(bearingWorld, onRail);
      }
   }

   private static CoasterCartClientFrameCache.RenderFrame computeRender(
      ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs, BlockPos rep, float partialTick
   ) {
      Vec3 plotCenter = Vec3.atCenterOf(rep);
      Vec3 sampleLogical = sub.logicalPose().transformPosition(plotCenter);
      Pose3d visualPose = clientVisualPose(sub, partialTick);
      Vec3 sampleVisual = visualPose.transformPosition(plotCenter);
      boolean snapEngaged = isSnapEngagedBlock(level, rep);
      boolean staffFrozen = PhysicsStaffClientDragProbe.isPhysicsStaffLocked(level, sub);
      boolean staffGrabbing = PhysicsStaffClientDragProbe.isAnyPhysicsStaffGrabbing(level, sub);
      boolean staffStretchRendering = !staffFrozen && CoasterCartStaffStretchRenderGate.stretchRendering(level, sub, snapEngaged, staffGrabbing);
      boolean clientEngaged;
      if (staffFrozen) {
         CoasterCartClientEngagement.forceDisengaged(sub.getUniqueId());
         clientEngaged = false;
      } else if (snapEngaged) {
         clientEngaged = true;
      } else {
         clientEngaged = CoasterCartClientEngagement.isClientEngaged(sub.getUniqueId());
      }

      CoasterPathTrackFrame.GraphHit trackHit = null;
      CoasterPathTrackFrame.TrackBasis trackBasis = null;
      Vec3 pathAnchorBottom = null;
      Quaternionf qPath = null;
      if (staffStretchRendering && !pathGraph.edges().isEmpty()) {
         double trackSearchSq = 2.25;
         trackHit = CoasterPathTrackFrame.nearestGraphHit(level, pathGraph, sampleVisual, trackSearchSq, (double)partialTick);
         if (trackHit == null) {
            trackHit = CoasterPathTrackFrame.nearestGraphHit(level, pathGraph, sampleLogical, trackSearchSq);
         }

         trackHit = CoasterCartRailSnapEligibility.filterHitForBearing(sub, level, trackHit);
         if (trackHit != null) {
            trackBasis = CoasterPathTrackFrame.trackBasisAtHit(level, trackHit, partialTick);
            pathAnchorBottom = CoasterPathTrackFrame.pathAnchorForSnappedBlock(level, trackHit, partialTick);
            Quaterniond qd = CoasterPathTrackFrame.quaternionFromTrackBasis(trackBasis);
            qPath = new Quaternionf((float)qd.x, (float)qd.y, (float)qd.z, (float)qd.w);
         }
      }

      Quaterniond qVisual = visualPose.orientation();
      Quaternionf qBody = new Quaternionf((float)qVisual.x, (float)qVisual.y, (float)qVisual.z, (float)qVisual.w);
      return new CoasterCartClientFrameCache.RenderFrame(
         rep, visualPose, sampleLogical, sampleVisual, snapEngaged, clientEngaged, staffStretchRendering, trackHit, trackBasis, pathAnchorBottom, qPath, qBody
      );
   }

   static Pose3d clientVisualPose(SubLevel sub, float partialTick) {
      Pose3d visualPose = new Pose3d();
      if (sub instanceof ClientSubLevel clientSub) {
         Pose3dc rp = clientSub.renderPose(partialTick);
         visualPose.position().set(rp.position());
         visualPose.orientation().set(rp.orientation());
         visualPose.rotationPoint().set(rp.rotationPoint());
         return visualPose;
      } else {
         sub.lastPose().lerp(sub.logicalPose(), (double)partialTick, visualPose);
         return visualPose;
      }
   }

   static Vec3 bodyTravelForwardWorld(Quaternionf bodyOrientation) {
      Vector3f v = new Vector3f(0.0F, 0.0F, -1.0F);
      bodyOrientation.transform(v);
      return new Vec3((double)v.x, (double)v.y, (double)v.z);
   }

   private static CoasterPathTrackFrame.GraphHit engagementHitForTick(ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, Vec3 bearingWorld) {
      UUID id = sub.getUniqueId();
      return Boolean.TRUE.equals(SUBLEVEL_HOSTED_TRACK.get(id))
         ? queryEngagementHit(level, sub, pathGraph, bearingWorld, id)
         : ENGAGEMENT_HIT_BY_SUB.computeIfAbsent(id, i -> queryEngagementHit(level, sub, pathGraph, bearingWorld, id));
   }

   @Nullable
   private static CoasterPathTrackFrame.GraphHit queryEngagementHit(ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, Vec3 bearingWorld, UUID subId) {
      if (pathGraph.edges().isEmpty()) {
         return null;
      } else {
         boolean staffGrabbing = PhysicsStaffClientDragProbe.isAnyPhysicsStaffGrabbing(level, sub);
         double searchSq = CoasterCartClientEngagement.engagementSearchDistSq(subId, staffGrabbing);
         CoasterPathTrackFrame.GraphHit hit = CoasterPathTrackFrame.nearestGraphHit(level, pathGraph, bearingWorld, searchSq);
         hit = CoasterCartRailSnapEligibility.filterHitForBearing(sub, level, hit);
         if (hit != null) {
            SUBLEVEL_HOSTED_TRACK.put(subId, CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge()));
         } else {
            SUBLEVEL_HOSTED_TRACK.remove(subId);
         }

         if (Boolean.TRUE.equals(SUBLEVEL_HOSTED_TRACK.get(subId))) {
            ENGAGEMENT_HIT_BY_SUB.remove(subId);
         }

         return hit;
      }
   }

   private static boolean isSnapEngagedBlock(Level level, BlockPos bearingCell) {
      BlockState st = level.getBlockState(bearingCell);
      CoasterCartBlock bearingBlock = (CoasterCartBlock)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
      return st.is(bearingBlock) && (Boolean)st.getValue(CoasterCartBlock.SNAP_ENGAGED);
   }

   public static record RenderFrame(
      BlockPos representativePlotPos,
      Pose3d visualPose,
      Vec3 sampleLogical,
      Vec3 sampleVisual,
      boolean snapEngaged,
      boolean clientEngaged,
      boolean staffStretchRendering,
      @Nullable CoasterPathTrackFrame.GraphHit trackHit,
      @Nullable CoasterPathTrackFrame.TrackBasis trackBasis,
      @Nullable Vec3 pathAnchorBottom,
      @Nullable Quaternionf qPath,
      Quaternionf qBody
   ) {
      public boolean trackLocked() {
         return this.trackHit != null;
      }

      public boolean drawStaffGlue() {
         return this.staffStretchRendering && this.trackLocked();
      }

      public boolean plotPoseAlignedExtras() {
         return this.snapEngaged && !this.trackLocked();
      }
   }

   public static record SoundFrame(Vec3 bearingWorldLogical, boolean onRail) {
   }
}
