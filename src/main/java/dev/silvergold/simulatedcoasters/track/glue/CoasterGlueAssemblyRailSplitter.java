package dev.silvergold.simulatedcoasters.track.glue;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurveDrops;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public final class CoasterGlueAssemblyRailSplitter {
   private CoasterGlueAssemblyRailSplitter() {
   }

   public static void splitRailsSpanningGlueBoundary(ServerLevel level, Iterable<BlockPos> glueBlocks) {
      Set<BlockPos> glue = new HashSet<>();

      for (BlockPos p : glueBlocks) {
         if (p != null) {
            glue.add(p.immutable());
         }
      }

      if (!glue.isEmpty()) {
         for (BlockPos pos : glue) {
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity anchor) {
               separateAnchorsTowardOutsidePeers(level, glue, anchor);
            }
         }
      }
   }

   private static void separateAnchorsTowardOutsidePeers(ServerLevel level, Set<BlockPos> glue, CoasterAnchorpointBlockEntity anchor) {
      for (BlockPos peer : Set.copyOf(anchor.getAnchorPeerCurvesView().keySet())) {
         if (!glue.contains(peer)) {
            BlockPos here = anchor.getBlockPos();
            BezierConnection bc = anchor.getAnchorPeerCurvesView().get(peer);
            if (bc != null) {
               BlockState anchorState = anchor.getBlockState();
               SoundType sound = anchorState.getSoundType();
               spawnCurveDropsDestroyFx(level, bc, here, sound);
               anchor.removeAnchorPeerCurve(level, peer);
            }
         }
      }
   }

   private static void spawnCurveDropsDestroyFx(ServerLevel level, BezierConnection bc, BlockPos soundPos, SoundType sound) {
      if (bc != null) {
         CoasterTrackCurveDrops.spawnItems(level, bc);
         bc.spawnDestroyParticles(level);
         if (sound != null) {
            level.playSound(null, soundPos, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
         }
      }
   }
}
