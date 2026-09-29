package dev.silvergold.simulatedcoasters.balloon;

import com.simibubi.create.AllSoundEvents;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.SimulatedCoastersSoundEvents;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class BalloonRemoval {
   private static final Set<UUID> REMOVING = ConcurrentHashMap.newKeySet();
   private static final float POP_PITCH_SPREAD = 0.08F;

   private BalloonRemoval() {
   }

   public static boolean isRemoving(UUID balloonSubLevelId) {
      return REMOVING.contains(balloonSubLevelId);
   }

   public static void pop(ServerLevel level, ServerSubLevel balloonSub) {
      if (!balloonSub.isRemoved()) {
         if (REMOVING.add(balloonSub.getUniqueId())) {
            UUID balloonId = balloonSub.getUniqueId();
            UUID hostSubLevelId = BalloonCascadePop.hostSubLevelIdOf(level, balloonSub);
            Vector3d poppedPos = new Vector3d(balloonSub.logicalPose().position());

            try {
               playPopEffects(level, balloonSub);
               detachAndRemove(level, balloonSub, true);
            } finally {
               REMOVING.remove(balloonId);
            }

            BalloonCascadePop.onBalloonPopped(level, hostSubLevelId, poppedPos, balloonId);
         }
      }
   }

   public static boolean safelyRemove(ServerLevel level, ServerSubLevel balloonSub) {
      if (balloonSub.isRemoved()) {
         return false;
      } else if (!REMOVING.add(balloonSub.getUniqueId())) {
         return false;
      } else {
         UUID balloonId = balloonSub.getUniqueId();

         try {
            playPickupEffects(level, balloonSub);
            detachAndRemove(level, balloonSub, true);
         } finally {
            REMOVING.remove(balloonId);
         }

         return true;
      }
   }

   public static void onHostBroken(ServerLevel level, ServerSubLevel balloonSub) {
      pop(level, balloonSub);
   }

   public static void onBalloonBlockRemoved(ServerLevel level, ServerSubLevel balloonSub) {
      if (!balloonSub.isRemoved() && !isRemoving(balloonSub.getUniqueId())) {
         detachAndRemove(level, balloonSub, false);
      }
   }

   private static void detachAndRemove(ServerLevel level, ServerSubLevel balloonSub, boolean breakBalloonBlock) {
      if (!balloonSub.isRemoved()) {
         BalloonBlockEntity be = balloonBlockEntity(balloonSub);
         if (be != null) {
            BalloonHostIndex.get(level).unregisterBalloon(balloonSub.getUniqueId());
         }

         BalloonTetherController.unregister(balloonSub.getUniqueId());
         if (breakBalloonBlock) {
            EmbeddedPlotLevelAccessor embedded = balloonSub.getPlot().getEmbeddedLevelAccessor();
            BlockState state = embedded.getBlockState(BlockPos.ZERO);
            if (state.is((Block)SimulatedCoastersBlocks.RED_BALLOON.get())) {
               embedded.setBlock(BlockPos.ZERO, Blocks.AIR.defaultBlockState(), 3);
            }
         }

         if (!balloonSub.isRemoved()) {
            balloonSub.markRemoved();
         }
      }
   }

   private static void playPopEffects(ServerLevel level, ServerSubLevel balloonSub) {
      BlockPos cell = balloonSub.getPlot().getCenterBlock();
      BlockState state = balloonSub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO);
      double x = (double)cell.getX() + 0.5;
      double y = (double)cell.getY() + 0.5;
      double z = (double)cell.getZ() + 0.5;
      float pitch = 1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.08F;
      level.playSound(null, cell, (SoundEvent)SimulatedCoastersSoundEvents.BALLOON_POP.get(), SoundSource.BLOCKS, 1.0F, pitch);
      if (state.is((Block)SimulatedCoastersBlocks.RED_BALLOON.get())) {
         level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 12, 0.2, 0.2, 0.2, 0.05);
      }

      level.sendParticles(ParticleTypes.POOF, x, y, z, 8, 0.15, 0.15, 0.15, 0.02);
   }

   private static void playPickupEffects(ServerLevel level, ServerSubLevel balloonSub) {
      BlockPos cell = balloonSub.getPlot().getCenterBlock();
      float pitch = level.getRandom().nextFloat() * 0.5F + 0.5F;
      AllSoundEvents.WRENCH_REMOVE.playOnServer(level, cell, 1.0F, pitch);
   }

   @Nullable
   private static BalloonBlockEntity balloonBlockEntity(ServerSubLevel balloonSub) {
      EmbeddedPlotLevelAccessor embedded = balloonSub.getPlot().getEmbeddedLevelAccessor();
      BlockEntity var3 = embedded.getBlockEntity(BlockPos.ZERO);
      return var3 instanceof BalloonBlockEntity ? (BalloonBlockEntity)var3 : null;
   }
}
