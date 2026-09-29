/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.BezierBankBlendLevel;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class BezierBankBlend {
    private static final float CHAIN_MONOTONIC_EPS = 1.0E-4f;
    private static final float GEOMETRIC_FRAME_ALIGNMENT_DOT_MIN = 0.55f;
    private static final int MAX_JOINT_BANK_CHAIN_ANCHORS = 512;

    private BezierBankBlend() {
    }

    public static float smoothJointBankParameter(float rawT, @Nullable BezierConnection bc) {
        return BezierBankBlend.smoothJointBankParameter(rawT, bc, BezierBankBlendLevel.tryGetClientLevel());
    }

    public static float smoothJointBankParameter(double rawT, @Nullable BezierConnection bc) {
        return BezierBankBlend.smoothJointBankParameter((float)rawT, bc);
    }

    public static float smoothJointBankParameter(double rawT, @Nullable BezierConnection bc, @Nullable Level level) {
        return BezierBankBlend.smoothJointBankParameter((float)rawT, bc, level);
    }

    public static float smoothJointBankParameter(float rawT, @Nullable BezierConnection bc, @Nullable Level level) {
        int edgeIdx;
        if (bc == null || !bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return rawT;
        }
        if (level == null) {
            return rawT;
        }
        bc = bc.isPrimary() ? bc : bc.secondary();
        BlockPos a0 = (BlockPos)bc.bePositions.getFirst();
        BlockPos a1 = (BlockPos)bc.bePositions.getSecond();
        float thetaP = BezierBankBlend.jointBankRadiansAt(level, a0);
        float thetaQ = BezierBankBlend.jointBankRadiansAt(level, a1);
        float dTheta = thetaQ - thetaP;
        if (Math.abs(dTheta) < 1.0E-6f) {
            return rawT;
        }
        List<BlockPos> chain = BezierBankBlend.orderedAnchorsAlongJointBankChain(level, a0, a1, bc);
        if (BezierBankBlend.segmentInLongMonotonicRun(level, chain, edgeIdx = BezierBankBlend.edgeStartIndexInChain(chain, a0, a1))) {
            return Mth.clamp((float)rawT, (float)0.0f, (float)1.0f);
        }
        if (BezierBankBlend.geometricFaceNormalsAlignedAcrossSegment(level, a0, a1, bc) < 0.55f) {
            return Mth.clamp((float)rawT, (float)0.0f, (float)1.0f);
        }
        BezierConnection prevNeighbour = BezierBankBlend.neighbourCoasterCurveAtAnchor(level, a0, bc);
        BezierConnection nextNeighbour = BezierBankBlend.neighbourCoasterCurveAtAnchor(level, a1, bc);
        float thetaPrev = prevNeighbour != null ? BezierBankBlend.jointBankRadiansAt(level, BezierBankBlend.otherEndpoint(prevNeighbour, a0)) : thetaP;
        float thetaNext = nextNeighbour != null ? BezierBankBlend.jointBankRadiansAt(level, BezierBankBlend.otherEndpoint(nextNeighbour, a1)) : thetaQ;
        float mP = 0.0f;
        if (prevNeighbour != null) {
            BlockPos aPrev = BezierBankBlend.otherEndpoint(prevNeighbour, a0);
            boolean prevOk = BezierBankBlend.geometricFaceNormalsAlignedAcrossSegment(level, aPrev, a0, prevNeighbour) >= 0.55f;
            mP = prevOk ? (thetaQ - thetaPrev) * 0.5f : 0.0f;
        }
        float mQ = 0.0f;
        if (nextNeighbour != null) {
            BlockPos aNext = BezierBankBlend.otherEndpoint(nextNeighbour, a1);
            boolean nextOk = BezierBankBlend.geometricFaceNormalsAlignedAcrossSegment(level, a1, aNext, nextNeighbour) >= 0.55f;
            mQ = nextOk ? (thetaNext - thetaP) * 0.5f : 0.0f;
        }
        float t = Mth.clamp((float)rawT, (float)0.0f, (float)1.0f);
        float t2 = t * t;
        float t3 = t2 * t;
        float h00 = 2.0f * t3 - 3.0f * t2 + 1.0f;
        float h10 = t3 - 2.0f * t2 + t;
        float h01 = -2.0f * t3 + 3.0f * t2;
        float h11 = t3 - t2;
        float theta = h00 * thetaP + h10 * mP + h01 * thetaQ + h11 * mQ;
        float alpha = (theta - thetaP) / dTheta;
        return Mth.clamp((float)alpha, (float)0.0f, (float)1.0f);
    }

    public static long mixBankBlendInputSignature(@Nullable Level level, @Nullable BezierConnection bc, long hash) {
        if (level == null || bc == null || !bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return hash;
        }
        BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
        BlockPos a0 = (BlockPos)p.bePositions.getFirst();
        BlockPos a1 = (BlockPos)p.bePositions.getSecond();
        hash = BezierBankBlend.mixFloat(hash, BezierBankBlend.jointBankRadiansAt(level, a0));
        hash = BezierBankBlend.mixFloat(hash, BezierBankBlend.jointBankRadiansAt(level, a1));
        hash = BezierBankBlend.mixChainNeighbour(level, a0, p, hash);
        return BezierBankBlend.mixChainNeighbour(level, a1, p, hash);
    }

    private static long mixChainNeighbour(Level level, BlockPos anchorPos, BezierConnection self, long hash) {
        BezierConnection neighbour = BezierBankBlend.neighbourCoasterCurveAtAnchor(level, anchorPos, self);
        if (neighbour == null) {
            return BezierBankBlend.mixLong(hash, 0L);
        }
        BlockPos far = BezierBankBlend.otherEndpoint(neighbour, anchorPos);
        return BezierBankBlend.mixFloat(BezierBankBlend.mixLong(hash, far.asLong()), BezierBankBlend.jointBankRadiansAt(level, far));
    }

    private static long mixLong(long hash, long value) {
        return (hash ^ value) * 1099511628211L;
    }

    private static long mixFloat(long hash, float value) {
        return BezierBankBlend.mixLong(hash, Float.floatToIntBits(value));
    }

    private static float jointBankRadiansAt(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
            return be.getJointBankRadians();
        }
        return 0.0f;
    }

    @Nullable
    private static BezierConnection neighbourCoasterCurveAtAnchor(Level level, BlockPos anchorPos, BezierConnection self) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
        if (incident.size() != 2) {
            return null;
        }
        BezierConnection a = incident.get(0);
        BezierConnection b = incident.get(1);
        BezierConnection other = BezierBankBlend.sameEdge(a, self) ? b : a;
        return BezierBankBlend.sameEdge(other, self) ? null : other;
    }

    private static boolean sameEdge(BezierConnection a, BezierConnection b) {
        BlockPos a0 = (BlockPos)a.bePositions.getFirst();
        BlockPos a1 = (BlockPos)a.bePositions.getSecond();
        BlockPos b0 = (BlockPos)b.bePositions.getFirst();
        BlockPos b1 = (BlockPos)b.bePositions.getSecond();
        return a0.equals((Object)b0) && a1.equals((Object)b1) || a0.equals((Object)b1) && a1.equals((Object)b0);
    }

    private static BlockPos otherEndpoint(BezierConnection bc, BlockPos thisEnd) {
        return ((BlockPos)bc.bePositions.getFirst()).equals((Object)thisEnd) ? (BlockPos)bc.bePositions.getSecond() : (BlockPos)bc.bePositions.getFirst();
    }

    private static List<BlockPos> orderedAnchorsAlongJointBankChain(Level level, BlockPos a0, BlockPos a1, BezierConnection segment) {
        BlockPos next;
        BezierConnection nb;
        BlockPos prev;
        BezierConnection nb2;
        ArrayList<BlockPos> anchors = new ArrayList<BlockPos>();
        anchors.add(a0);
        anchors.add(a1);
        HashSet<BlockPos> visited = new HashSet<BlockPos>();
        visited.add(a0);
        visited.add(a1);
        BlockPos left = a0;
        BezierConnection inc = segment;
        while (anchors.size() < 512 && (nb2 = BezierBankBlend.neighbourCoasterCurveAtAnchor(level, left, inc)) != null && visited.add(prev = BezierBankBlend.otherEndpoint(nb2, left))) {
            anchors.add(0, prev);
            left = prev;
            inc = nb2;
        }
        BlockPos right = a1;
        inc = segment;
        while (anchors.size() < 512 && (nb = BezierBankBlend.neighbourCoasterCurveAtAnchor(level, right, inc)) != null && visited.add(next = BezierBankBlend.otherEndpoint(nb, right))) {
            anchors.add(next);
            right = next;
            inc = nb;
        }
        return anchors;
    }

    private static int edgeStartIndexInChain(List<BlockPos> chain, BlockPos segStart, BlockPos segEnd) {
        for (int i = 0; i < chain.size() - 1; ++i) {
            if (!chain.get(i).equals((Object)segStart) || !chain.get(i + 1).equals((Object)segEnd)) continue;
            return i;
        }
        return -1;
    }

    private static boolean segmentInLongMonotonicRun(Level level, List<BlockPos> chain, int edgeStartIdx) {
        if (chain.size() < 3 || edgeStartIdx < 0) {
            return false;
        }
        float[] unwrapped = BezierBankBlend.unwrappedJointBankChain(level, chain);
        if (unwrapped == null) {
            return false;
        }
        for (int[] run : BezierBankBlend.maximalWeaklyMonotonicRuns(unwrapped)) {
            int rs;
            int re = run[1];
            if (re - (rs = run[0]) + 1 < 3 || rs > edgeStartIdx || edgeStartIdx + 1 > re) continue;
            return true;
        }
        return false;
    }

    @Nullable
    private static float[] unwrappedJointBankChain(Level level, List<BlockPos> orderedAnchors) {
        int n = orderedAnchors.size();
        if (n < 2) {
            return null;
        }
        float[] raw = new float[n];
        for (int i = 0; i < n; ++i) {
            raw[i] = BezierBankBlend.jointBankRadiansAt(level, orderedAnchors.get(i));
        }
        float[] unwrapped = new float[n];
        unwrapped[0] = raw[0];
        for (int i = 1; i < n; ++i) {
            double d;
            for (d = (double)(raw[i] - raw[i - 1]); d > Math.PI; d -= 6.2831854820251465) {
            }
            while (d < -Math.PI) {
                d += 6.2831854820251465;
            }
            unwrapped[i] = unwrapped[i - 1] + (float)d;
        }
        return unwrapped;
    }

    private static List<int[]> maximalWeaklyMonotonicRuns(float[] unwrapped) {
        int n = unwrapped.length;
        ArrayList<int[]> runs = new ArrayList<int[]>();
        if (n < 2) {
            return runs;
        }
        int runStart = 0;
        Boolean increasing = null;
        for (int ei = 0; ei < n - 1; ++ei) {
            boolean inc;
            float d = unwrapped[ei + 1] - unwrapped[ei];
            if (Math.abs(d) < 1.0E-4f) continue;
            boolean bl = inc = d > 0.0f;
            if (increasing == null) {
                increasing = inc;
                continue;
            }
            if (increasing == inc) continue;
            runs.add(new int[]{runStart, ei});
            runStart = ei;
            increasing = inc;
        }
        runs.add(new int[]{runStart, n - 1});
        return runs;
    }

    private static float geometricFaceNormalsAlignedAcrossSegment(Level level, BlockPos u, BlockPos v, BezierConnection seg) {
        Vec3 axU = CoasterBezierHandleEdit.tangentIntoFromAnchor(seg, u);
        Vec3 axV = CoasterBezierHandleEdit.tangentIntoFromAnchor(seg, v);
        if (axU == null || axV == null) {
            return 1.0f;
        }
        Vec3 gU = CoasterTrackPlacement.anchorGeometricFaceNormal(level, u, axU);
        Vec3 gV = CoasterTrackPlacement.anchorGeometricFaceNormal(level, v, axV);
        return (float)Math.abs(gU.dot(gV));
    }
}

