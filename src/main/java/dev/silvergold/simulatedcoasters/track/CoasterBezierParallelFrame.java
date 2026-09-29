/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.catnip.math.VecHelper
 *  net.minecraft.util.Mth
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterBezierParallelFrame {
    private static final int SAMPLES = 64;
    private static final double EPS_SQ = 1.0E-12;
    private static final Map<BezierConnection, Frame> CACHE = Collections.synchronizedMap(new WeakHashMap());

    private CoasterBezierParallelFrame() {
    }

    @Nullable
    public static Vec3 faceAt(BezierConnection bc, float t, float alpha) {
        if (bc == null || !CoasterBezierRailFrames.isCoaster(bc)) {
            return null;
        }
        Frame frame = CoasterBezierParallelFrame.frameFor(bc);
        return frame == null ? null : frame.faceAt(t, alpha);
    }

    @Nullable
    private static Frame frameFor(BezierConnection bc) {
        long signature = CoasterBezierParallelFrame.signatureOf(bc);
        Frame cached = CACHE.get(bc);
        if (cached != null && cached.signature == signature) {
            return cached;
        }
        Frame built = CoasterBezierParallelFrame.build(bc, signature);
        if (built != null) {
            CACHE.put(bc, built);
        }
        return built;
    }

    private static long signatureOf(BezierConnection bc) {
        long h = -3750763034362895579L;
        h = CoasterBezierParallelFrame.mix(h, (Vec3)bc.starts.getFirst());
        h = CoasterBezierParallelFrame.mix(h, (Vec3)bc.starts.getSecond());
        h = CoasterBezierParallelFrame.mix(h, (Vec3)bc.axes.getFirst());
        h = CoasterBezierParallelFrame.mix(h, (Vec3)bc.axes.getSecond());
        h = CoasterBezierParallelFrame.mix(h, (Vec3)bc.normals.getFirst());
        h = CoasterBezierParallelFrame.mix(h, (Vec3)bc.normals.getSecond());
        return (h ^= CoasterPeerCurveHandleLengths.revision()) * 1099511628211L;
    }

    private static long mix(long hash, Vec3 v) {
        hash ^= Double.doubleToLongBits(v.x);
        hash *= 1099511628211L;
        hash ^= Double.doubleToLongBits(v.y);
        hash *= 1099511628211L;
        hash ^= Double.doubleToLongBits(v.z);
        return hash *= 1099511628211L;
    }

    @Nullable
    private static Frame build(BezierConnection bc, long signature) {
        Vec3 end1 = (Vec3)bc.starts.getFirst();
        Vec3 end2 = (Vec3)bc.starts.getSecond();
        Vec3 finish1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
        Vec3 finish2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
        Vec3[] positions = new Vec3[65];
        Vec3[] tangents = new Vec3[65];
        Vec3 lastGoodTangent = null;
        for (int i = 0; i <= 64; ++i) {
            float t = (float)i / 64.0f;
            positions[i] = VecHelper.bezier((Vec3)end1, (Vec3)end2, (Vec3)finish1, (Vec3)finish2, (float)t);
            Vec3 d = VecHelper.bezierDerivative((Vec3)end1, (Vec3)end2, (Vec3)finish1, (Vec3)finish2, (float)t);
            if (d.lengthSqr() < 1.0E-12) {
                Vec3 vec3 = d = lastGoodTangent != null ? lastGoodTangent : end2.subtract(end1);
                if (d.lengthSqr() < 1.0E-12) {
                    d = new Vec3(0.0, 0.0, 1.0);
                }
            }
            tangents[i] = d.normalize();
            lastGoodTangent = tangents[i];
        }
        Vec3 startRef = CoasterBezierParallelFrame.orthogonalize((Vec3)bc.normals.getFirst(), tangents[0]);
        if (startRef == null) {
            startRef = CoasterBezierParallelFrame.anyPerpendicular(tangents[0]);
        }
        Vec3[] refs = new Vec3[65];
        refs[0] = startRef;
        for (int i = 0; i < 64; ++i) {
            refs[i + 1] = CoasterBezierParallelFrame.transport(positions[i], positions[i + 1], tangents[i], tangents[i + 1], refs[i]);
        }
        double roll = 0.0;
        Vec3 target = CoasterBezierParallelFrame.orthogonalize((Vec3)bc.normals.getSecond(), tangents[64]);
        if (target != null) {
            Vec3 transported = refs[64];
            double cos = Mth.clamp((double)transported.dot(target), (double)-1.0, (double)1.0);
            double sin = transported.cross(target).dot(tangents[64]);
            roll = Math.atan2(sin, cos);
        }
        return new Frame(signature, tangents, refs, roll);
    }

    private static Vec3 transport(Vec3 p0, Vec3 p1, Vec3 t0, Vec3 t1, Vec3 r0) {
        Vec3 orthogonal;
        Vec3 v2;
        double c2;
        Vec3 r = r0;
        Vec3 tL = t0;
        Vec3 v1 = p1.subtract(p0);
        double c1 = v1.lengthSqr();
        if (c1 > 1.0E-12) {
            r = r.subtract(v1.scale(2.0 / c1 * v1.dot(r)));
            tL = t0.subtract(v1.scale(2.0 / c1 * v1.dot(t0)));
        }
        if ((c2 = (v2 = t1.subtract(tL)).lengthSqr()) > 1.0E-12) {
            r = r.subtract(v2.scale(2.0 / c2 * v2.dot(r)));
        }
        return (orthogonal = CoasterBezierParallelFrame.orthogonalize(r, t1)) != null ? orthogonal : CoasterBezierParallelFrame.anyPerpendicular(t1);
    }

    @Nullable
    private static Vec3 orthogonalize(Vec3 v, Vec3 axisUnit) {
        Vec3 perp = v.subtract(axisUnit.scale(axisUnit.dot(v)));
        return perp.lengthSqr() < 1.0E-10 ? null : perp.normalize();
    }

    private static Vec3 anyPerpendicular(Vec3 axisUnit) {
        Vec3 candidate = Math.abs(axisUnit.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 perp = CoasterBezierParallelFrame.orthogonalize(candidate, axisUnit);
        return perp != null ? perp : new Vec3(1.0, 0.0, 0.0);
    }

    private static Vec3 rotateAbout(Vec3 v, Vec3 axisUnit, double radians) {
        if (Math.abs(radians) < 1.0E-9) {
            return v;
        }
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return v.scale(cos).add(axisUnit.cross(v).scale(sin)).add(axisUnit.scale(axisUnit.dot(v) * (1.0 - cos)));
    }

    private record Frame(long signature, Vec3[] tangents, Vec3[] refs, double totalRoll) {
        Vec3 faceAt(float t, float alpha) {
            double x = Mth.clamp((float)t, (float)0.0f, (float)1.0f) * 64.0f;
            int i = (int)Math.floor(x);
            if (i >= 64) {
                i = 63;
            }
            double frac = x - (double)i;
            Vec3 tangent = Frame.blend(this.tangents[i], this.tangents[i + 1], frac);
            Vec3 reference = Frame.blend(this.refs[i], this.refs[i + 1], frac);
            Vec3 orthogonal = CoasterBezierParallelFrame.orthogonalize(reference, tangent);
            if (orthogonal == null) {
                orthogonal = CoasterBezierParallelFrame.anyPerpendicular(tangent);
            }
            return CoasterBezierParallelFrame.rotateAbout(orthogonal, tangent, (double)Mth.clamp((float)alpha, (float)0.0f, (float)1.0f) * this.totalRoll);
        }

        private static Vec3 blend(Vec3 a, Vec3 b, double frac) {
            Vec3 v = a.scale(1.0 - frac).add(b.scale(frac));
            return v.lengthSqr() < 1.0E-12 ? a : v.normalize();
        }
    }
}

