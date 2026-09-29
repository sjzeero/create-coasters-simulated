/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.catnip.data.Couple
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterPeerCurveHandleLengths {
    private static final Map<String, Couple<Double>> BY_EDGE = Collections.synchronizedMap(new HashMap());
    private static final Map<BezierConnection, Couple<Double>> INSTANCE_LENGTHS = Collections.synchronizedMap(new WeakHashMap());
    private static final Map<BezierConnection, Couple<Double>> PREVIEW_BY_CONNECTION = Collections.synchronizedMap(new WeakHashMap());
    private static final AtomicLong REVISION = new AtomicLong();

    private CoasterPeerCurveHandleLengths() {
    }

    public static long revision() {
        return REVISION.get();
    }

    public static boolean isCoaster(BezierConnection bc) {
        return bc != null && bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id);
    }

    public static String edgeKey(BezierConnection bc) {
        return CoasterPeerCurveHandleLengths.edgeKeyFromEndpoints((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond());
    }

    public static String edgeKeyFromEndpoints(BlockPos a, BlockPos b) {
        long lb;
        long la = a.asLong();
        return la <= (lb = b.asLong()) ? la + ":" + lb : lb + ":" + la;
    }

    public static BlockPos lexMin(BlockPos a, BlockPos b) {
        return a.asLong() <= b.asLong() ? a : b;
    }

    public static BlockPos lexMax(BlockPos a, BlockPos b) {
        return a.asLong() <= b.asLong() ? b : a;
    }

    @Nullable
    public static Couple<Double> getCanonicalLengths(BezierConnection bc) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(bc)) {
            return null;
        }
        Couple<Double> preview = PREVIEW_BY_CONNECTION.get(bc);
        if (preview != null) {
            return preview;
        }
        Couple<Double> inst = INSTANCE_LENGTHS.get(bc);
        if (inst != null) {
            return inst;
        }
        return BY_EDGE.get(CoasterPeerCurveHandleLengths.edgeKey(bc));
    }

    public static double getLengthAtEndpoint(BezierConnection bc, int endpointIndex) {
        Couple<Double> canon = CoasterPeerCurveHandleLengths.getCanonicalLengths(bc);
        double fallback = CoasterPeerCurveHandleLengths.fallbackSingleHandleLength(bc);
        if (canon == null) {
            return fallback;
        }
        BlockPos lo = CoasterPeerCurveHandleLengths.lexMin((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond());
        boolean firstIsLo = ((BlockPos)bc.bePositions.getFirst()).equals((Object)lo);
        double h0 = firstIsLo ? (Double)canon.getFirst() : (Double)canon.getSecond();
        double h1 = firstIsLo ? (Double)canon.getSecond() : (Double)canon.getFirst();
        return endpointIndex == 0 ? h0 : h1;
    }

    private static double fallbackSingleHandleLength(BezierConnection bc) {
        return bc.getHandleLength();
    }

    public static Vec3 controlPointWorld(BezierConnection bc, int endpointIndex) {
        Vec3 start = endpointIndex == 0 ? (Vec3)bc.starts.getFirst() : (Vec3)bc.starts.getSecond();
        Vec3 axis = endpointIndex == 0 ? (Vec3)bc.axes.getFirst() : (Vec3)bc.axes.getSecond();
        double h = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(bc, endpointIndex);
        return start.add(axis.scale(h));
    }

    public static double maxHandleLengthForEdge(Level level, BezierConnection bc) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(bc)) {
            return Double.MAX_VALUE;
        }
        BlockPos p0 = (BlockPos)bc.bePositions.getFirst();
        BlockPos p1 = (BlockPos)bc.bePositions.getSecond();
        Vec3 c0 = CoasterAnchorpointBlockEntity.worldCenter(level, p0);
        Vec3 c1 = CoasterAnchorpointBlockEntity.worldCenter(level, p1);
        double chord = c0.distanceTo(c1);
        return Math.max(1.0E-4, chord * 0.7);
    }

    public static void putCanonical(BezierConnection bc, double hAtLo, double hAtHi) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(bc)) {
            return;
        }
        Couple lengths = Couple.create((Object)CoasterPeerCurveHandleLengths.sanitizeLength(hAtLo), (Object)CoasterPeerCurveHandleLengths.sanitizeLength(hAtHi));
        BY_EDGE.put(CoasterPeerCurveHandleLengths.edgeKey(bc), (Couple<Double>)lengths);
        CoasterPeerCurveHandleLengths.bindInstanceLengths(bc, (Couple<Double>)lengths);
    }

    private static void bindInstanceLengths(BezierConnection bc, Couple<Double> lengths) {
        BezierConnection other;
        REVISION.incrementAndGet();
        INSTANCE_LENGTHS.put(bc, lengths);
        BezierConnection bezierConnection = other = bc.isPrimary() ? bc.secondary() : bc;
        if (other != bc) {
            INSTANCE_LENGTHS.put(other, lengths);
        }
    }

    public static void putPreviewCanonical(BezierConnection bc, double hAtLo, double hAtHi) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(bc)) {
            return;
        }
        REVISION.incrementAndGet();
        PREVIEW_BY_CONNECTION.put(bc, (Couple<Double>)Couple.create((Object)CoasterPeerCurveHandleLengths.sanitizeLength(hAtLo), (Object)CoasterPeerCurveHandleLengths.sanitizeLength(hAtHi)));
    }

    public static void commitPreviewLengths(BezierConnection bc) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(bc)) {
            return;
        }
        REVISION.incrementAndGet();
        Couple<Double> preview = PREVIEW_BY_CONNECTION.remove(bc);
        if (preview != null) {
            BY_EDGE.put(CoasterPeerCurveHandleLengths.edgeKey(bc), preview);
        }
    }

    public static void removeEdge(BezierConnection bc) {
        if (bc != null) {
            REVISION.incrementAndGet();
            BY_EDGE.remove(CoasterPeerCurveHandleLengths.edgeKey(bc));
        }
    }

    @Nullable
    public static Couple<Double> takeCanonicalLengthsForRemap(BezierConnection primary) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(primary) || !primary.isPrimary()) {
            return null;
        }
        REVISION.incrementAndGet();
        return BY_EDGE.remove(CoasterPeerCurveHandleLengths.edgeKey(primary));
    }

    private static double sanitizeLength(double h) {
        if (Double.isNaN(h) || h < 1.0E-4) {
            return 1.0E-4;
        }
        return h;
    }

    public static void putSymmetricFromRuntime(BezierConnection primary) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(primary) || !primary.isPrimary()) {
            return;
        }
        if (CoasterPeerCurveHandleLengths.getCanonicalLengths(primary) != null) {
            return;
        }
        String key = CoasterPeerCurveHandleLengths.edgeKey(primary);
        BY_EDGE.remove(key);
        double h = primary.getHandleLength();
        CoasterPeerCurveHandleLengths.putCanonical(primary, h, h);
    }

    public static void copyEdgeLengths(BezierConnection fromGraph, BezierConnection toGraph) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(toGraph)) {
            return;
        }
        Couple<Double> c = CoasterPeerCurveHandleLengths.getCanonicalLengths(fromGraph);
        if (c != null) {
            BY_EDGE.put(CoasterPeerCurveHandleLengths.edgeKey(toGraph), c);
            CoasterPeerCurveHandleLengths.bindInstanceLengths(toGraph, c);
        }
    }

    public static void carryLengthsToReplacement(BezierConnection from, BezierConnection to) {
        if (!(CoasterPeerCurveHandleLengths.isCoaster(from) && CoasterPeerCurveHandleLengths.isCoaster(to) && CoasterPeerCurveHandleLengths.edgeKey(from).equals(CoasterPeerCurveHandleLengths.edgeKey(to)))) {
            return;
        }
        Couple<Double> preview = PREVIEW_BY_CONNECTION.get(from);
        if (preview != null) {
            CoasterPeerCurveHandleLengths.putPreviewCanonical(to, (Double)preview.getFirst(), (Double)preview.getSecond());
            return;
        }
        CoasterPeerCurveHandleLengths.copyEdgeLengths(from, to);
    }
}

