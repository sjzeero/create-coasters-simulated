/*
 * Decompiled with CFR 0.152.
 */
package dev.silvergold.simulatedcoasters.track;

public final class CoasterCurveTieredDye {
    private CoasterCurveTieredDye() {
    }

    public static DyeStep nextDyeStep(int railRgb, int beamRgb, int dyeRgb) {
        int d = dyeRgb & 0xFFFFFF;
        int rail = railRgb & 0xFFFFFF;
        int beam = beamRgb & 0xFFFFFF;
        if (rail == 0) {
            return new DyeStep(d, beam, true);
        }
        if (rail != d) {
            return new DyeStep(d, beam, true);
        }
        if (beam == d) {
            return new DyeStep(rail, beam, false);
        }
        return new DyeStep(rail, d, true);
    }

    public static AxeStep nextAxeStep(int railRgb, int beamRgb) {
        int rail = railRgb & 0xFFFFFF;
        int beam = beamRgb & 0xFFFFFF;
        if (beam != 0) {
            return new AxeStep(rail, 0, beam);
        }
        if (rail != 0) {
            return new AxeStep(0, 0, rail);
        }
        return new AxeStep(0, 0, 0);
    }

    public record DyeStep(int newRailRgb, int newBeamRgb, boolean consumeDye) {
    }

    public record AxeStep(int newRailRgb, int newBeamRgb, int clearedRgbForParticles) {
        public boolean isNoOp() {
            return this.clearedRgbForParticles == 0;
        }
    }
}

