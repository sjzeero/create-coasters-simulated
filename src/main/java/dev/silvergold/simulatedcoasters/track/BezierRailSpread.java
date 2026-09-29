/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class BezierRailSpread {
    private BezierRailSpread() {
    }

    public static Vec3 faceCrossTangentOrFallback(Vec3 faceNormal, Vec3 tangentUnit) {
        return BezierRailSpread.faceCrossTangentOrFallback(faceNormal, tangentUnit, null, null);
    }

    public static Vec3 faceCrossTangentOrFallback(Vec3 faceNormal, Vec3 tangentUnit, @Nullable Vec3 endpointFaceA, @Nullable Vec3 endpointFaceB) {
        Vec3 face = BezierRailSpread.orthogonalFaceForTangent(faceNormal, tangentUnit, endpointFaceA, endpointFaceB);
        Vec3 rail = face.cross(tangentUnit);
        if (rail.lengthSqr() >= 1.0E-10) {
            return rail;
        }
        Vec3 bank = face.subtract(tangentUnit.scale(tangentUnit.dot(face)));
        if (bank.lengthSqr() >= 1.0E-10) {
            return bank.cross(tangentUnit);
        }
        Vec3 y = new Vec3(0.0, 1.0, 0.0);
        Vec3 b2 = y.subtract(tangentUnit.scale(tangentUnit.dot(y)));
        if (b2.lengthSqr() >= 1.0E-10) {
            return b2.cross(tangentUnit);
        }
        Vec3 x = new Vec3(1.0, 0.0, 0.0);
        Vec3 b3 = x.subtract(tangentUnit.scale(tangentUnit.dot(x)));
        if (b3.lengthSqr() >= 1.0E-10) {
            return b3.cross(tangentUnit);
        }
        Vec3 f = tangentUnit.cross(y);
        if (f.lengthSqr() >= 1.0E-10) {
            return f;
        }
        f = tangentUnit.cross(x);
        if (f.lengthSqr() >= 1.0E-10) {
            return f;
        }
        return new Vec3(1.0E-4, 0.0, 0.0);
    }

    public static Vec3 orthogonalFaceForTangent(Vec3 blendedFace, Vec3 tangentUnit, @Nullable Vec3 endpointFaceA, @Nullable Vec3 endpointFaceB) {
        Vec3 t = tangentUnit.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : tangentUnit.normalize();
        Vec3 perp = blendedFace.subtract(t.scale(t.dot(blendedFace)));
        if (perp.lengthSqr() >= 1.0E-10) {
            return perp.normalize();
        }
        if (endpointFaceA != null && (perp = endpointFaceA.subtract(t.scale(t.dot(endpointFaceA)))).lengthSqr() >= 1.0E-10) {
            return perp.normalize();
        }
        if (endpointFaceB != null && (perp = endpointFaceB.subtract(t.scale(t.dot(endpointFaceB)))).lengthSqr() >= 1.0E-10) {
            return perp.normalize();
        }
        return blendedFace;
    }

    public static Vec3 railHalfGaugeOffset(Vec3 faceNormal, Vec3 tangentUnit) {
        return BezierRailSpread.faceCrossTangentOrFallback(faceNormal, tangentUnit).normalize().scale(0.9375);
    }
}

