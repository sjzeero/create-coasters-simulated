package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterCurvePickGeometry;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurvePendingRemoval;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.Optional;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;

public final class AnchorPeerCurvePick {
   private static final double RAY_DIST_SQ_EPS = 5.0E-5;
   private static final double RAY_DEPTH_EPS = 0.001;

   private AnchorPeerCurvePick() {
   }

   @Nullable
   private static Pose3d renderPose(ClientSubLevel sub, float pt) {
      return sub == null ? null : (Pose3d)sub.renderPose(pt);
   }

   private static AABB curveBoundsWorld(BezierConnection bc, ClientSubLevel sub, float pt) {
      return sub == null ? bc.getBounds() : new BoundingBox3d(bc.getBounds()).transform(sub.renderPose(pt)).toMojang();
   }

   private static Vec3 curvePickMidWorld(Vec3 plotLocalMid, BlockPos anchorPos, Minecraft mc, float pt) {
      ClientSubLevel sub = (ClientSubLevel)Sable.HELPER.getContaining(mc.level, anchorPos);
      Pose3d rp = renderPose(sub, pt);
      return rp == null ? plotLocalMid : rp.transformPosition(plotLocalMid);
   }

   private static Vec3 anchorCenterForCurveCompare(Level level, BlockPos anchorEp, Minecraft mc, float pt) {
      Vec3 c = CoasterAnchorpointBlockEntity.worldCenter(level, anchorEp);
      ClientSubLevel epSub = (ClientSubLevel)Sable.HELPER.getContaining(mc.level, anchorEp);
      Pose3d rp = renderPose(epSub, pt);
      return rp == null ? c : rp.transformPosition(c);
   }

   private static boolean isCoasterMaterial(BezierConnection bc) {
      return bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
   }

   private static void tryWireTrackTrackCoasterHit(Minecraft mc, @Nullable BezierPointSelection trackSel, double trackDistSq, double blockDistSq) {
      if (trackSel != null) {
         BezierConnection bc = TrackOutlineBezierAccess.primaryBezierForSelection(trackSel);
         if (bc != null && isCoasterMaterial(bc)) {
            if (trackDistSq + 5.0E-5 < blockDistSq) {
               mc.hitResult = BlockHitResult.miss(trackSel.vec(), Direction.UP, BlockPos.ZERO);
            }
         }
      }
   }

   @Nullable
   public static BlockHitResult syntheticMiningHit(Minecraft mc) {
      return syntheticCurveBlockHit(mc, false);
   }

   @Nullable
   public static BlockHitResult syntheticPickBlockHit(Minecraft mc) {
      return syntheticCurveBlockHit(mc, true);
   }

