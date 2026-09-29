/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$DoubleValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 */
package dev.silvergold.simulatedcoasters;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue UNIVERSAL_DRAG_COMPENSATION_FRACTION = BUILDER.comment(new String[]{"Fraction of dimension universal drag to compensate for engaged coaster carts and rivets hosted on cart sub-levels (0\u20131).", "Higher resists damping more; values near 1 can overshoot when contacts share the same sub-step."}).defineInRange("universalDragCompensationFraction", 1.0, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue MAX_CHAIN_LIFT_SPAN_BLOCKS = BUILDER.comment("Maximum chain lift span length along the track centerline (blocks).").defineInRange("maxChainLiftSpanBlocks", 64.0, 1.0, 128.0);
    public static final ModConfigSpec.IntValue MAX_TRACK_CONNECTION_SPAN_BLOCKS = BUILDER.comment("Maximum straight-line distance between coaster anchorpoints when placing track (blocks).").defineInRange("maxTrackConnectionSpanBlocks", 45, 1, 128);
    public static final ModConfigSpec.DoubleValue MAX_BALLOON_PAYLOAD_MASS = BUILDER.comment(new String[]{"Maximum hosted sub-level mass (Sable mass units) a balloon can help support at the tether anchor.", "Applies weight compensation only \u2014 does not change empty-balloon hover rise rate."}).defineInRange("maxBalloonPayloadMass", 48.0, 0.0, 512.0);
    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static double universalDragCompensationFraction() {
        return (Double)UNIVERSAL_DRAG_COMPENSATION_FRACTION.get();
    }

    public static double maxChainLiftSpanBlocks() {
        return (Double)MAX_CHAIN_LIFT_SPAN_BLOCKS.get();
    }

    public static int maxTrackConnectionSpanBlocks() {
        return (Integer)MAX_TRACK_CONNECTION_SPAN_BLOCKS.get();
    }

    public static double maxBalloonPayloadMass() {
        return (Double)MAX_BALLOON_PAYLOAD_MASS.get();
    }
}

