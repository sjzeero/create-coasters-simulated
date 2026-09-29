package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.FakeTrackBlock;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class AnchorPeerFakeTracks {
   private AnchorPeerFakeTracks() {
   }

   public static boolean rasterContains(BezierConnection bc, BlockPos worldPos) {
      BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
      BlockPos base = (BlockPos)primary.bePositions.getFirst();
      Map<Pair<Integer, Integer>, Double> yLevels = primary.rasterise();

      for (Entry<Pair<Integer, Integer>, Double> entry : yLevels.entrySet()) {
         double yValue = entry.getValue();
         int floor = Mth.floor(yValue);
         BlockPos targetPos = new BlockPos((Integer)entry.getKey().getFirst(), floor, (Integer)entry.getKey().getSecond()).offset(base).above(1);
         if (targetPos.equals(worldPos)) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public static BlockPos blockPosForCurveHit(BezierConnection bc, Vec3 worldHit) {
      BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
      Map<Pair<Integer, Integer>, Double> yLevels = primary.rasterise();
      if (yLevels.isEmpty()) {
         return null;
      } else {
         BlockPos base = (BlockPos)primary.bePositions.getFirst();
         BlockPos bestPos = null;
         double bestDistSq = Double.MAX_VALUE;

         for (Entry<Pair<Integer, Integer>, Double> entry : yLevels.entrySet()) {
            double yValue = entry.getValue();
            int floor = Mth.floor(yValue);
            BlockPos targetPos = new BlockPos((Integer)entry.getKey().getFirst(), floor, (Integer)entry.getKey().getSecond()).offset(base).above(1);
            AABB box = unitBlockBounds(targetPos);
            double d = distSqPointToAabb(worldHit, box);
            if (d < bestDistSq) {
               bestDistSq = d;
               bestPos = targetPos;
            }
         }

         return bestPos;
      }
   }

   @Nullable
   public static BezierConnection resolvePrimaryCoasterCurveAtFakeCell(Level level, BlockPos fakeCell, Vec3 worldHit) {
      List<BezierConnection> curves = hostedPrimaryCoasterCurvesContainingCell(level, fakeCell);
      if (curves.isEmpty()) {
         return null;
      } else if (curves.size() == 1) {
         return curves.get(0);
      } else {
         BezierConnection best = null;
         double bestD = Double.MAX_VALUE;

         for (BezierConnection bc : curves) {
            double d = distanceSqToCoasterCurveInteraction(bc, worldHit, level);
            if (d < bestD) {
               bestD = d;
               best = bc;
            }
         }

         return best;
      }
   }

   public static double distanceSqToCoasterCurveInteraction(BezierConnection bc, Vec3 worldHit, @Nullable Level level) {
      BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
      int samples = Math.max(48, primary.getSegmentCount() * 4);
      double best = Double.MAX_VALUE;

      for (int i = 0; i <= samples; i++) {
         double t = (double)i / (double)samples;
         Vec3 p = primary.getPosition(t);
         if (level != null && primary.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
            p = p.add(CoasterBezierRailFrames.curveInteractionLiftOffset(primary, (float)t, level));
         }

         double d = p.distanceToSqr(worldHit);
         if (d < best) {
            best = d;
         }
      }

      return Math.min(best, primary.getBounds().inflate(0.75).distanceToSqr(worldHit));
   }

   public static BlockPos nearestEndpointBlockForHit(Vec3 worldHit, BlockPos a, BlockPos b) {
      double da = distSqPointToAabb(worldHit, unitBlockBounds(a));
      double db = distSqPointToAabb(worldHit, unitBlockBounds(b));
      return da <= db ? a : b;
   }

   public static Vec3 clampInteractionInsideCell(Vec3 rawWorld, BlockPos fakeTrackCell) {
      AABB b = unitBlockBounds(fakeTrackCell);
      double eps = 0.03125;
      double x = Mth.clamp(rawWorld.x, b.minX + eps, b.maxX - eps);
      double y = Mth.clamp(rawWorld.y, b.minY + eps, b.maxY - eps);
      double z = Mth.clamp(rawWorld.z, b.minZ + eps, b.maxZ - eps);
      return new Vec3(x, y, z);
   }

   private static AABB unitBlockBounds(BlockPos p) {
      return new AABB((double)p.getX(), (double)p.getY(), (double)p.getZ(), (double)p.getX() + 1.0, (double)p.getY() + 1.0, (double)p.getZ() + 1.0);
   }

   private static double distSqPointToAabb(Vec3 p, AABB aabb) {
      double x = Mth.clamp(p.x, aabb.minX, aabb.maxX);
      double y = Mth.clamp(p.y, aabb.minY, aabb.maxY);
      double z = Mth.clamp(p.z, aabb.minZ, aabb.maxZ);
      double dx = p.x - x;
      double dy = p.y - y;
      double dz = p.z - z;
      return dx * dx + dy * dy + dz * dz;
   }

   public static boolean isManagedFakeTrack(Level level, BlockPos pos) {
      return !AllBlocks.FAKE_TRACK.has(level.getBlockState(pos)) ? false : rasterScanHostedCurvesForCell(level, pos);
   }

   public static List<BezierConnection> hostedPrimaryCoasterCurvesContainingCell(Level level, BlockPos fakeCell) {
      List<BezierConnection> out = new ArrayList<>();
      Set<String> seenEdges = new HashSet<>();
      int bx = fakeCell.getX() >> 4;
      int bz = fakeCell.getZ() >> 4;

      for (int cox = -2; cox <= 2; cox++) {
         for (int coz = -2; coz <= 2; coz++) {
            int cx = bx + cox;
            int cz = bz + coz;
            if (level.hasChunk(cx, cz)) {
               LevelChunk chunk = level.getChunk(cx, cz);

               for (BlockEntity be : chunk.getBlockEntities().values()) {
                  if (be instanceof CoasterAnchorpointBlockEntity) {
                     CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)be;

                     for (BezierConnection raw : anchor.getAnchorPeerCurvesView().values()) {
                        if (raw != null) {
                           BezierConnection curve = raw.isPrimary() ? raw : raw.secondary();
                           if (curve.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)
                              && rasterContains(curve, fakeCell)
                              && seenEdges.add(edgeKey(curve))) {
                              out.add(curve);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return out;
   }

   @Nullable
   public static BlockPos nearestCoasterAnchorPosForFakeTrackCell(Level level, BlockPos fakeCell, Vec3 referenceWorld) {
      BezierConnection primary = resolvePrimaryCoasterCurveAtFakeCell(level, fakeCell, referenceWorld);
      List<BezierConnection> curves = primary != null ? List.of(primary) : hostedPrimaryCoasterCurvesContainingCell(level, fakeCell);
      if (curves.isEmpty()) {
         return null;
      } else {
         BlockPos best = null;
         double bestD = Double.MAX_VALUE;
         Set<Long> seenAnchors = new HashSet<>();

         for (BezierConnection bc : curves) {
            BlockPos a = (BlockPos)bc.bePositions.getFirst();
            BlockPos b = (BlockPos)bc.bePositions.getSecond();

            for (BlockPos ep : List.of(a, b)) {
               if (level.getBlockEntity(ep) instanceof CoasterAnchorpointBlockEntity) {
                  long key = ep.asLong();
                  if (seenAnchors.add(key)) {
                     Vec3 c = CoasterAnchorpointBlockEntity.worldCenter(level, ep);
                     double d = referenceWorld.distanceToSqr(c);
                     if (d < bestD) {
                        bestD = d;
                        best = ep.immutable();
                     }
                  }
               }
            }
         }

         return best;
      }
   }

   private static String edgeKey(BezierConnection bc) {
      long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
      long b = ((BlockPos)bc.bePositions.getSecond()).asLong();
      return a <= b ? a + ":" + b : b + ":" + a;
   }

   public static boolean isManagedFakeTrackAt(ServerLevel level, BlockPos pos) {
      return !AllBlocks.FAKE_TRACK.has(level.getBlockState(pos)) ? false : rasterScanHostedCurvesForCell(level, pos);
   }

   private static boolean rasterScanHostedCurvesForCell(Level level, BlockPos pos) {
      int bx = pos.getX() >> 4;
      int bz = pos.getZ() >> 4;

      for (int cox = -2; cox <= 2; cox++) {
         for (int coz = -2; coz <= 2; coz++) {
            int cx = bx + cox;
            int cz = bz + coz;
            if (level.hasChunk(cx, cz)) {
               LevelChunk chunk = level.getChunk(cx, cz);

               for (BlockEntity be : chunk.getBlockEntities().values()) {
                  if (be instanceof CoasterAnchorpointBlockEntity) {
                     CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)be;

                     for (BezierConnection raw : anchor.getAnchorPeerCurvesView().values()) {
                        if (raw != null) {
                           BezierConnection curve = raw.isPrimary() ? raw : raw.secondary();
                           if (rasterContains(curve, pos)) {
                              return true;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   public static void manageFakeTracksAlong(ServerLevel level, BezierConnection bc, boolean remove) {
      if (bc.isPrimary()) {
         Map<Pair<Integer, Integer>, Double> yLevels = bc.rasterise();

         for (Entry<Pair<Integer, Integer>, Double> entry : yLevels.entrySet()) {
            double yValue = entry.getValue();
            int floor = Mth.floor(yValue);
            BlockPos targetPos = new BlockPos((Integer)entry.getKey().getFirst(), floor, (Integer)entry.getKey().getSecond())
               .offset((Vec3i)bc.bePositions.getFirst())
               .above(1);
            if (level.isLoaded(targetPos)) {
               BlockState stateAtPos = level.getBlockState(targetPos);
               boolean present = AllBlocks.FAKE_TRACK.has(stateAtPos);
               if (remove) {
                  if (present) {
                     level.removeBlock(targetPos, false);
                  }
               } else {
                  FluidState fluidState = stateAtPos.getFluidState();
                  if (fluidState.isEmpty() || fluidState.isSourceOfType(Fluids.WATER)) {
                     if (!present && stateAtPos.canBeReplaced()) {
                        level.setBlock(targetPos, ProperWaterloggedBlock.withWater(level, AllBlocks.FAKE_TRACK.getDefaultState(), targetPos), 3);
                     }

                     FakeTrackBlock.keepAlive(level, targetPos);
                  }
               }
            }
         }
      }
   }

   static void keepAliveAlong(ServerLevel level, BezierConnection bc) {
      manageFakeTracksAlong(level, bc, false);
   }
}