   @Nullable
   private static BlockHitResult syntheticCurveBlockHit(Minecraft mc, boolean allowCreateTrackOutline) {
      if (AnchorPeerCurveHit.isActive() && mc.level != null) {
         BlockPos mine = AnchorPeerCurveHit.rasterBlockPos();
         if (mine == null) {
            mine = AnchorPeerCurveHit.focusAnchor();
         }

         if (mine != null) {
            Vec3 loc = mc.hitResult != null ? mc.hitResult.getLocation() : Vec3.atCenterOf(mine);
            if (AnchorPeerCurveHit.rasterBlockPos() != null && AnchorPeerFakeTracks.isManagedFakeTrack(mc.level, mine)) {
               loc = AnchorPeerFakeTracks.clampInteractionInsideCell(loc, mine);
            }

            return new BlockHitResult(loc, Direction.UP, mine, false);
         }
      }

      if (allowCreateTrackOutline && mc.level != null) {
         BezierPointSelection sel = TrackBlockOutline.result;
         if (sel == null) {
            return null;
         } else {
            BezierConnection bc = TrackOutlineBezierAccess.primaryBezierForSelection(sel);
            if (bc != null && isCoasterMaterial(bc)) {
               BlockPos pos = sel.blockEntity().getBlockPos();
               return new BlockHitResult(sel.vec(), Direction.UP, pos, false);
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static boolean shouldIgnoreBlockHitForCurvePick(Level level, BlockPos hitPos) {
      if (AnchorPeerFakeTracks.isManagedFakeTrack(level, hitPos)) {
         return false;
      } else if (level.getBlockState(hitPos).getBlock() instanceof CoasterAnchorpointBlock
         && level.getBlockEntity(hitPos) instanceof CoasterAnchorpointBlockEntity anchorBe
         && !anchorBe.getAnchorPeerCurvesView().isEmpty()) {
         return true;
      } else {
         BlockPos below = hitPos.below();
         if (level.getBlockEntity(below) instanceof CoasterAnchorpointBlockEntity be && !be.getAnchorPeerCurvesView().isEmpty()) {
            return true;
         }

         return false;
      }
   }

   private static double blockDistSqForCurveCompare(Minecraft mc, Vec3 origin) {
      if (!(mc.hitResult instanceof BlockHitResult bhr) || mc.hitResult.getType() != Type.BLOCK) {
         return Double.POSITIVE_INFINITY;
      }

      return shouldIgnoreBlockHitForCurvePick(mc.level, bhr.getBlockPos())
         ? Double.POSITIVE_INFINITY
         : Sable.HELPER.distanceSquaredWithSubLevels(mc.level, origin, bhr.getLocation());
   }

   public static void refineAfterCreatePass(Minecraft mc) {
      if (mc.cameraEntity instanceof LocalPlayer player && mc.level != null) {
         float pt = AnimationTickHolder.getPartialTicks(mc.level);
         Vec3 origin = Sable.HELPER.getEyePositionInterpolated(player, pt);
         double maxRange = Double.MAX_VALUE;
         if (mc.hitResult != null
            && mc.hitResult.getType() == Type.BLOCK
            && mc.hitResult instanceof BlockHitResult bhr0
            && !shouldIgnoreBlockHitForCurvePick(mc.level, bhr0.getBlockPos())) {
            maxRange = Sable.HELPER.distanceSquaredWithSubLevels(mc.level, origin, bhr0.getLocation());
         } else if (mc.hitResult != null) {
            maxRange = mc.hitResult.getLocation().distanceToSqr(origin);
         }

         double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
         Vec3 target = RaycastHelper.getTraceTarget(player, Math.min(maxRange, range) + 1.0, origin);
         Vec3 ray = target.subtract(origin);
         double rayLen = ray.length();
         if (rayLen < 1.0E-6) {
            return;
         }

         BezierPointSelection trackSel = TrackBlockOutline.result;
         double trackDistSq = trackSel == null ? Double.POSITIVE_INFINITY : origin.distanceToSqr(trackSel.vec());
         double blockDistSq = blockDistSqForCurveCompare(mc, origin);
         double bestAnchorDistSq = Double.POSITIVE_INFINITY;
         BezierConnection bestBc = null;
         Vec3 bestVec = null;
         Vec3 bestAngles = null;
         Vec3 bestDir = null;
         BlockPos bestMidHostAnchorPos = null;

         for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
            if (!anchor.isRemoved() && anchor.getLevel() == mc.level) {
               BlockPos owningAnchorPos = anchor.getBlockPos();
               ClientSubLevel anchorSub = (ClientSubLevel)Sable.HELPER.getContaining(mc.level, owningAnchorPos);
               Pose3d anchorRp = renderPose(anchorSub, pt);

               for (BezierConnection bcRaw : anchor.getAnchorPeerCurvesView().values()) {
                  if (bcRaw != null) {
                     BezierConnection bc = bcRaw.isPrimary() ? bcRaw : bcRaw.secondary();
                     if (!AnchorPeerCurvePendingRemoval.isPending(bc)) {
                        AABB bounds = curveBoundsWorld(bc, anchorSub, pt);
                        if (bounds.contains(origin) || !bounds.clip(origin, target).isEmpty()) {
                           float[] stepLUT = bc.getStepLUT();
                           int segments = (int)(bc.getLength() * 2.0);
                           AABB segmentBounds = isCoasterMaterial(bc)
                              ? CoasterCurvePickGeometry.curvePickCrossSection().bounds()
                              : AllShapes.TRACK_ORTHO.get(Direction.SOUTH).bounds();
                           segmentBounds = segmentBounds.move(-0.5, segmentBounds.getYsize() / -2.0, -0.5);
                           int bestSegment = -1;
                           double bestDistance = Double.MAX_VALUE;
                           double newMaxRange = maxRange;
                           Vec3 pickMid = null;
                           Vec3 pickAngles = null;
                           Vec3 pickDir = null;

                           for (int i = 0; i < stepLUT.length - 2; i++) {
                              float t = stepLUT[i] * (float)i / (float)segments;
                              float t1 = stepLUT[i + 1] * (float)(i + 1) / (float)segments;
                              float t2 = stepLUT[i + 2] * (float)(i + 2) / (float)segments;
                              Vec3 v1 = bc.getPosition((double)t);
                              Vec3 v2 = bc.getPosition((double)t2);
                              Vec3 diff = v2.subtract(v1);
                              Vec3 angles = CoasterBezierRailFrames.modelAnglesForSegment(bc, t1, diff, mc.level);
                              Vec3 mid = v1.add(diff.scale(0.5));
                              if (isCoasterMaterial(bc)) {
                                 mid = mid.add(CoasterBezierRailFrames.curveInteractionLiftOffset(bc, t1, mc.level));
                              }

                              Vec3 localOrigin;
                              Vec3 localDirection;
                              if (anchorRp == null) {
                                 localOrigin = origin.subtract(mid);
                                 localDirection = target.subtract(origin);
                              } else {
                                 Vec3 invO = anchorRp.transformPositionInverse(origin);
                                 Vec3 invT = anchorRp.transformPositionInverse(target);
                                 localOrigin = invO.subtract(mid);
                                 localDirection = invT.subtract(invO);
                              }

                              localOrigin = VecHelper.rotate(localOrigin, (double)AngleHelper.deg(-angles.x), Axis.X);
                              localOrigin = VecHelper.rotate(localOrigin, (double)AngleHelper.deg(-angles.y), Axis.Y);
                              localDirection = VecHelper.rotate(localDirection, (double)AngleHelper.deg(-angles.x), Axis.X);
                              localDirection = VecHelper.rotate(localDirection, (double)AngleHelper.deg(-angles.y), Axis.Y);
                              Optional<Vec3> clip = segmentBounds.clip(localOrigin, localOrigin.add(localDirection));
                              if (!clip.isEmpty() && (bestSegment == -1 || !(bestDistance < clip.get().distanceToSqr(0.0, 0.25, 0.0)))) {
                                 double distanceToSqr = clip.get().distanceToSqr(localOrigin);
                                 if (!(distanceToSqr > maxRange)) {
                                    bestSegment = i;
                                    newMaxRange = distanceToSqr;
                                    bestDistance = clip.get().distanceToSqr(0.0, 0.25, 0.0);
                                    pickMid = mid;
                                    pickAngles = angles;
                                    pickDir = diff.normalize();
                                 }
                              }
                           }

                           if (bestSegment != -1 && pickMid != null && pickAngles != null && pickDir != null) {
                              maxRange = newMaxRange;
                              Vec3 pickMidWorld = curvePickMidWorld(pickMid, owningAnchorPos, mc, pt);
                              double anchorPickDistSq = Sable.HELPER.distanceSquaredWithSubLevels(mc.level, origin, pickMidWorld);
                              if (anchorPickDistSq < bestAnchorDistSq) {
                                 bestAnchorDistSq = anchorPickDistSq;
                                 bestBc = bc;
                                 bestVec = pickMid;
                                 bestAngles = pickAngles;
                                 bestDir = pickDir;
                                 bestMidHostAnchorPos = owningAnchorPos;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (bestBc != null
            && bestVec != null
            && bestAngles != null
            && bestDir != null
            && bestMidHostAnchorPos != null
            && !AnchorPeerCurvePendingRemoval.isPending(bestBc)) {
            if (bestAnchorDistSq + 5.0E-5 < blockDistSq && bestAnchorDistSq + 5.0E-5 < trackDistSq) {
               Vec3 bestMidWorld = curvePickMidWorld(bestVec, bestMidHostAnchorPos, mc, pt);
               if (mc.hitResult instanceof BlockHitResult bhrMc
                  && mc.hitResult.getType() == Type.BLOCK
                  && !shouldIgnoreBlockHitForCurvePick(mc.level, bhrMc.getBlockPos())) {
                  double blockT = Math.max(0.0, bhrMc.getLocation().subtract(origin).dot(ray) / rayLen);
                  double anchorT = Math.max(0.0, bestMidWorld.subtract(origin).dot(ray) / rayLen);
                  if (anchorT >= blockT - 0.001) {
                     AnchorPeerCurveHit.clear();
                     tryWireTrackTrackCoasterHit(mc, trackSel, trackDistSq, blockDistSq);
                     return;
                  }
               }

               BlockPos first = (BlockPos)bestBc.bePositions.getFirst();
               BlockPos second = (BlockPos)bestBc.bePositions.getSecond();
               Level level = mc.level;
               Vec3 c1w = anchorCenterForCurveCompare(level, first, mc, pt);
               Vec3 c2w = anchorCenterForCurveCompare(level, second, mc, pt);
               BlockPos focus = bestMidWorld.distanceToSqr(c1w) <= bestMidWorld.distanceToSqr(c2w) ? first : second;
               BlockPos hitBlock = AnchorPeerFakeTracks.blockPosForCurveHit(bestBc, bestMidWorld);
               if (hitBlock == null) {
                  hitBlock = focus;
               }

               BlockPos rasterBlock = AnchorPeerFakeTracks.rasterContains(bestBc, hitBlock) ? hitBlock : null;
               TrackBlockOutline.result = null;
               AnchorPeerCurveHit.set(bestBc, bestVec, bestAngles, bestDir, focus, rasterBlock);
               mc.hitResult = BlockHitResult.miss(bestMidWorld, Direction.UP, BlockPos.ZERO);
               return;
            }

            AnchorPeerCurveHit.clear();
            tryWireTrackTrackCoasterHit(mc, trackSel, trackDistSq, blockDistSq);
            return;
         }

         AnchorPeerCurveHit.clear();
         tryWireTrackTrackCoasterHit(mc, trackSel, trackDistSq, blockDistSq);
         return;
      }
   }
}
