/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class CoasterCartRailSnapEligibility {
    private CoasterCartRailSnapEligibility() {
    }

    public static boolean isRailEdgeFullyOnBearingSubLevel(SubLevel bearingSub, Level level, CoasterPathEdge edge) {
        SubLevel a = Sable.HELPER.getContaining(level, (Vec3i)edge.from());
        SubLevel b = Sable.HELPER.getContaining(level, (Vec3i)edge.to());
        if (a == null || b == null) {
            return false;
        }
        UUID id = bearingSub.getUniqueId();
        return Objects.equals(id, a.getUniqueId()) && Objects.equals(id, b.getUniqueId());
    }

    public static boolean isRailEdgeFullyOnBearingSubLevel(SubLevel bearingSub, Level level, CoasterPathTrackFrame.GraphHit hit) {
        return hit != null && CoasterCartRailSnapEligibility.isRailEdgeFullyOnBearingSubLevel(bearingSub, level, hit.edge());
    }

    @Nullable
    public static CoasterPathTrackFrame.GraphHit filterHitForBearing(SubLevel bearingSub, Level level, @Nullable CoasterPathTrackFrame.GraphHit hit) {
        if (CoasterCartRailSnapEligibility.isRailEdgeFullyOnBearingSubLevel(bearingSub, level, hit)) {
            return null;
        }
        return hit;
    }
}

