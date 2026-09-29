package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class RivetPlacement {
   private static final double SNAP_GRID_BLOCKS = 0.5;
   public static final int[] Y_ROTATION_DEGREES = new int[]{0, 15, 30, 45, 60, 75};
   private static final double LOOK_NORMAL_SNAP_COS = Math.cos(Math.toRadians(45.0));
   public static final double HOST_LOCK_DISTAL_BLOCKS = 3.0;

   private RivetPlacement() {
   }

   public static int cycleYRotationDegrees(int current) {
      for (int i = 0; i < Y_ROTATION_DEGREES.length; i++) {
         if (Y_ROTATION_DEGREES[i] == current) {
            return Y_ROTATION_DEGREES[(i + 1) % Y_ROTATION_DEGREES.length];
         }
      }

      return Y_ROTATION_DEGREES[0];
   }

   public static int wrapYRotationDegrees(int degrees) {
      return Math.floorMod(degrees, 360);
   }

   public static int yRotationFromLook(Level level, RivetPlacement.Resolved placement, Vec3 lookDirection, int storedYRotationDegrees) {
      Vec3 outward = hostFaceOutwardWorld(level, placement);
      if (outward.lengthSqr() < 1.0E-12) {
         outward = Vec3.atLowerCornerOf(placement.face().getNormal());
      }

      outward = outward.normalize();
      Vec3 look = lookDirection.normalize();
      if (Math.abs(look.dot(outward)) >= LOOK_NORMAL_SNAP_COS) {
         return wrapYRotationDegrees(storedYRotationDegrees);
      } else {
         Vec3 onPlane = look.subtract(outward.scale(look.dot(outward)));
         if (onPlane.lengthSqr() < 1.0E-8) {
            return wrapYRotationDegrees(storedYRotationDegrees);
         } else {
            onPlane = onPlane.normalize();
            Vec3 reference = referenceForwardWorld(level, placement);
            double sin = reference.cross(onPlane).dot(outward);
            double cos = reference.dot(onPlane);
            return wrapYRotationDegrees((int)Math.round(Math.toDegrees(Math.atan2(sin, cos))));
         }
      }
   }

   public static Vec3 referenceForwardWorld(Level level, RivetPlacement.Resolved placement) {
      RivetPlacement.Resolved zeroTwist = new RivetPlacement.Resolved(placement.host(), placement.face(), placement.lx(), placement.ly(), placement.lz(), 0);
      Quaterniond world = worldOrientation(level, zeroTwist);
      Vector3d forward = new Vector3d(0.0, 0.0, 1.0);
      world.transform(forward);
      return new Vec3(forward.x, forward.y, forward.z).normalize();
   }

   public static Vec3 referenceRightWorld(Level level, RivetPlacement.Resolved placement) {
      Quaterniond world = worldOrientation(level, placement);
      Vector3d right = new Vector3d(1.0, 0.0, 0.0);
      world.transform(right);
      return new Vec3(right.x, right.y, right.z).normalize();
   }

   public static int effectiveYRotationDegrees(Level level, BlockHitResult hit, boolean freePosition, int tabYRotationDegrees, Vec3 lookDirection) {
      return freePosition ? yRotationFromLook(level, resolve(hit, true), lookDirection, tabYRotationDegrees) : tabYRotationDegrees;
   }

   public static boolean crosshairTargetsExistingRivet(Level level, BlockHitResult hit, boolean freePosition, int tabYRotationDegrees, Vec3 lookDirection) {
      if (isRivetAt(level, hit.getBlockPos())) {
         return true;
      } else {
         int yRotation = effectiveYRotationDegrees(level, hit, freePosition, tabYRotationDegrees, lookDirection);
         return placementOverlapsExistingRivet(level, resolve(hit, freePosition).withYRotation(yRotation));
      }
   }

   public static boolean looksAtExistingRivet(Level level, Entity entity) {
      BlockHitResult hit = findHit(level, entity, Block.OUTLINE);
      return hit != null && isRivetAt(level, hit.getBlockPos());
   }

   @Nullable
   public static BlockHitResult findPlacementHit(Level level, Entity entity) {
      return findHit(level, entity, Block.COLLIDER);
   }

   @Nullable
   private static BlockHitResult findHit(Level level, Entity entity, Block blockMode) {
      double reach = entity instanceof Player player ? player.blockInteractionRange() : 5.0;
      Vec3 from = entity.getEyePosition(1.0F);
      Vec3 to = from.add(entity.getViewVector(1.0F).scale(reach));
      BlockHitResult hit = level.clip(new ClipContext(from, to, blockMode, Fluid.NONE, entity));
      return hit.getType() == Type.BLOCK ? hit : null;
   }

   public static RivetPlacement.Resolved resolve(BlockHitResult hit, boolean freePosition) {
      BlockPos host = hit.getBlockPos();
      Direction face = hit.getDirection();
      Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(host));
      double lx = Mth.clamp(local.x, 0.0, 1.0);
      double ly = Mth.clamp(local.y, 0.0, 1.0);
      double lz = Mth.clamp(local.z, 0.0, 1.0);
      if (!freePosition) {
         switch (face.getAxis()) {
            case X:
               ly = snapHalfBlock(ly);
               lz = snapHalfBlock(lz);
               break;
            case Y:
               lx = snapHalfBlock(lx);
               lz = snapHalfBlock(lz);
               break;
            case Z:
               lx = snapHalfBlock(lx);
               ly = snapHalfBlock(ly);
         }
      }

      return new RivetPlacement.Resolved(host, face, lx, ly, lz);
   }

   public static boolean isValidPlacement(Level level, RivetPlacement.Resolved placement) {
      if (isRivetAt(level, placement.host())) {
         return false;
      } else {
         return isBalloonHost(level, placement.host()) ? false : !placementOverlapsExistingRivet(level, placement);
      }
   }

   public static boolean isBalloonHost(Level level, BlockPos pos) {
      if (BalloonResolve.isBalloonBlockState(level.getBlockState(pos))) {
         return true;
      } else {
         SubLevel sub = Sable.HELPER.getContaining(level, pos);
         return sub != null && !sub.isRemoved() && BalloonResolve.isBalloonSubLevel(sub);
      }
   }

   public static boolean isRivetAt(Level level, BlockPos pos) {
      if (level.getBlockState(pos).is((net.minecraft.world.level.block.Block)SimulatedCoastersBlocks.RIVET.get())) {
         return true;
      } else {
         SubLevel sub = Sable.HELPER.getContaining(level, pos);
         if (sub != null && !sub.isRemoved()) {
            BlockPos rivetCell = sub.getPlot().getCenterBlock();
            return !pos.equals(rivetCell)
               ? false
               : sub.getPlot()
                  .getEmbeddedLevelAccessor()
                  .getBlockState(BlockPos.ZERO)
                  .is((net.minecraft.world.level.block.Block)SimulatedCoastersBlocks.RIVET.get());
         } else {
            return false;
         }
      }
   }

   public static boolean isRivetHostedSubLevel(Level level, SubLevel sub) {
      if (sub == null || sub.isRemoved()) {
         return false;
      } else {
         return RivetHostKey.isRivetSubLevel(sub)
            ? true
            : level.getBlockState(sub.getPlot().getCenterBlock()).is((net.minecraft.world.level.block.Block)SimulatedCoastersBlocks.RIVET.get());
      }
   }

   public static boolean isRivetHostedPlacement(Level level, BlockPos clickedPos) {
      return isRivetHostedSubLevel(level, Sable.HELPER.getContaining(level, clickedPos));
   }

   public static boolean shouldBypassSablePlacementCollision(BlockPlaceContext context) {
      Level level = context.getLevel();
      BlockPos clicked = context.getClickedPos();
      SubLevel containing = Sable.HELPER.getContaining(level, clicked);
      if (isRivetHostedSubLevel(level, containing)) {
         return true;
      } else {
         BoundingBox3d placed = new BoundingBox3d(clicked);
         if (containing != null) {
            placed.transform(containing.logicalPose(), placed);
         }

         for (SubLevel sub : Sable.HELPER.getAllIntersecting(level, placed)) {
            if (isRivetHostedSubLevel(level, sub)) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean placementOverlapsExistingRivet(Level level, RivetPlacement.Resolved placement) {
      AABB box = previewAabb(placement);
      BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
      BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);

      for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
         if (isRivetAt(level, pos)) {
            return true;
         }
      }

      return false;
   }

   public static AABB previewAabb(RivetPlacement.Resolved placement) {
      RivetPlacement.PreviewOutline outline = previewOutline(placement);
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double minZ = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      double maxZ = Double.NEGATIVE_INFINITY;

      for (Vec3 corner : previewCorners(outline)) {
         minX = Math.min(minX, corner.x);
         minY = Math.min(minY, corner.y);
         minZ = Math.min(minZ, corner.z);
         maxX = Math.max(maxX, corner.x);
         maxY = Math.max(maxY, corner.y);
         maxZ = Math.max(maxZ, corner.z);
      }

      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public static RivetPlacement.PreviewOutline previewOutline(RivetPlacement.Resolved placement) {
      double h = 0.3125;
      double hd = 0.0625;
      Quaterniond orient = plotOrientationForFace(placement.face(), placement.yRotationDegrees());
      Matrix3d basis = new Matrix3d().set(orient);
      Vector3d axisRight = basis.getColumn(0, new Vector3d());
      Vector3d axisUp = basis.getColumn(1, new Vector3d());
      Vector3d axisForward = basis.getColumn(2, new Vector3d());
      return new RivetPlacement.PreviewOutline(faceAnchorPlot(placement), vec3(axisRight), vec3(axisUp), vec3(axisForward), h, hd, h);
   }

   private static Vec3[] previewCorners(RivetPlacement.PreviewOutline outline) {
      Vec3 center = outline.center();
      Vec3 axisRight = outline.axisRight();
      Vec3 axisUp = outline.axisUp();
      Vec3 axisForward = outline.axisForward();
      Vec3[] corners = new Vec3[8];
      int idx = 0;

      for (int sx = 0; sx < 2; sx++) {
         for (int sy = 0; sy < 2; sy++) {
            for (int sz = 0; sz < 2; sz++) {
               double fx = sx == 0 ? -1.0 : 1.0;
               double fy = sy == 0 ? -1.0 : 1.0;
               double fz = sz == 0 ? -1.0 : 1.0;
               corners[idx++] = center.add(axisRight.scale(fx * outline.halfRight()))
                  .add(axisUp.scale(fy * outline.halfUp()))
                  .add(axisForward.scale(fz * outline.halfForward()));
            }
         }
      }

      return corners;
   }

   private static Vec3 vec3(Vector3d v) {
      return new Vec3(v.x, v.y, v.z);
   }

   public static Vec3 faceAnchorPlot(RivetPlacement.Resolved placement) {
      return faceAnchorPlot(placement.host(), placement.face(), placement.lx(), placement.ly(), placement.lz());
   }

   public static Vec3 faceAnchorPlot(BlockPos host, Direction face, double lx, double ly, double lz) {
      return new Vec3((double)host.getX() + lx, (double)host.getY() + ly, (double)host.getZ() + lz);
   }

   public static Vec3 faceAnchorWorld(Level level, RivetPlacement.Resolved placement) {
      Vec3 plotAnchor = faceAnchorPlot(placement);
      SubLevel hostSub = Sable.HELPER.getContaining(level, placement.host());
      return hostSub == null ? plotAnchor : hostSub.logicalPose().transformPosition(plotAnchor);
   }

   public static Vec3 hostFaceOutwardWorld(Level level, RivetPlacement.Resolved placement) {
      Vec3 local = Vec3.atLowerCornerOf(placement.face().getNormal());
      SubLevel hostSub = Sable.HELPER.getContaining(level, placement.host());
      if (hostSub == null) {
         return local;
      } else {
         Vector3d n = new Vector3d(local.x, local.y, local.z);
         hostSub.logicalPose().orientation().transform(n);
         return new Vec3(n.x, n.y, n.z);
      }
   }

   public static BlockState hostBlockState(Level level, RivetHostKey host) {
      return level.getBlockState(host.cell());
   }

   public static Quaterniond worldOrientation(Level level, RivetPlacement.Resolved placement) {
      Quaterniond local = plotOrientationForFace(placement.face(), placement.yRotationDegrees());
      SubLevel hostSub = Sable.HELPER.getContaining(level, placement.host());
      return hostSub == null ? local : hostSub.logicalPose().orientation().mul(local, new Quaterniond());
   }

   public static RivetPlacement.Attachment attachment(RivetPlacement.Resolved placement) {
      return new RivetPlacement.Attachment(placement.lx(), placement.ly(), placement.lz());
   }

   public static RivetPlacement.Attachment rotateAttachment(Direction face, RivetPlacement.Attachment attachment, AssemblyTransform transform) {
      Rotation rotation = transform.getRotation();
      if (rotation == Rotation.NONE) {
         return attachment;
      } else {
         Direction newFace = rotation.rotate(face);
         Vec3 blockLocal = attachmentBlockLocal(face, attachment.lx(), attachment.ly(), attachment.lz());
         Vec3 centered = blockLocal.subtract(0.5, 0.5, 0.5);
         Vec3 rotated = centered.yRot(yRotRadians(rotation)).add(0.5, 0.5, 0.5);
         return attachmentOnFace(newFace, rotated);
      }
   }

   private static float yRotRadians(Rotation rotation) {
      return switch (rotation) {
         case NONE -> 0.0F;
         case CLOCKWISE_90 -> (float) (-Math.PI / 2);
         case CLOCKWISE_180 -> (float) -Math.PI;
         case COUNTERCLOCKWISE_90 -> (float) (Math.PI / 2);
         default -> throw new MatchException(null, null);
      };
   }

   private static Vec3 attachmentBlockLocal(Direction face, double lx, double ly, double lz) {
      return new Vec3(lx, ly, lz);
   }

   private static RivetPlacement.Attachment attachmentOnFace(Direction face, Vec3 blockLocal) {
      double lx = Mth.clamp(blockLocal.x - Math.floor(blockLocal.x), 0.0, 1.0);
      double ly = Mth.clamp(blockLocal.y - Math.floor(blockLocal.y), 0.0, 1.0);
      double lz = Mth.clamp(blockLocal.z - Math.floor(blockLocal.z), 0.0, 1.0);
      return new RivetPlacement.Attachment(lx, ly, lz);
   }

   private static double snapHalfBlock(double local) {
      return Mth.clamp((double)Math.round(local / 0.5) * 0.5, 0.0, 1.0);
   }

   public static Quaterniond plotOrientationForFace(Direction face) {
      return plotOrientationForFace(face, 0);
   }

   public static Quaterniond plotOrientationForFace(Direction face, int yRotationDegrees) {
      Vector3d outward = new Vector3d((double)face.getStepX(), (double)face.getStepY(), (double)face.getStepZ());
      Vector3d xAxis = new Vector3d();
      Vector3d zAxis = new Vector3d();
      Vector3d worldUp = new Vector3d(0.0, 1.0, 0.0);
      Vector3d worldSouth = new Vector3d(0.0, 0.0, 1.0);
      if (face.getAxis() == Axis.Y) {
         worldSouth.cross(outward, xAxis).normalize();
         if (face == Direction.DOWN) {
            xAxis.negate();
         }
      } else {
         outward.cross(worldUp, xAxis).normalize();
      }

      xAxis.cross(outward, zAxis).normalize();
      Matrix3d basis = new Matrix3d();
      basis.setColumn(0, xAxis);
      basis.setColumn(1, outward);
      basis.setColumn(2, zAxis);
      Quaterniond orientation = new Quaterniond().setFromNormalized(basis);
      if (yRotationDegrees != 0) {
         orientation.mul(new Quaterniond().rotateY(Math.toRadians((double)yRotationDegrees)));
      }

      return orientation;
   }

   public static Vec3 rivetOutwardAnchorPlot(BlockPos plotCell) {
      return new Vec3((double)plotCell.getX() + 0.5, (double)plotCell.getY() + 1.0, (double)plotCell.getZ() + 0.5);
   }

   public static Vec3 hostDistalAnchorPlot(BlockPos host, Direction face, double lx, double ly, double lz) {
      return faceAnchorPlot(host, face, lx, ly, lz).add(Vec3.atLowerCornerOf(face.getNormal()).scale(3.0));
   }

   public static Vec3 rivetDistalAnchorPlot(BlockPos plotCell) {
      return rivetOutwardAnchorPlot(plotCell).add(0.0, 3.0, 0.0);
   }

   public static Vec3 hostLateralAnchorPlot(BlockPos host, Direction face, double lx, double ly, double lz, int yRotationDegrees) {
      Quaterniond orientation = plotOrientationForFace(face, yRotationDegrees);
      Vector3d lateral = new Vector3d(1.0, 0.0, 0.0);
      orientation.transform(lateral);
      return faceAnchorPlot(host, face, lx, ly, lz).add(new Vec3(lateral.x, lateral.y, lateral.z).scale(3.0));
   }

   public static Vec3 rivetLateralAnchorPlot(BlockPos plotCell) {
      return rivetOutwardAnchorPlot(plotCell).add(3.0, 0.0, 0.0);
   }

   public static record Attachment(double lx, double ly, double lz) {
   }

   public static record PreviewOutline(Vec3 center, Vec3 axisRight, Vec3 axisUp, Vec3 axisForward, double halfRight, double halfUp, double halfForward) {
   }

   public static record Resolved(BlockPos host, Direction face, double lx, double ly, double lz, int yRotationDegrees) {
      public Resolved(BlockPos host, Direction face, double lx, double ly, double lz) {
         this(host, face, lx, ly, lz, 0);
      }

      public RivetPlacement.Resolved withYRotation(int degrees) {
         return new RivetPlacement.Resolved(this.host, this.face, this.lx, this.ly, this.lz, RivetPlacement.wrapYRotationDegrees(degrees));
      }
   }
}
