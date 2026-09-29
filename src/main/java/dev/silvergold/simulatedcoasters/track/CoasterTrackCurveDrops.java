package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.BezierConnection.Segment;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CoasterTrackCurveDrops {
   private static final double MIN_CHORD_BLOCKS_FOR_SEGMENT_LUT = 0.500000001;

   private CoasterTrackCurveDrops() {
   }

   public static boolean isCoasterMaterial(BezierConnection bc) {
      return bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
   }

   public static int trackItemCostForAnchorPair(Level level, BlockPos anchorA, BlockPos anchorB) {
      Vec3 c0 = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorA);
      Vec3 c1 = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorB);
      return trackItemCostForChordBlocks(c0.distanceTo(c1));
   }

   public static int trackItemCostForPreviewEndpoints(Level level, BlockPos fromAnchor, BlockPos toTarget) {
      Vec3 c0 = CoasterAnchorpointBlockEntity.worldCenterBase(level, fromAnchor);
      Vec3 c1 = CoasterAnchorpointBlockEntity.worldCenterBase(level, toTarget);
      return trackItemCostForChordBlocks(c0.distanceTo(c1));
   }

   public static int trackItemCostForConnection(Level level, BezierConnection bc) {
      if (!isCoasterMaterial(bc)) {
         return bc != null ? bc.getTrackItemCost() : 0;
      } else {
         return trackItemCostForAnchorPair(level, (BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond());
      }
   }

   static int trackItemCostForChordBlocks(double chordBlocks) {
      double lengthForSegments = Math.max(chordBlocks, 0.500000001);
      int segmentCount = (int)(lengthForSegments * 2.0);
      return (segmentCount + 1) / 2;
   }

   public static void spawnItems(Level level, BezierConnection bc) {
      if (isCoasterMaterial(bc)) {
         if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            int tracks = trackItemCostForConnection(level, bc);
            if (tracks > 0) {
               Vec3 origin = Vec3.atLowerCornerOf((Vec3i)bc.bePositions.getFirst());
               ItemStack stack = new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
               int spawned = 0;

               for (Segment segment : bc) {
                  if (spawned >= tracks) {
                     break;
                  }

                  if (segment.index % 2 == 0 && segment.index != bc.getSegmentCount()) {
                     Vec3 v = VecHelper.offsetRandomly(segment.position, level.random, 0.125F).add(origin);
                     ItemEntity entity = new ItemEntity(level, v.x, v.y, v.z, stack.copy());
                     entity.setDefaultPickUpDelay();
                     level.addFreshEntity(entity);
                     spawned++;
                  }
               }

               while (spawned < tracks) {
                  Vec3 mid = CoasterAnchorpointBlockEntity.worldCenterBase(level, (BlockPos)bc.bePositions.getFirst())
                     .add(CoasterAnchorpointBlockEntity.worldCenterBase(level, (BlockPos)bc.bePositions.getSecond()))
                     .scale(0.5);
                  Vec3 v = VecHelper.offsetRandomly(mid.subtract(origin), level.random, 0.125F).add(origin);
                  ItemEntity entity = new ItemEntity(level, v.x, v.y, v.z, stack.copy());
                  entity.setDefaultPickUpDelay();
                  level.addFreshEntity(entity);
                  spawned++;
               }
            }
         }
      }
   }

   public static void addItemsToPlayer(Player player, BezierConnection bc) {
      if (isCoasterMaterial(bc)) {
         Level level = player.level();
         Inventory inv = player.getInventory();

         for (int tracks = trackItemCostForConnection(level, bc); tracks > 0; tracks -= 64) {
            inv.placeItemBackInInventory(new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get(), Math.min(64, tracks)));
         }
      }
   }
}
