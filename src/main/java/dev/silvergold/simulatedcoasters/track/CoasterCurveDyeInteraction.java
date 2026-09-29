package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveRailInstancing;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterCurveDyeInteraction {
   private CoasterCurveDyeInteraction() {
   }

   public static void applyRailTintFromPreview(ServerPlayer player, BlockPos curveOwner, BlockPos curveRemote, Vec3 soundPos, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.getItem() instanceof DyeItem dyeItem) {
         ServerLevel level = player.serverLevel();
         if (validateHostedCurveEndpoints(level, curveOwner, curveRemote)) {
            if (applyRailTint(level, curveOwner, curveRemote, dyeItem)) {
               playCurveTintSound(level, soundPos, SoundEvents.HONEYCOMB_WAX_ON, 0.55F);
               player.swing(hand, true);
            }
         }
      }
   }

   public static void clearDyeFromPreview(ServerPlayer player, BlockPos curveOwner, BlockPos curveRemote, Vec3 soundPos, InteractionHand hand) {
      if (player.getItemInHand(hand).getItem() instanceof AxeItem) {
         ServerLevel level = player.serverLevel();
         if (validateHostedCurveEndpoints(level, curveOwner, curveRemote)) {
            if (clearDyeAtCurve(level, curveOwner, curveRemote, soundPos)) {
               player.swing(hand, true);
            }
         }
      }
   }

   private static boolean applyRailTint(Level level, BlockPos owner, BlockPos remote, DyeItem dyeItem) {
      int diffuseRgb = CoasterCurveTintColors.diffuseRgbFromDye(dyeItem.getDyeColor());
      int beam = getStoredCurveBeamRgb(level, owner, remote);
      return applyTieredTintsToOwner(level, owner, remote, diffuseRgb, beam);
   }

   static boolean validateHostedCurveEndpoints(Level level, BlockPos owner, BlockPos remote) {
      if (!(level.getBlockEntity(owner) instanceof CoasterAnchorpointBlockEntity anchor)) {
         return false;
      } else {
         BezierConnection raw = anchor.getAnchorPeerCurvesView().get(remote);
         if (raw == null) {
            return false;
         } else {
            BezierConnection primary = raw.isPrimary() ? raw : raw.secondary();
            if (!primary.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
               return false;
            } else {
               return !CoasterCurveRailInstancing.useSplineRails(primary)
                  ? false
                  : ((BlockPos)primary.bePositions.getFirst()).equals(owner) && ((BlockPos)primary.bePositions.getSecond()).equals(remote);
            }
         }
      }
   }

   private static boolean clearDyeAtCurve(Level level, BlockPos owner, BlockPos remote, Vec3 soundPos) {
      BezierConnection primary = primaryForHostedEndpoints(level, owner, remote);
      if (primary == null) {
         return false;
      } else {
         int rail = getStoredCurveRailRgb(level, owner, remote);
         int beam = getStoredCurveBeamRgb(level, owner, remote);
         CoasterCurveTieredDye.AxeStep step = CoasterCurveTieredDye.nextAxeStep(rail, beam);
         if (step.isNoOp()) {
            return false;
         } else if (!applyTieredTintsToOwner(level, owner, remote, step.newRailRgb(), step.newBeamRgb())) {
            return false;
         } else {
            if (level instanceof ServerLevel sl) {
               int cleared = step.clearedRgbForParticles();
               if (cleared != 0) {
                  CoasterCurveUndyeParticles.spawnAlongCurve(sl, primary, cleared);
               }

               playCurveTintSound(sl, soundPos, SoundEvents.AXE_WAX_OFF, 0.6F);
            }

            return true;
         }
      }
   }

   private static void playCurveTintSound(ServerLevel level, Vec3 soundPos, SoundEvent event, float volume) {
      level.playSound(null, soundPos.x, soundPos.y, soundPos.z, event, SoundSource.PLAYERS, volume, 1.0F);
   }

   @Nullable
   private static BezierConnection primaryForHostedEndpoints(Level level, BlockPos owner, BlockPos remote) {
      if (level.getBlockEntity(owner) instanceof CoasterAnchorpointBlockEntity anchor) {
         BezierConnection raw = anchor.getAnchorPeerCurvesView().get(remote);
         if (raw == null) {
            return null;
         } else {
            BezierConnection primary = raw.isPrimary() ? raw : raw.secondary();
            if (!primary.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
               return null;
            } else {
               return !CoasterCurveRailInstancing.useSplineRails(primary) ? null : primary;
            }
         }
      } else {
         return null;
      }
   }

   private static int getStoredCurveRailRgb(Level level, BlockPos owner, BlockPos remote) {
      return level.getBlockEntity(owner) instanceof CoasterAnchorpointBlockEntity ape ? ape.getPeerCurveRailDiffuseRgb(remote) : 0;
   }

   private static int getStoredCurveBeamRgb(Level level, BlockPos owner, BlockPos remote) {
      return level.getBlockEntity(owner) instanceof CoasterAnchorpointBlockEntity ape ? ape.getPeerCurveBeamDiffuseRgb(remote) : 0;
   }

   private static boolean applyTieredTintsToOwner(Level level, BlockPos owner, BlockPos remote, int railRgb, int beamRgb) {
      return level.getBlockEntity(owner) instanceof CoasterAnchorpointBlockEntity ape ? ape.setPeerCurveTieredTints(remote, railRgb, beamRgb) : false;
   }
}
