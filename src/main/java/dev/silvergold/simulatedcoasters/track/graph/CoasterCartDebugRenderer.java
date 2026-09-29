/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  net.createmod.catnip.data.Pair
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.graph;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartRailSnapEligibility;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartDebugRenderer {
    private static final String OUTLINER_BASE = "coaster_cart_dbg";
    private static final int LINE_COLOR = 16605168;
    private static final int FORWARD_COLOR = 15158332;
    private static final int RIGHT_COLOR = 3066993;
    private static final int UP_COLOR = 3447003;
    private static final double AXIS_LENGTH = 1.6;

    private CoasterCartDebugRenderer() {
    }

    public static void clear() {
        Outliner outliner = Outliner.getInstance();
        for (int i = 0; i <= 3; ++i) {
            outliner.remove((Object)Pair.of((Object)OUTLINER_BASE, (Object)i));
        }
    }

    public static void render(Level level, CoasterPathGraph graph) {
        Minecraft mc = Minecraft.getInstance();
        Entity cameraEntity = mc.cameraEntity;
        if (cameraEntity == null || graph.edges().isEmpty()) {
            CoasterCartDebugRenderer.clear();
            return;
        }
        SubLevel cart = CoasterCartDebugRenderer.resolveCrosshairCart(mc, level);
        if (cart == null || !CoasterCartDebugRenderer.renderCrosshairCartBearings(level, graph, cart)) {
            CoasterCartDebugRenderer.clear();
        }
    }

    private static boolean renderCrosshairCartBearings(Level level, CoasterPathGraph graph, SubLevel cart) {
        Block bearingBlock = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
        LevelPlot plot = cart.getPlot();
        boolean[] drew = new boolean[]{false};
        CoasterCartPlotScan.forEachBearingWorldCell(plot, bearingBlock, bearingPlot -> {
            Vec3 bearingWorld = cart.logicalPose().transformPosition(Vec3.atCenterOf((Vec3i)bearingPlot));
            CoasterPathTrackFrame.GraphHit hit = CoasterPathTrackFrame.nearestGraphHit(level, graph, bearingWorld, 6.25);
            if ((hit = CoasterCartRailSnapEligibility.filterHitForBearing(cart, level, hit)) == null) {
                return;
            }
            drew[0] = true;
            Outliner.getInstance().showLine((Object)Pair.of((Object)OUTLINER_BASE, (Object)0), bearingWorld, hit.point()).colored(16605168).lineWidth(0.0625f);
            CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit);
            CoasterCartDebugRenderer.drawAxis(bearingWorld, basis.forward(), 15158332, 1);
            CoasterCartDebugRenderer.drawAxis(bearingWorld, basis.right(), 3066993, 2);
            CoasterCartDebugRenderer.drawAxis(bearingWorld, basis.up(), 3447003, 3);
        });
        return drew[0];
    }

    @Nullable
    private static SubLevel resolveCrosshairCart(Minecraft mc, Level level) {
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult) {
            BlockHitResult bhr = (BlockHitResult)hit;
            if (hit.getType() == HitResult.Type.BLOCK) {
                SubLevel cart = CoasterCartChainLinkInteraction.resolveHoveredCart(level, hit);
                if (cart != null) {
                    return cart;
                }
                SubLevel sub = CoasterCartResolve.findSubLevelAt(level, bhr.getBlockPos());
                if (sub != null && !sub.isRemoved() && CoasterCartResolve.isCartSubLevel(sub)) {
                    return sub;
                }
            }
        }
        return null;
    }

    private static void drawAxis(Vec3 origin, Vec3 dirUnit, int color, int subKey) {
        if (dirUnit.lengthSqr() < 1.0E-12) {
            return;
        }
        Vec3 end = origin.add(dirUnit.normalize().scale(1.6));
        Outliner.getInstance().showLine((Object)Pair.of((Object)OUTLINER_BASE, (Object)subKey), origin, end).colored(color).lineWidth(0.05f);
    }
}

