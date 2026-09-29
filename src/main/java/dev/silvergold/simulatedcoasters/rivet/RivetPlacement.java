/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  dev.ryanhcode.sable.companion.math.BoundingBox3d
 *  dev.ryanhcode.sable.companion.math.BoundingBox3dc
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix3d
 *  org.joml.Matrix3dc
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class RivetPlacement {
    private static final double SNAP_GRID_BLOCKS = 0.5;
    public static final int[] Y_ROTATION_DEGREES = new int[]{0, 15, 30, 45, 60, 75};
    private static final double LOOK_NORMAL_SNAP_COS = Math.cos(Math.toRadians(45.0));
    public static final double HOST_LOCK_DISTAL_BLOCKS = 3.0;

    private RivetPlacement() {
    }

    public static int cycleYRotationDegrees(int current) {
        for (int i = 0; i < Y_ROTATION_DEGREES.length; ++i) {
            if (Y_ROTATION_DEGREES[i] != current) continue;
            return Y_ROTATION_DEGREES[(i + 1) % Y_ROTATION_DEGREES.length];
        }
        return Y_ROTATION_DEGREES[0];
    }

    public static int wrapYRotationDegrees(int degrees) {
        return Math.floorMod(degrees, 360);
    }

    public static int yRotationFromLook(Level level, Resolved placement, Vec3 lookDirection, int storedYRotationDegrees) {
        Vec3 outward = RivetPlacement.hostFaceOutwardWorld(level, placement);
        if (outward.lengthSqr() < 1.0E-12) {
            outward = Vec3.atLowerCornerOf((Vec3i)placement.face().getNormal());
        }
        outward = outward.normalize();
        Vec3 look = lookDirection.normalize();
        if (Math.abs(look.dot(outward)) >= LOOK_NORMAL_SNAP_COS) {
            return RivetPlacement.wrapYRotationDegrees(storedYRotationDegrees);
        }
        Vec3 onPlane = look.subtract(outward.scale(look.dot(outward)));
        if (onPlane.lengthSqr() < 1.0E-8) {
            return RivetPlacement.wrapYRotationDegrees(storedYRotationDegrees);
        }
        onPlane = onPlane.normalize();
        Vec3 reference = RivetPlacement.referenceForwardWorld(level, placement);
        double sin = reference.cross(onPlane).dot(outward);
        double cos = reference.dot(onPlane);
        return RivetPlacement.wrapYRotationDegrees((int)Math.round(Math.toDegrees(Math.atan2(sin, cos))));
    }

    public static Vec3 referenceForwardWorld(Level level, Resolved placement) {
        Resolved zeroTwist = new Resolved(placement.host(), placement.face(), placement.lx(), placement.ly(), placement.lz(), 0);
        Quaterniond world = RivetPlacement.worldOrientation(level, zeroTwist);
        Vector3d forward = new Vector3d(0.0, 0.0, 1.0);
        world.transform(forward);
        return new Vec3(forward.x, forward.y, forward.z).normalize();
    }

    public static Vec3 referenceRightWorld(Level level, Resolved placement) {
        Quaterniond world = RivetPlacement.worldOrientation(level, placement);
        Vector3d right = new Vector3d(1.0, 0.0, 0.0);
        world.transform(right);
        return new Vec3(right.x, right.y, right.z).normalize();
    }

    public static int effectiveYRotationDegrees(Level level, BlockHitResult hit, boolean freePosition, int tabYRotationDegrees, Vec3 lookDirection) {
        if (freePosition) {
            return RivetPlacement.yRotationFromLook(level, RivetPlacement.resolve(hit, true), lookDirection, tabYRotationDegrees);
        }
        return tabYRotationDegrees;
    }

    public static boolean crosshairTargetsExistingRivet(Level level, BlockHitResult hit, boolean freePosition, int tabYRotationDegrees, Vec3 lookDirection) {
        if (RivetPlacement.isRivetAt(level, hit.getBlockPos())) {
            return true;
        }
        int yRotation = RivetPlacement.effectiveYRotationDegrees(level, hit, freePosition, tabYRotationDegrees, lookDirection);
        return RivetPlacement.placementOverlapsExistingRivet(level, RivetPlacement.resolve(hit, freePosition).withYRotation(yRotation));
    }

    public static boolean looksAtExistingRivet(Level level, Entity entity) {
        BlockHitResult hit = RivetPlacement.findHit(level, entity, ClipContext.Block.OUTLINE);
        return hit != null && RivetPlacement.isRivetAt(level, hit.getBlockPos());
    }

    @Nullable
    public static BlockHitResult findPlacementHit(Level level, Entity entity) {
        return RivetPlacement.findHit(level, entity, ClipContext.Block.COLLIDER);
    }

    @Nullable
    private static BlockHitResult findHit(Level level, Entity entity, ClipContext.Block blockMode) {
        double d;
        if (entity instanceof Player) {
            Player player = (Player)entity;
            d = player.blockInteractionRange();
        } else {
            d = 5.0;
        }
        double reach = d;
        Vec3 from = entity.getEyePosition(1.0f);
        Vec3 to = from.add(entity.getViewVector(1.0f).scale(reach));
        BlockHitResult hit = level.clip(new ClipContext(from, to, blockMode, ClipContext.Fluid.NONE, entity));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    public static Resolved resolve(BlockHitResult hit, boolean freePosition) {
        BlockPos host = hit.getBlockPos();
        Direction face = hit.getDirection();
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf((Vec3i)host));
        double lx = Mth.clamp((double)local.x, (double)0.0, (double)1.0);
        double ly = Mth.clamp((double)local.y, (double)0.0, (double)1.0);
        double lz = Mth.clamp((double)local.z, (double)0.0, (double)1.0);
        if (!freePosition) {
            switch (face.getAxis()) {
                case X: {
                    ly = RivetPlacement.snapHalfBlock(ly);
                    lz = RivetPlacement.snapHalfBlock(lz);
                    break;
                }
                case Y: {
                    lx = RivetPlacement.snapHalfBlock(lx);
                    lz = RivetPlacement.snapHalfBlock(lz);
                    break;
                }
                case Z: {
                    lx = RivetPlacement.snapHalfBlock(lx);
                    ly = RivetPlacement.snapHalfBlock(ly);
                }
            }
        }
        return new Resolved(host, face, lx, ly, lz);
    }

    public static boolean isValidPlacement(Level level, Resolved placement) {
        if (RivetPlacement.isRivetAt(level, placement.host())) {
            return false;
        }
        if (RivetPlacement.isBalloonHost(level, placement.host())) {
            return false;
        }
        return !RivetPlacement.placementOverlapsExistingRivet(level, placement);
    }

    public static boolean isBalloonHost(Level level, BlockPos pos) {
        if (BalloonResolve.isBalloonBlockState(level.getBlockState(pos))) {
            return true;
        }
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)pos);
        return sub != null && !sub.isRemoved() && BalloonResolve.isBalloonSubLevel(sub);
    }

    public static boolean isRivetAt(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is((Block)SimulatedCoastersBlocks.RIVET.get())) {
            return true;
        }
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)pos);
        if (sub == null || sub.isRemoved()) {
            return false;
        }
        BlockPos rivetCell = sub.getPlot().getCenterBlock();
        if (!pos.equals((Object)rivetCell)) {
            return false;
        }
        return sub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is((Block)SimulatedCoastersBlocks.RIVET.get());
    }

    public static boolean isRivetHostedSubLevel(Level level, SubLevel sub) {
        if (sub == null || sub.isRemoved()) {
            return false;
        }
        if (RivetHostKey.isRivetSubLevel(sub)) {
            return true;
        }
        return level.getBlockState(sub.getPlot().getCenterBlock()).is((Block)SimulatedCoastersBlocks.RIVET.get());
    }

    public static boolean isRivetHostedPlacement(Level level, BlockPos clickedPos) {
        return RivetPlacement.isRivetHostedSubLevel(level, Sable.HELPER.getContaining(level, (Vec3i)clickedPos));
    }

    public static boolean shouldBypassSablePlacementCollision(BlockPlaceContext context) {
        BlockPos clicked;
        SubLevel containing;
        Level level = context.getLevel();
        if (RivetPlacement.isRivetHostedSubLevel(level, containing = Sable.HELPER.getContaining(level, (Vec3i)(clicked = context.getClickedPos())))) {
            return true;
        }
        BoundingBox3d placed = new BoundingBox3d(clicked);
        if (containing != null) {
            placed.transform((Pose3dc)containing.logicalPose(), placed);
        }
        for (SubLevel sub : Sable.HELPER.getAllIntersecting(level, (BoundingBox3dc)placed)) {
            if (!RivetPlacement.isRivetHostedSubLevel(level, sub)) continue;
            return true;
        }
        return false;
    }

    public static boolean placementOverlapsExistingRivet(Level level, Resolved placement) {
        AABB box = RivetPlacement.previewAabb(placement);
        BlockPos min = BlockPos.containing((double)box.minX, (double)box.minY, (double)box.minZ);
        BlockPos max = BlockPos.containing((double)box.maxX, (double)box.maxY, (double)box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed((BlockPos)min, (BlockPos)max)) {
            if (!RivetPlacement.isRivetAt(level, pos)) continue;
            return true;
        }
        return false;
    }

    public static AABB previewAabb(Resolved placement) {
        PreviewOutline outline = RivetPlacement.previewOutline(placement);
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Vec3 corner : RivetPlacement.previewCorners(outline)) {
            minX = Math.min(minX, corner.x);
            minY = Math.min(minY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxX = Math.max(maxX, corner.x);
            maxY = Math.max(maxY, corner.y);
            maxZ = Math.max(maxZ, corner.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static PreviewOutline previewOutline(Resolved placement) {
        double h = 0.3125;
        double hd = 0.0625;
        Quaterniond orient = RivetPlacement.plotOrientationForFace(placement.face(), placement.yRotationDegrees());
        Matrix3d basis = new Matrix3d().set((Quaterniondc)orient);
        Vector3d axisRight = basis.getColumn(0, new Vector3d());
        Vector3d axisUp = basis.getColumn(1, new Vector3d());
        Vector3d axisForward = basis.getColumn(2, new Vector3d());
        return new PreviewOutline(RivetPlacement.faceAnchorPlot(placement), RivetPlacement.vec3(axisRight), RivetPlacement.vec3(axisUp), RivetPlacement.vec3(axisForward), h, hd, h);
    }

    private static Vec3[] previewCorners(PreviewOutline outline) {
        Vec3 center = outline.center();
        Vec3 axisRight = outline.axisRight();
        Vec3 axisUp = outline.axisUp();
        Vec3 axisForward = outline.axisForward();
        Vec3[] corners = new Vec3[8];
        int idx = 0;
        for (int sx = 0; sx < 2; ++sx) {
            for (int sy = 0; sy < 2; ++sy) {
                for (int sz = 0; sz < 2; ++sz) {
                    double fx = sx == 0 ? -1.0 : 1.0;
                    double fy = sy == 0 ? -1.0 : 1.0;
                    double fz = sz == 0 ? -1.0 : 1.0;
                    corners[idx++] = center.add(axisRight.scale(fx * outline.halfRight())).add(axisUp.scale(fy * outline.halfUp())).add(axisForward.scale(fz * outline.halfForward()));
                }
            }
        }
        return corners;
    }

    private static Vec3 vec3(Vector3d v) {
        return new Vec3(v.x, v.y, v.z);
    }

    public static Vec3 faceAnchorPlot(Resolved placement) {
        return RivetPlacement.faceAnchorPlot(placement.host(), placement.face(), placement.lx(), placement.ly(), placement.lz());
    }

    public static Vec3 faceAnchorPlot(BlockPos host, Direction face, double lx, double ly, double lz) {
        return new Vec3((double)host.getX() + lx, (double)host.getY() + ly, (double)host.getZ() + lz);
    }

    public static Vec3 faceAnchorWorld(Level level, Resolved placement) {
        Vec3 plotAnchor = RivetPlacement.faceAnchorPlot(placement);
        SubLevel hostSub = Sable.HELPER.getContaining(level, (Vec3i)placement.host());
        if (hostSub == null) {
            return plotAnchor;
        }
        return hostSub.logicalPose().transformPosition(plotAnchor);
    }

    public static Vec3 hostFaceOutwardWorld(Level level, Resolved placement) {
        Vec3 local = Vec3.atLowerCornerOf((Vec3i)placement.face().getNormal());
        SubLevel hostSub = Sable.HELPER.getContaining(level, (Vec3i)placement.host());
        if (hostSub == null) {
            return local;
        }
        Vector3d n = new Vector3d(local.x, local.y, local.z);
        hostSub.logicalPose().orientation().transform(n);
        return new Vec3(n.x, n.y, n.z);
    }

    public static BlockState hostBlockState(Level level, RivetHostKey host) {
        return level.getBlockState(host.cell());
    }

    public static Quaterniond worldOrientation(Level level, Resolved placement) {
        Quaterniond local = RivetPlacement.plotOrientationForFace(placement.face(), placement.yRotationDegrees());
        SubLevel hostSub = Sable.HELPER.getContaining(level, (Vec3i)placement.host());
        if (hostSub == null) {
            return local;
        }
        return hostSub.logicalPose().orientation().mul((Quaterniondc)local, new Quaterniond());
    }

    public static Attachment attachment(Resolved placement) {
        return new Attachment(placement.lx(), placement.ly(), placement.lz());
    }

    public static Attachment rotateAttachment(Direction face, Attachment attachment, SubLevelAssemblyHelper.AssemblyTransform transform) {
        Rotation rotation = transform.getRotation();
        if (rotation == Rotation.NONE) {
            return attachment;
        }
        Direction newFace = rotation.rotate(face);
        Vec3 blockLocal = RivetPlacement.attachmentBlockLocal(face, attachment.lx(), attachment.ly(), attachment.lz());
        Vec3 centered = blockLocal.subtract(0.5, 0.5, 0.5);
        Vec3 rotated = centered.yRot(RivetPlacement.yRotRadians(rotation)).add(0.5, 0.5, 0.5);
        return RivetPlacement.attachmentOnFace(newFace, rotated);
    }

    private static float yRotRadians(Rotation rotation) {
        return switch (rotation) {
            default -> throw new MatchException(null, null);
            case Rotation.NONE -> 0.0f;
            case Rotation.CLOCKWISE_90 -> -1.5707964f;
            case Rotation.CLOCKWISE_180 -> (float)(-Math.PI);
            case Rotation.COUNTERCLOCKWISE_90 -> 1.5707964f;
        };
    }

    private static Vec3 attachmentBlockLocal(Direction face, double lx, double ly, double lz) {
        return new Vec3(lx, ly, lz);
    }

    private static Attachment attachmentOnFace(Direction face, Vec3 blockLocal) {
        double lx = Mth.clamp((double)(blockLocal.x - Math.floor(blockLocal.x)), (double)0.0, (double)1.0);
        double ly = Mth.clamp((double)(blockLocal.y - Math.floor(blockLocal.y)), (double)0.0, (double)1.0);
        double lz = Mth.clamp((double)(blockLocal.z - Math.floor(blockLocal.z)), (double)0.0, (double)1.0);
        return new Attachment(lx, ly, lz);
    }

    private static double snapHalfBlock(double local) {
        return Mth.clamp((double)((double)Math.round(local / 0.5) * 0.5), (double)0.0, (double)1.0);
    }

    public static Quaterniond plotOrientationForFace(Direction face) {
        return RivetPlacement.plotOrientationForFace(face, 0);
    }

    public static Quaterniond plotOrientationForFace(Direction face, int yRotationDegrees) {
        Vector3d outward = new Vector3d((double)face.getStepX(), (double)face.getStepY(), (double)face.getStepZ());
        Vector3d xAxis = new Vector3d();
        Vector3d zAxis = new Vector3d();
        Vector3d worldUp = new Vector3d(0.0, 1.0, 0.0);
        Vector3d worldSouth = new Vector3d(0.0, 0.0, 1.0);
        if (face.getAxis() == Direction.Axis.Y) {
            worldSouth.cross((Vector3dc)outward, xAxis).normalize();
            if (face == Direction.DOWN) {
                xAxis.negate();
            }
        } else {
            outward.cross((Vector3dc)worldUp, xAxis).normalize();
        }
        xAxis.cross((Vector3dc)outward, zAxis).normalize();
        Matrix3d basis = new Matrix3d();
        basis.setColumn(0, (Vector3dc)xAxis);
        basis.setColumn(1, (Vector3dc)outward);
        basis.setColumn(2, (Vector3dc)zAxis);
        Quaterniond orientation = new Quaterniond().setFromNormalized((Matrix3dc)basis);
        if (yRotationDegrees != 0) {
            orientation.mul((Quaterniondc)new Quaterniond().rotateY(Math.toRadians(yRotationDegrees)));
        }
        return orientation;
    }

    public static Vec3 rivetOutwardAnchorPlot(BlockPos plotCell) {
        return new Vec3((double)plotCell.getX() + 0.5, (double)plotCell.getY() + 1.0, (double)plotCell.getZ() + 0.5);
    }

    public static Vec3 hostDistalAnchorPlot(BlockPos host, Direction face, double lx, double ly, double lz) {
        return RivetPlacement.faceAnchorPlot(host, face, lx, ly, lz).add(Vec3.atLowerCornerOf((Vec3i)face.getNormal()).scale(3.0));
    }

    public static Vec3 rivetDistalAnchorPlot(BlockPos plotCell) {
        return RivetPlacement.rivetOutwardAnchorPlot(plotCell).add(0.0, 3.0, 0.0);
    }

    public static Vec3 hostLateralAnchorPlot(BlockPos host, Direction face, double lx, double ly, double lz, int yRotationDegrees) {
        Quaterniond orientation = RivetPlacement.plotOrientationForFace(face, yRotationDegrees);
        Vector3d lateral = new Vector3d(1.0, 0.0, 0.0);
        orientation.transform(lateral);
        return RivetPlacement.faceAnchorPlot(host, face, lx, ly, lz).add(new Vec3(lateral.x, lateral.y, lateral.z).scale(3.0));
    }

    public static Vec3 rivetLateralAnchorPlot(BlockPos plotCell) {
        return RivetPlacement.rivetOutwardAnchorPlot(plotCell).add(3.0, 0.0, 0.0);
    }

    public record Resolved(BlockPos host, Direction face, double lx, double ly, double lz, int yRotationDegrees) {
        public Resolved(BlockPos host, Direction face, double lx, double ly, double lz) {
            this(host, face, lx, ly, lz, 0);
        }

        public Resolved withYRotation(int degrees) {
            return new Resolved(this.host, this.face, this.lx, this.ly, this.lz, RivetPlacement.wrapYRotationDegrees(degrees));
        }
    }

    public record PreviewOutline(Vec3 center, Vec3 axisRight, Vec3 axisUp, Vec3 axisForward, double halfRight, double halfUp, double halfForward) {
    }

    public record Attachment(double lx, double ly, double lz) {
    }
}

