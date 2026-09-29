/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.util.Mth
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;

public final class CoasterCartLinkDistanceEdit {
    private static final double SNAP_EPSILON = 1.0E-6;

    private CoasterCartLinkDistanceEdit() {
    }

    public static boolean isOnSnapGrid(double centerDistanceBlocks) {
        double step = 0.5;
        double snapped = (double)Math.round(centerDistanceBlocks / step) * step;
        return Math.abs(centerDistanceBlocks - snapped) < 1.0E-6;
    }

    public static double snapToGrid(double centerDistanceBlocks, boolean floor) {
        double step = 0.5;
        double scaled = centerDistanceBlocks / step;
        double snapped = (floor ? Math.floor(scaled) : Math.ceil(scaled)) * step;
        return Mth.clamp((double)snapped, (double)1.5, (double)6.0);
    }

    public static double steppedDistance(double current, boolean shorten) {
        double step = 0.5;
        double min = 1.5;
        double max = 6.0;
        if (shorten) {
            if (current <= min + 1.0E-6) {
                return current;
            }
            return Math.max(min, current - step);
        }
        if (current >= max - 1.0E-6) {
            return current;
        }
        return Math.min(max, current + step);
    }

    public static List<AdjustedLink> computeAdjustments(ServerSubLevel hostCart, boolean shorten) {
        List<CoasterCartTrainLinkData.LinkRecord> links = CoasterCartTrainLinkData.readAll(hostCart);
        if (links.isEmpty()) {
            return List.of();
        }
        boolean allSnapped = links.stream().allMatch(r -> CoasterCartLinkDistanceEdit.isOnSnapGrid(r.centerDistance()));
        ArrayList<AdjustedLink> out = new ArrayList<AdjustedLink>(links.size());
        for (CoasterCartTrainLinkData.LinkRecord record : links) {
            double next;
            ServerSubLevel partner = CoasterCartResolve.findCartById(hostCart.getLevel(), record.partnerId());
            if (partner == null || partner.isRemoved()) continue;
            double current = record.centerDistance();
            double d = next = allSnapped ? CoasterCartLinkDistanceEdit.steppedDistance(current, shorten) : CoasterCartLinkDistanceEdit.snapToGrid(current, shorten);
            if (Math.abs(next - current) < 1.0E-6) continue;
            out.add(new AdjustedLink(partner, next));
        }
        return out;
    }

    public record AdjustedLink(ServerSubLevel partner, double newDistance) {
    }
}

