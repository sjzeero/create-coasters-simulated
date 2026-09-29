/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.foundation.block.BigOutlines
 *  com.simibubi.create.foundation.utility.RaycastHelper
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.createmod.catnip.math.VecHelper
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.foundation.block.BigOutlines;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.PickInteractionGates;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BigOutlines.class})
public class BigOutlinesCoasterMixin {
    private static final double RAY_T_EPS = 0.001;

    @Inject(method={"pick"}, at={@At(value="RETURN")})
    private static void simulatedcoasters$anchorPeerCurveBigOutlineAlongRay(CallbackInfo ci) {
        LocalPlayer player;
        Minecraft mc;
        block14: {
            block13: {
                mc = Minecraft.getInstance();
                Entity entity = mc.cameraEntity;
                if (!(entity instanceof LocalPlayer)) break block13;
                player = (LocalPlayer)entity;
                if (mc.level != null) break block14;
            }
            return;
        }
        if (AnchorPeerCurveHit.isActive()) {
            return;
        }
        if (PickInteractionGates.isDestroyInputDown(mc)) {
            return;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult instanceof BlockHitResult) {
            BlockHitResult bhr = (BlockHitResult)hitResult;
            if (mc.hitResult.getType() == HitResult.Type.BLOCK && AnchorPeerFakeTracks.isManagedFakeTrack((Level)mc.level, bhr.getBlockPos())) {
                return;
            }
        }
        Vec3 origin = player.getEyePosition(AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level));
        double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        double maxReachSq = range * range;
        Vec3 target = RaycastHelper.getTraceTarget((Player)player, (double)(range + 1.0), (Vec3)origin);
        Vec3 ray = target.subtract(origin);
        double rayLen = ray.length();
        if (rayLen < 1.0E-9) {
            return;
        }
        Vec3 dir = ray.scale(1.0 / rayLen);
        double bestDistSq = Double.POSITIVE_INFINITY;
        double bestRayT = Double.POSITIVE_INFINITY;
        HitResult hitResult2 = mc.hitResult;
        if (hitResult2 instanceof BlockHitResult) {
            BlockHitResult bhr = (BlockHitResult)hitResult2;
            if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
                Vec3 hp = bhr.getLocation();
                bestDistSq = Sable.HELPER.distanceSquaredWithSubLevels((Level)mc.level, (Position)origin, (Position)hp);
                bestRayT = Math.max(0.0, hp.subtract(origin).dot(dir));
            }
        }
        BlockHitResult bestHit = null;
        double bestCandidateDistSq = Double.POSITIVE_INFINITY;
        for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
            double d;
            BlockHitResult hit;
            BlockPos p;
            BlockState state;
            if (anchor.isRemoved() || anchor.getLevel() != mc.level || !((state = mc.level.getBlockState(p = anchor.getBlockPos())).getBlock() instanceof CoasterAnchorpointBlock) || (hit = BigOutlinesCoasterMixin.clipAnchorInteractionShape((Level)mc.level, p, state, origin, target)) == null) continue;
            double t1 = hit.getLocation().subtract(origin).dot(dir);
            if (bestRayT < Double.POSITIVE_INFINITY && t1 >= bestRayT - 0.001 || (d = Sable.HELPER.distanceSquaredWithSubLevels((Level)mc.level, (Position)origin, (Position)hit.getLocation())) > maxReachSq || !(d < bestCandidateDistSq)) continue;
            bestCandidateDistSq = d;
            bestHit = hit;
        }
        if (bestHit == null) {
            return;
        }
        if (bestDistSq < Double.POSITIVE_INFINITY && !(bestCandidateDistSq + 5.0E-5 < bestDistSq)) {
            return;
        }
        BlockPos hitPos = bestHit.getBlockPos();
        Vec3 vec = bestHit.getLocation();
        vec = vec.subtract(Vec3.atCenterOf((Vec3i)hitPos));
        vec = VecHelper.clampComponentWise((Vec3)vec, (float)1.0f);
        vec = vec.add(Vec3.atCenterOf((Vec3i)hitPos));
        mc.hitResult = new BlockHitResult(vec, bestHit.getDirection(), hitPos, bestHit.isInside());
    }

    @Nullable
    private static BlockHitResult clipAnchorInteractionShape(Level level, BlockPos anchorPos, BlockState state, Vec3 originWorld, Vec3 targetWorld) {
        BlockHitResult hit;
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)anchorPos);
        Vec3 clipOrigin = originWorld;
        Vec3 clipTarget = targetWorld;
        if (sub != null) {
            Pose3d pose3d;
            if (level instanceof LevelPoseProviderExtension) {
                LevelPoseProviderExtension provider = (LevelPoseProviderExtension)level;
                pose3d = provider.sable$getPose(sub);
            } else {
                pose3d = sub.logicalPose();
            }
            Pose3d pose = pose3d;
            clipOrigin = pose.transformPositionInverse(originWorld);
            clipTarget = pose.transformPositionInverse(targetWorld);
        }
        if ((hit = state.getInteractionShape((BlockGetter)level, anchorPos).clip(clipOrigin, clipTarget, anchorPos)) == null || sub == null) {
            return hit;
        }
        Vec3 worldLoc = Sable.HELPER.projectOutOfSubLevel(level, hit.getLocation());
        return new BlockHitResult(worldLoc, hit.getDirection(), hit.getBlockPos(), hit.isInside());
    }
}

