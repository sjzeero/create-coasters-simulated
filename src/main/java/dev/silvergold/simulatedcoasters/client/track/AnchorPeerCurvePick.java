/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllShapes
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackBlockOutline
 *  com.simibubi.create.content.trains.track.TrackBlockOutline$BezierPointSelection
 *  com.simibubi.create.foundation.utility.RaycastHelper
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.math.BoundingBox3d
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.createmod.catnip.math.AngleHelper
 *  net.createmod.catnip.math.VecHelper
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.client.track.TrackOutlineBezierAccess;
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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
        if (sub == null) {
            return bc.getBounds();
        }
        return new BoundingBox3d(bc.getBounds()).transform(sub.renderPose(pt)).toMojang();
    }

    private static Vec3 curvePickMidWorld(Vec3 plotLocalMid, BlockPos anchorPos, Minecraft mc, float pt) {
        ClientSubLevel sub = (ClientSubLevel)Sable.HELPER.getContaining((Level)mc.level, (Vec3i)anchorPos);
        Pose3d rp = AnchorPeerCurvePick.renderPose(sub, pt);
        return rp == null ? plotLocalMid : rp.transformPosition(plotLocalMid);
    }

    private static Vec3 anchorCenterForCurveCompare(Level level, BlockPos anchorEp, Minecraft mc, float pt) {
        Vec3 c = CoasterAnchorpointBlockEntity.worldCenter(level, anchorEp);
        ClientSubLevel epSub = (ClientSubLevel)Sable.HELPER.getContaining((Level)mc.level, (Vec3i)anchorEp);
        Pose3d rp = AnchorPeerCurvePick.renderPose(epSub, pt);
        return rp == null ? c : rp.transformPosition(c);
    }

    private static boolean isCoasterMaterial(BezierConnection bc) {
        return bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id);
    }

    private static void tryWireTrackTrackCoasterHit(Minecraft mc, @Nullable TrackBlockOutline.BezierPointSelection trackSel, double trackDistSq, double blockDistSq) {
        if (trackSel == null) {
            return;
        }
        BezierConnection bc = TrackOutlineBezierAccess.primaryBezierForSelection(trackSel);
        if (bc == null || !AnchorPeerCurvePick.isCoasterMaterial(bc)) {
            return;
        }
        if (!(trackDistSq + 5.0E-5 < blockDistSq)) {
            return;
        }
        mc.hitResult = BlockHitResult.miss((Vec3)trackSel.vec(), (Direction)Direction.UP, (BlockPos)BlockPos.ZERO);
    }

    @Nullable
    public static BlockHitResult syntheticMiningHit(Minecraft mc) {
        return AnchorPeerCurvePick.syntheticCurveBlockHit(mc, false);
    }

    @Nullable
    public static BlockHitResult syntheticPickBlockHit(Minecraft mc) {
        return AnchorPeerCurvePick.syntheticCurveBlockHit(mc, true);
    }

    @Nullable
    private static BlockHitResult syntheticCurveBlockHit(Minecraft mc, boolean allowCreateTrackOutline) {
        if (AnchorPeerCurveHit.isActive() && mc.level != null) {
            BlockPos mine = AnchorPeerCurveHit.rasterBlockPos();
            if (mine == null) {
                mine = AnchorPeerCurveHit.focusAnchor();
            }
            if (mine != null) {
                Vec3 loc;
                Vec3 vec3 = loc = mc.hitResult != null ? mc.hitResult.getLocation() : Vec3.atCenterOf((Vec3i)mine);
                if (AnchorPeerCurveHit.rasterBlockPos() != null && AnchorPeerFakeTracks.isManagedFakeTrack((Level)mc.level, mine)) {
                    loc = AnchorPeerFakeTracks.clampInteractionInsideCell(loc, mine);
                }
                return new BlockHitResult(loc, Direction.UP, mine, false);
            }
        }
        if (!allowCreateTrackOutline || mc.level == null) {
            return null;
        }
        TrackBlockOutline.BezierPointSelection sel = TrackBlockOutline.result;
        if (sel == null) {
            return null;
        }
        BezierConnection bc = TrackOutlineBezierAccess.primaryBezierForSelection(sel);
        if (bc == null || !AnchorPeerCurvePick.isCoasterMaterial(bc)) {
            return null;
        }
        BlockPos pos = sel.blockEntity().getBlockPos();
        return new BlockHitResult(sel.vec(), Direction.UP, pos, false);
    }

    private static boolean shouldIgnoreBlockHitForCurvePick(Level level, BlockPos hitPos) {
        CoasterAnchorpointBlockEntity be;
        CoasterAnchorpointBlockEntity anchorBe;
        BlockEntity blockEntity;
        if (AnchorPeerFakeTracks.isManagedFakeTrack(level, hitPos)) {
            return false;
        }
        if (level.getBlockState(hitPos).getBlock() instanceof CoasterAnchorpointBlock && (blockEntity = level.getBlockEntity(hitPos)) instanceof CoasterAnchorpointBlockEntity && !(anchorBe = (CoasterAnchorpointBlockEntity)blockEntity).getAnchorPeerCurvesView().isEmpty()) {
            return true;
        }
        BlockPos below = hitPos.below();
        BlockEntity blockEntity2 = level.getBlockEntity(below);
        return blockEntity2 instanceof CoasterAnchorpointBlockEntity && !(be = (CoasterAnchorpointBlockEntity)blockEntity2).getAnchorPeerCurvesView().isEmpty();
    }

    private static double blockDistSqForCurveCompare(Minecraft mc, Vec3 origin) {
        BlockHitResult bhr;
        block5: {
            block4: {
                HitResult hitResult = mc.hitResult;
                if (!(hitResult instanceof BlockHitResult)) break block4;
                bhr = (BlockHitResult)hitResult;
                if (mc.hitResult.getType() == HitResult.Type.BLOCK) break block5;
            }
            return Double.POSITIVE_INFINITY;
        }
        if (AnchorPeerCurvePick.shouldIgnoreBlockHitForCurvePick((Level)mc.level, bhr.getBlockPos())) {
            return Double.POSITIVE_INFINITY;
        }
        return Sable.HELPER.distanceSquaredWithSubLevels((Level)mc.level, (Position)origin, (Position)bhr.getLocation());
    }

    public static void refineAfterCreatePass(Minecraft mc) {
        BlockPos owningAnchorPos;
        BlockHitResult bhr0;
        HitResult hitResult;
        LocalPlayer player;
        block20: {
            block19: {
                Entity entity = mc.cameraEntity;
                if (!(entity instanceof LocalPlayer)) break block19;
                player = (LocalPlayer)entity;
                if (mc.level != null) break block20;
            }
            return;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
        Vec3 origin = Sable.HELPER.getEyePositionInterpolated((Entity)player, pt);
        double maxRange = Double.MAX_VALUE;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK && (hitResult = mc.hitResult) instanceof BlockHitResult && !AnchorPeerCurvePick.shouldIgnoreBlockHitForCurvePick((Level)mc.level, (bhr0 = (BlockHitResult)hitResult).getBlockPos())) {
            maxRange = Sable.HELPER.distanceSquaredWithSubLevels((Level)mc.level, (Position)origin, (Position)bhr0.getLocation());
        } else if (mc.hitResult != null) {
            maxRange = mc.hitResult.getLocation().distanceToSqr(origin);
        }
        double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vec3 target = RaycastHelper.getTraceTarget((Player)player, (double)(Math.min(maxRange, range) + 1.0), (Vec3)origin);
        Vec3 ray = target.subtract(origin);
        double rayLen = ray.length();
        if (rayLen < 1.0E-6) {
            return;
        }
        // Could not load outer class - annotation placement on inner may be incorrect
        @Nullable TrackBlockOutline.BezierPointSelection trackSel = TrackBlockOutline.result;
        double trackDistSq = trackSel == null ? Double.POSITIVE_INFINITY : origin.distanceToSqr(trackSel.vec());
        double blockDistSq = AnchorPeerCurvePick.blockDistSqForCurveCompare(mc, origin);
        double bestAnchorDistSq = Double.POSITIVE_INFINITY;
        BezierConnection bestBc = null;
        Vec3 bestVec = null;
        Vec3 bestAngles = null;
        Vec3 bestDir = null;
        BlockPos bestMidHostAnchorPos = null;
        for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
            if (anchor.isRemoved() || anchor.getLevel() != mc.level) continue;
            owningAnchorPos = anchor.getBlockPos();
            ClientSubLevel anchorSub = (ClientSubLevel)Sable.HELPER.getContaining((Level)mc.level, (Vec3i)owningAnchorPos);
            Pose3d anchorRp = AnchorPeerCurvePick.renderPose(anchorSub, pt);
            for (BezierConnection bcRaw : anchor.getAnchorPeerCurvesView().values()) {
                AABB bounds;
                BezierConnection bc;
                if (bcRaw == null || AnchorPeerCurvePendingRemoval.isPending(bc = bcRaw.isPrimary() ? bcRaw : bcRaw.secondary()) || !(bounds = AnchorPeerCurvePick.curveBoundsWorld(bc, anchorSub, pt)).contains(origin) && bounds.clip(origin, target).isEmpty()) continue;
                float[] stepLUT = bc.getStepLUT();
                int segments = (int)(bc.getLength() * 2.0);
                AABB segmentBounds = AnchorPeerCurvePick.isCoasterMaterial(bc) ? CoasterCurvePickGeometry.curvePickCrossSection().bounds() : AllShapes.TRACK_ORTHO.get(Direction.SOUTH).bounds();
                segmentBounds = segmentBounds.move(-0.5, segmentBounds.getYsize() / -2.0, -0.5);
                int bestSegment = -1;
                double bestDistance = Double.MAX_VALUE;
                double newMaxRange = maxRange;
                Vec3 pickMid = null;
                Vec3 pickAngles = null;
                Vec3 pickDir = null;
                for (int i = 0; i < stepLUT.length - 2; ++i) {
                    double distanceToSqr;
                    Vec3 localDirection;
                    Vec3 localOrigin;
                    float t = stepLUT[i] * (float)i / (float)segments;
                    float t1 = stepLUT[i + 1] * (float)(i + 1) / (float)segments;
                    float t2 = stepLUT[i + 2] * (float)(i + 2) / (float)segments;
                    Vec3 v1 = bc.getPosition((double)t);
                    Vec3 v2 = bc.getPosition((double)t2);
                    Vec3 diff = v2.subtract(v1);
                    Vec3 angles = CoasterBezierRailFrames.modelAnglesForSegment(bc, t1, diff, (Level)mc.level);
                    Vec3 mid = v1.add(diff.scale(0.5));
                    if (AnchorPeerCurvePick.isCoasterMaterial(bc)) {
                        mid = mid.add(CoasterBezierRailFrames.curveInteractionLiftOffset(bc, t1, (Level)mc.level));
                    }
                    if (anchorRp == null) {
                        localOrigin = origin.subtract(mid);
                        localDirection = target.subtract(origin);
                    } else {
                        Vec3 invO = anchorRp.transformPositionInverse(origin);
                        Vec3 invT = anchorRp.transformPositionInverse(target);
                        localOrigin = invO.subtract(mid);
                        localDirection = invT.subtract(invO);
                    }
                    localOrigin = VecHelper.rotate((Vec3)localOrigin, (double)AngleHelper.deg((double)(-angles.x)), (Direction.Axis)Direction.Axis.X);
                    localOrigin = VecHelper.rotate((Vec3)localOrigin, (double)AngleHelper.deg((double)(-angles.y)), (Direction.Axis)Direction.Axis.Y);
                    localDirection = VecHelper.rotate((Vec3)localDirection, (double)AngleHelper.deg((double)(-angles.x)), (Direction.Axis)Direction.Axis.X);
                    localDirection = VecHelper.rotate((Vec3)localDirection, (double)AngleHelper.deg((double)(-angles.y)), (Direction.Axis)Direction.Axis.Y);
                    Optional clip = segmentBounds.clip(localOrigin, localOrigin.add(localDirection));
                    if (clip.isEmpty() || bestSegment != -1 && bestDistance < ((Vec3)clip.get()).distanceToSqr(0.0, 0.25, 0.0) || (distanceToSqr = ((Vec3)clip.get()).distanceToSqr(localOrigin)) > maxRange) continue;
                    bestSegment = i;
                    newMaxRange = distanceToSqr;
                    bestDistance = ((Vec3)clip.get()).distanceToSqr(0.0, 0.25, 0.0);
                    pickMid = mid;
                    pickAngles = angles;
                    pickDir = diff.normalize();
                }
                if (bestSegment == -1 || pickMid == null || pickAngles == null || pickDir == null) continue;
                maxRange = newMaxRange;
                Vec3 pickMidWorld = AnchorPeerCurvePick.curvePickMidWorld(pickMid, owningAnchorPos, mc, pt);
                double anchorPickDistSq = Sable.HELPER.distanceSquaredWithSubLevels((Level)mc.level, (Position)origin, (Position)pickMidWorld);
                if (!(anchorPickDistSq < bestAnchorDistSq)) continue;
                bestAnchorDistSq = anchorPickDistSq;
                bestBc = bc;
                bestVec = pickMid;
                bestAngles = pickAngles;
                bestDir = pickDir;
                bestMidHostAnchorPos = owningAnchorPos;
            }
        }
        if (bestBc == null || bestVec == null || bestAngles == null || bestDir == null || bestMidHostAnchorPos == null || AnchorPeerCurvePendingRemoval.isPending(bestBc)) {
            AnchorPeerCurveHit.clear();
            AnchorPeerCurvePick.tryWireTrackTrackCoasterHit(mc, trackSel, trackDistSq, blockDistSq);
            return;
        }
        if (!(bestAnchorDistSq + 5.0E-5 < blockDistSq) || !(bestAnchorDistSq + 5.0E-5 < trackDistSq)) {
            AnchorPeerCurveHit.clear();
            AnchorPeerCurvePick.tryWireTrackTrackCoasterHit(mc, trackSel, trackDistSq, blockDistSq);
            return;
        }
        Vec3 bestMidWorld = AnchorPeerCurvePick.curvePickMidWorld(bestVec, bestMidHostAnchorPos, mc, pt);
        owningAnchorPos = mc.hitResult;
        if (owningAnchorPos instanceof BlockHitResult) {
            BlockHitResult bhrMc = (BlockHitResult)owningAnchorPos;
            if (mc.hitResult.getType() == HitResult.Type.BLOCK && !AnchorPeerCurvePick.shouldIgnoreBlockHitForCurvePick((Level)mc.level, bhrMc.getBlockPos())) {
                double blockT = Math.max(0.0, bhrMc.getLocation().subtract(origin).dot(ray) / rayLen);
                double anchorT = Math.max(0.0, bestMidWorld.subtract(origin).dot(ray) / rayLen);
                if (anchorT >= blockT - 0.001) {
                    AnchorPeerCurveHit.clear();
                    AnchorPeerCurvePick.tryWireTrackTrackCoasterHit(mc, trackSel, trackDistSq, blockDistSq);
                    return;
                }
            }
        }
        BlockPos first = (BlockPos)bestBc.bePositions.getFirst();
        BlockPos second = (BlockPos)bestBc.bePositions.getSecond();
        ClientLevel level = mc.level;
        Vec3 c1w = AnchorPeerCurvePick.anchorCenterForCurveCompare((Level)level, first, mc, pt);
        Vec3 c2w = AnchorPeerCurvePick.anchorCenterForCurveCompare((Level)level, second, mc, pt);
        BlockPos focus = bestMidWorld.distanceToSqr(c1w) <= bestMidWorld.distanceToSqr(c2w) ? first : second;
        BlockPos hitBlock = AnchorPeerFakeTracks.blockPosForCurveHit(bestBc, bestMidWorld);
        if (hitBlock == null) {
            hitBlock = focus;
        }
        BlockPos rasterBlock = AnchorPeerFakeTracks.rasterContains(bestBc, hitBlock) ? hitBlock : null;
        TrackBlockOutline.result = null;
        AnchorPeerCurveHit.set(bestBc, bestVec, bestAngles, bestDir, focus, rasterBlock);
        mc.hitResult = BlockHitResult.miss((Vec3)bestMidWorld, (Direction)Direction.UP, (BlockPos)BlockPos.ZERO);
    }
}

