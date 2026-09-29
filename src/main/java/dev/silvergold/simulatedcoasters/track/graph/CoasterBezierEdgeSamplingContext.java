package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CoasterBezierEdgeSamplingContext {
   private final CoasterPathEdge edge;
   private final BezierConnection bc;
   private final double extSpanFrom;
   private final double extSpanTo;
   private final float extLenFrom;
   private final float extLenTo;
   private final double tLo;
   private final double tHi;

   private CoasterBezierEdgeSamplingContext(
      CoasterPathEdge edge, BezierConnection bc, double extSpanFrom, double extSpanTo, float extLenFrom, float extLenTo, double tLo, double tHi
   ) {
      this.edge = edge;
      this.bc = bc;
      this.extSpanFrom = extSpanFrom;
      this.extSpanTo = extSpanTo;
      this.extLenFrom = extLenFrom;
      this.extLenTo = extLenTo;
      this.tLo = tLo;
      this.tHi = tHi;
   }

   public static CoasterBezierEdgeSamplingContext forCoasterEdge(Level level, CoasterPathEdge edge) {
      BezierConnection bc = edge.bezier();
      if (bc != null && CoasterPathGraphManager.isCoasterGraphBezier(bc)) {
         double extSpanFrom = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, true);
         double extSpanTo = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, false);
         double lo = extSpanFrom > 0.0 ? -extSpanFrom : 0.0;
         double hi = extSpanTo > 0.0 ? 1.0 + extSpanTo : 1.0;
         return new CoasterBezierEdgeSamplingContext(
            edge,
            bc,
            extSpanFrom,
            extSpanTo,
            CoasterOpenEndExtension.extensionBlocks(level, edge.from()),
            CoasterOpenEndExtension.extensionBlocks(level, edge.to()),
            lo,
            hi
         );
      } else {
         return null;
      }
   }

   public CoasterPathEdge edge() {
      return this.edge;
   }

   public double tLo() {
      return this.tLo;
   }

   public double tHi() {
      return this.tHi;
   }

   public double[] sampleTRange() {
      return new double[]{this.tLo, this.tHi};
   }

   public Vec3 plotLocalCenterline(double t) {
      if (t < 0.0 && this.extSpanFrom > 0.0) {
         double s = Mth.clamp(-t / this.extSpanFrom, 0.0, 1.0);
         Vec3 p0 = CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(this.bc, 0.0));
         Vec3 outward = CoasterPathGraphManager.coasterBezierPlotLocalTangent(this.bc, 0.0).normalize().scale(-1.0);
         return p0.add(outward.scale(s * (double)this.extLenFrom));
      } else if (t > 1.0 && this.extSpanTo > 0.0) {
         double s = Mth.clamp((t - 1.0) / this.extSpanTo, 0.0, 1.0);
         Vec3 p1 = CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(this.bc, 1.0));
         Vec3 outward = CoasterPathGraphManager.coasterBezierPlotLocalTangent(this.bc, 1.0).normalize();
         return p1.add(outward.scale(s * (double)this.extLenTo));
      } else {
         return CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(this.bc, Mth.clamp(t, 0.0, 1.0)));
      }
   }
}
