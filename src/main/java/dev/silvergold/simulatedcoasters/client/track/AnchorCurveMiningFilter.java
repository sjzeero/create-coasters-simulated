package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class AnchorCurveMiningFilter {
   private static boolean active;
   private static long endpointPairMin;
   private static long endpointPairMax;
   @Nullable
   private static BlockPos endpointA;
   @Nullable
   private static BlockPos endpointB;

   private AnchorCurveMiningFilter() {
   }

   public static void beginMining(BlockPos mined, BlockPos peer) {
      endpointA = mined.immutable();
      endpointB = peer.immutable();
      long la = endpointA.asLong();
      long lb = endpointB.asLong();
      endpointPairMin = Math.min(la, lb);
      endpointPairMax = Math.max(la, lb);
      active = true;
   }

   public static void clear() {
      active = false;
      endpointB = null;
      endpointA = null;
      endpointPairMax = 0L;
      endpointPairMin = 0L;
   }

   public static boolean suppressAnchorBlockBreakTexture(BlockPos pos) {
      return active && endpointA != null && endpointB != null ? pos.equals(endpointA) || pos.equals(endpointB) : false;
   }

   public static boolean crumblingMatches(BezierConnection bc) {
      if (active && endpointA != null && endpointB != null) {
         BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
         long la = ((BlockPos)p.bePositions.getFirst()).asLong();
         long lb = ((BlockPos)p.bePositions.getSecond()).asLong();
         long mn = Math.min(la, lb);
         long mx = Math.max(la, lb);
         return mn == endpointPairMin && mx == endpointPairMax;
      } else {
         return true;
      }
   }
}
