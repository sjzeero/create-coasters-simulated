package dev.silvergold.simulatedcoasters.track;

public final class CoasterCurveTieredDye {
   private CoasterCurveTieredDye() {
   }

   public static CoasterCurveTieredDye.DyeStep nextDyeStep(int railRgb, int beamRgb, int dyeRgb) {
      int d = dyeRgb & 16777215;
      int rail = railRgb & 16777215;
      int beam = beamRgb & 16777215;
      if (rail == 0) {
         return new CoasterCurveTieredDye.DyeStep(d, beam, true);
      } else if (rail != d) {
         return new CoasterCurveTieredDye.DyeStep(d, beam, true);
      } else {
         return beam == d ? new CoasterCurveTieredDye.DyeStep(rail, beam, false) : new CoasterCurveTieredDye.DyeStep(rail, d, true);
      }
   }

   public static CoasterCurveTieredDye.AxeStep nextAxeStep(int railRgb, int beamRgb) {
      int rail = railRgb & 16777215;
      int beam = beamRgb & 16777215;
      if (beam != 0) {
         return new CoasterCurveTieredDye.AxeStep(rail, 0, beam);
      } else {
         return rail != 0 ? new CoasterCurveTieredDye.AxeStep(0, 0, rail) : new CoasterCurveTieredDye.AxeStep(0, 0, 0);
      }
   }

   public static record AxeStep(int newRailRgb, int newBeamRgb, int clearedRgbForParticles) {
      public boolean isNoOp() {
         return this.clearedRgbForParticles == 0;
      }
   }

   public static record DyeStep(int newRailRgb, int newBeamRgb, boolean consumeDye) {
   }
}
