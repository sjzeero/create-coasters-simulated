package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.foundation.block.BigOutlines;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BigOutlines.class})
public class BigOutlinesCoasterMixin {
   private static final double RAY_T_EPS = 0.001;

   @Inject(
      method = {"pick"},
      at = {@At("RETURN")}
   )
   private static void simulatedcoasters$anchorPeerCurveBigOutlineAlongRay(CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (!(mc.cameraEntity instanceof LocalPlayer player) || mc.level == null) {
         return;
      }

      if (!AnchorPeerCurveHit.isActive()) {
         if (!PickInteractionGates.isDestroyInputDown(mc)) {
            if (mc.hitResult instanceof BlockHitResult bhr
               && mc.hitResult.getType() == Type.BLOCK
               && AnchorPeerFakeTracks.isManagedFakeTrack(mc.level, bhr.getBlockPos())) {
               return;
            }

            Vec3 origin = player.getEyePosition(AnimationTickHolder.getPartialTicks(mc.level));
            double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
            double maxReachSq = range * range;
            Vec3 target = RaycastHelper.getTraceTarget(player, range + 1.0, origin);
            Vec3 ray = target.subtract(origin);
            double rayLen = ray.length();
            if (!(rayLen < 1.0E-9)) {
               Vec3 dir = ray.scale(1.0 / rayLen);
               double bestDistSq = Double.POSITIVE_INFINITY;
               double bestRayT = Double.POSITIVE_INFINITY;
               if (mc.hitResult instanceof BlockHitResult bhr && mc.hitResult.getType() == Type.BLOCK) {
                  Vec3 hp = bhr.getLocation();
                  bestDistSq = Sable.HELPER.distanceSquaredWithSubLevels(mc.level, origin, hp);
                  bestRayT = Math.max(0.0, hp.subtract(origin).dot(dir));
               }

               BlockHitResult bestHit = null;
               double bestCandidateDistSq = Double.POSITIVE_INFINITY;

               for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
                  if (!anchor.isRemoved() && anchor.getLevel() == mc.level) {
                     BlockPos p = anchor.getBlockPos();
                     BlockState state = mc.level.getBlockState(p);
                     if (state.getBlock() instanceof CoasterAnchorpointBlock) {
                        BlockHitResult hit = clipAnchorInteractionShape(mc.level, p, state, origin, target);
                        if (hit != null) {
                           double t1 = hit.getLocation().subtract(origin).dot(dir);
                           if (!(bestRayT < Double.POSITIVE_INFINITY) || !(t1 >= bestRayT - 0.001)) {
                              double d = Sable.HELPER.distanceSquaredWithSubLevels(mc.level, origin, hit.getLocation());
                              if (!(d > maxReachSq) && d < bestCandidateDistSq) {
                                 bestCandidateDistSq = d;
                                 bestHit = hit;
                              }
                           }
                        }
                     }
                  }
               }

               if (bestHit != null) {
                  if (!(bestDistSq < Double.POSITIVE_INFINITY) || bestCandidateDistSq + 5.0E-5 < bestDistSq) {
                     BlockPos hitPos = bestHit.getBlockPos();
                     Vec3 vec = bestHit.getLocation();
                     vec = vec.subtract(Vec3.atCenterOf(hitPos));
                     vec = VecHelper.clampComponentWise(vec, 1.0F);
                     vec = vec.add(Vec3.atCenterOf(hitPos));
                     mc.hitResult = new BlockHitResult(vec, bestHit.getDirection(), hitPos, bestHit.isInside());
                  }
               }
            }
         }
      }
   }

   @Nullable
   private static BlockHitResult clipAnchorInteractionShape(Level level, BlockPos anchorPos, BlockState state, Vec3 originWorld, Vec3 targetWorld) {
      SubLevel sub = Sable.HELPER.getContaining(level, anchorPos);
      Vec3 clipOrigin = originWorld;
      Vec3 clipTarget = targetWorld;
      if (sub != null) {
         Pose3dc pose = (Pose3dc)(level instanceof LevelPoseProviderExtension provider ? provider.sable$getPose(sub) : sub.logicalPose());
         clipOrigin = pose.transformPositionInverse(originWorld);
         clipTarget = pose.transformPositionInverse(targetWorld);
      }

      BlockHitResult hit = state.getInteractionShape(level, anchorPos).clip(clipOrigin, clipTarget, anchorPos);
      if (hit != null && sub != null) {
         Vec3 worldLoc = Sable.HELPER.projectOutOfSubLevel(level, hit.getLocation());
         return new BlockHitResult(worldLoc, hit.getDirection(), hit.getBlockPos(), hit.isInside());
      } else {
         return hit;
      }
   }
}
