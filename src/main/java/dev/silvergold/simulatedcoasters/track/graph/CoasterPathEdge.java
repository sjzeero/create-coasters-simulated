package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record CoasterPathEdge(
   BlockPos from,
   BlockPos to,
   @Nullable Vec3 straightFromWorld,
   @Nullable Vec3 straightToWorld,
   @Nullable BezierConnection bezier,
   double rollStart,
   double rollEnd,
   int tangentSign
) {
   public CoasterPathEdge(
      BlockPos from,
      BlockPos to,
      @Nullable Vec3 straightFromWorld,
      @Nullable Vec3 straightToWorld,
      @Nullable BezierConnection bezier,
      double rollStart,
      double rollEnd,
      int tangentSign
   ) {
      tangentSign = tangentSign < 0 ? -1 : 1;
      this.from = from;
      this.to = to;
      this.straightFromWorld = straightFromWorld;
      this.straightToWorld = straightToWorld;
      this.bezier = bezier;
      this.rollStart = rollStart;
      this.rollEnd = rollEnd;
      this.tangentSign = tangentSign;
   }

   public CoasterPathEdge withTangentSign(int sign) {
      return new CoasterPathEdge(this.from, this.to, this.straightFromWorld, this.straightToWorld, this.bezier, this.rollStart, this.rollEnd, sign);
   }

   public static CoasterPathEdge straight(BlockPos blockA, BlockPos blockB, Vec3 fromWorld, Vec3 toWorld) {
      return new CoasterPathEdge(blockA, blockB, fromWorld, toWorld, null, 0.0, 0.0, 1);
   }

   public static CoasterPathEdge curve(BlockPos from, BlockPos to, BezierConnection bc) {
      return new CoasterPathEdge(from, to, null, null, bc, 0.0, 0.0, 1);
   }

   public static CoasterPathEdge curve(BlockPos from, BlockPos to, BezierConnection bc, double rollStart, double rollEnd) {
      return new CoasterPathEdge(from, to, null, null, bc, rollStart, rollEnd, 1);
   }
}
