package dev.silvergold.simulatedcoasters.track.assembly;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CoasterBezierAssemblyRotationFix {
   private CoasterBezierAssemblyRotationFix() {
   }

   public static void afterMoveBlocks(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> sourceCells) {
      StructureTransform st = yAxisStructureRotation(transform);
      List<BlockPos> placed = collectDestinations(transform, sourceCells);

      for (BlockPos dst : placed) {
         if (level.getBlockEntity(dst) instanceof CoasterAnchorpointBlockEntity anchor) {
            if (st != null) {
               applyStructureToDistinctAnchorBezier(level, anchor.getBlockPos(), anchor, st);
            }

            anchor.rewriteNeighborPositionsAfterAssemblyTransform(transform);
         }

         BlockState stBl = level.getBlockState(dst);
         scheduleCoasterTick(level, dst, stBl.getBlock());
      }

      if (st != null) {
         resyncMirrorSecondariesFromPrimaries(level, placed);
      }
   }

   private static List<BlockPos> collectDestinations(AssemblyTransform transform, Iterable<BlockPos> sourceCells) {
      List<BlockPos> placed = new ArrayList<>();

      for (BlockPos src : sourceCells) {
         if (src != null) {
            placed.add(transform.apply(src).immutable());
         }
      }

      return placed;
   }

   private static void scheduleCoasterTick(ServerLevel level, BlockPos dst, Block block) {
      if (block instanceof CoasterAnchorpointBlock) {
         if (!level.getBlockTicks().hasScheduledTick(dst, block)) {
            level.scheduleTick(dst, block, 1);
         }
      }
   }

   public static void applyBezierYStructureAroundOwner(ServerLevel level, BlockPos ownerWorldPos, BezierConnection primary, StructureTransform tf) {
      if (tf.rotationAxis == Axis.Y) {
         if (primary.isPrimary()) {
            Couple<Double> handleLengths = CoasterPeerCurveHandleLengths.takeCanonicalLengthsForRemap(primary);
            AnchorPeerFakeTracks.manageFakeTracksAlong(level, primary, true);
            primary.normals.replace(tf::applyWithoutOffsetUncentered);
            primary.axes.replace(tf::applyWithoutOffsetUncentered);
            BlockPos diff = ((BlockPos)primary.bePositions.getSecond()).subtract((Vec3i)primary.bePositions.getFirst());
            primary.bePositions
               .setSecond(
                  BlockPos.containing(Vec3.atCenterOf((Vec3i)primary.bePositions.getFirst()).add(tf.applyWithoutOffsetUncentered(Vec3.atLowerCornerOf(diff))))
               );
            Vec3 beVec = Vec3.atLowerCornerOf(ownerWorldPos);
            Vec3 teCenterVec = beVec.add(0.5, 0.5, 0.5);
            Vec3 start = (Vec3)primary.starts.getFirst();
            Vec3 startToBE = start.subtract(teCenterVec);
            Vec3 endToStart = ((Vec3)primary.starts.getSecond()).subtract(start);
            startToBE = tf.applyWithoutOffsetUncentered(startToBE).add(teCenterVec);
            endToStart = tf.applyWithoutOffsetUncentered(endToStart).add(startToBE);
            primary.starts.setFirst(startToBE);
            primary.starts.setSecond(endToStart);
            AnchorPeerFakeTracks.manageFakeTracksAlong(level, primary, false);
            if (handleLengths != null) {
               CoasterPeerCurveHandleLengths.putCanonical(primary, (Double)handleLengths.getFirst(), (Double)handleLengths.getSecond());
            }
         }
      }
   }

   private static void applyStructureToDistinctAnchorBezier(ServerLevel level, BlockPos owner, CoasterAnchorpointBlockEntity anchor, StructureTransform st) {
      Map<BlockPos, BezierConnection> snap = anchor.viewAnchorPeerCurvesSnapshot();
      if (!snap.isEmpty()) {
         for (BezierConnection bc : snap.values()) {
            if (bc.isPrimary()) {
               applyBezierYStructureAroundOwner(level, owner, bc, st);
            }
         }
      }
   }

   private static void resyncMirrorSecondariesFromPrimaries(ServerLevel level, List<BlockPos> placed) {
      for (BlockPos dst : placed) {
         BlockEntity be = level.getBlockEntity(dst);
         if (be instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)be;
            BlockPos owner = anchor.getBlockPos();

            for (Entry<BlockPos, BezierConnection> e : anchor.getAnchorPeerCurvesView().entrySet()) {
               BezierConnection bc = e.getValue();
               if (bc != null && bc.isPrimary()) {
                  BlockPos peer = e.getKey();
                  if (level.getBlockEntity(peer) instanceof CoasterAnchorpointBlockEntity peerBe) {
                     peerBe.overwriteReciprocalForPeerCurve(owner, bc);
                  }
               }
            }
         }
      }
   }

   public static StructureTransform yAxisStructureRotation(AssemblyTransform asm) {
      Rotation rot = asm.getRotation();
      int quarters = readQuarterTurnSteps(asm);
      if (rot == Rotation.NONE && quarters != 0) {
         rot = Rotation.values()[Math.floorMod(4 - quarters, 4)];
      }

      return rot == Rotation.NONE ? null : new StructureTransform(BlockPos.ZERO, Axis.Y, rot, Mirror.NONE);
   }

   private static int readQuarterTurnSteps(AssemblyTransform asm) {
      try {
         Field field = asm.getClass().getDeclaredField("angle");
         field.setAccessible(true);
         return field.getInt(asm);
      } catch (ReflectiveOperationException var2) {
         return 0;
      }
   }
}
