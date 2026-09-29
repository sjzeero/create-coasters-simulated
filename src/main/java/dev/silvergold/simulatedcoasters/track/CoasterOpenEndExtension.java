package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterOpenEndExtension {
   private CoasterOpenEndExtension() {
   }

   public static float extensionBlocks(Level level, BlockPos anchorPos) {
      if (level.isClientSide()) {
         Float preview = OpenEndExtensionPreviewProvider.extensionBlocksAt(anchorPos);
         if (preview != null) {
            return clampExtensionBlocks(preview);
         }
      }

      return level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be ? be.getOpenEndExtensionBlocks() : 0.5F;
   }

   public static float extensionBlocksForCurveEndpoint(Level level, BezierConnection bc, boolean atFirstEndpoint) {
      BlockPos anchor = atFirstEndpoint ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
      if (level.getBlockEntity(anchor) instanceof CoasterAnchorpointBlockEntity be && be.legCount() == 1) {
         return extensionBlocks(level, anchor);
      }

      return 0.5F;
   }

   public static boolean isOpenEndAnchor(Level level, BlockPos anchorPos) {
      return level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be ? be.legCount() == 1 : false;
   }

   @Nullable
   public static BezierConnection primaryCoasterCurveAt(Level level, BlockPos anchorPos) {
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         for (BezierConnection cRaw : be.getAnchorPeerCurvesView().values()) {
            if (cRaw != null) {
               BezierConnection p = cRaw.isPrimary() ? cRaw : cRaw.secondary();
               if (p.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
                  return p;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static Vec3 tipPlotLocalAt(Level level, BlockPos anchorPos, BezierConnection bc) {
      return tipPlotLocal(bc, anchorPos, extensionBlocks(level, anchorPos));
   }

   public static Vec3 junctionPlotLocal(BezierConnection bc, BlockPos anchorPos) {
      double t = ((BlockPos)bc.bePositions.getFirst()).equals(anchorPos) ? 0.0 : 1.0;
      return CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, t));
   }

   public static Vec3 outwardPlotLocal(BezierConnection bc, BlockPos anchorPos) {
      double t = ((BlockPos)bc.bePositions.getFirst()).equals(anchorPos) ? 0.0 : 1.0;
      Vec3 tan = CoasterPathGraphManager.coasterBezierPlotLocalTangent(bc, t);
      if (tan.lengthSqr() < 1.0E-14) {
         tan = new Vec3(0.0, 0.0, 1.0);
      } else {
         tan = tan.normalize();
      }

      return ((BlockPos)bc.bePositions.getFirst()).equals(anchorPos) ? tan.scale(-1.0) : tan;
   }

   public static Vec3 tipPlotLocal(BezierConnection bc, BlockPos anchorPos, float extensionBlocks) {
      return junctionPlotLocal(bc, anchorPos).add(outwardPlotLocal(bc, anchorPos).scale((double)extensionBlocks));
   }

   public static float clampExtensionBlocks(float blocks) {
      return Mth.clamp(blocks, 0.15F, 1.25F);
   }

   public static float snapExtensionBlocksToCenter(float blocks) {
      float clamped = clampExtensionBlocks(blocks);
      float center = 0.5F;
      return Math.abs(clamped - center) <= 0.08F ? center : clamped;
   }

   public static Vec3 virtualTargetPlotLocalForExtensionBlocks(BezierConnection bc, BlockPos anchorPos, Vec3 virtualTargetPlotLocal, float extensionBlocks) {
      Vec3 junction = junctionPlotLocal(bc, anchorPos);
      Vec3 outward = outwardPlotLocal(bc, anchorPos);
      if (outward.lengthSqr() < 1.0E-14) {
         return virtualTargetPlotLocal;
      } else {
         outward = outward.normalize();
         double rawAlong = virtualTargetPlotLocal.subtract(junction).dot(outward);
         return virtualTargetPlotLocal.add(outward.scale((double)extensionBlocks - rawAlong));
      }
   }

   @Nullable
   public static Float extensionBlocksFromVirtualTarget(
      Level level, BlockPos anchorPos, BezierConnection bc, Vec3 virtualTargetPlotLocal, boolean applyCenterSnap
   ) {
      if (!isOpenEndAnchor(level, anchorPos)) {
         return null;
      } else {
         Vec3 junction = junctionPlotLocal(bc, anchorPos);
         Vec3 outward = outwardPlotLocal(bc, anchorPos);
         if (outward.lengthSqr() < 1.0E-14) {
            return null;
         } else {
            double along = virtualTargetPlotLocal.subtract(junction).dot(outward);
            float clamped = clampExtensionBlocks((float)along);
            return applyCenterSnap ? snapExtensionBlocksToCenter(clamped) : clamped;
         }
      }
   }

   @Nullable
   public static Float extensionBlocksFromVirtualTarget(Level level, BlockPos anchorPos, BezierConnection bc, Vec3 virtualTargetPlotLocal) {
      return extensionBlocksFromVirtualTarget(level, anchorPos, bc, virtualTargetPlotLocal, true);
   }
}
