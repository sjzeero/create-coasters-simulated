package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ChainLiftPlacementSounds {
   private ChainLiftPlacementSounds() {
   }

   public static Vec3 spanEndWorldPos(Level level, CoasterPathGraphSpans.SpanPath spanPath) {
      List<CoasterPathGraphSpans.PathSegment> segments = spanPath.segments();
      if (segments.isEmpty()) {
         return Vec3.ZERO;
      } else {
         CoasterPathGraphSpans.PathSegment last = segments.getLast();
         return CoasterPathGraphManager.sampleEdgePosition(level, last.edge(), last.tEnd(), null);
      }
   }

   public static void playChainCommit(Level level, Vec3 worldPos) {
      BlockState chain = Blocks.CHAIN.defaultBlockState();
      SoundType sound = chain.getSoundType(level, null, null);
      level.playSound(
         null, worldPos.x, worldPos.y, worldPos.z, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F
      );
   }
}
