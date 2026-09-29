/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientFrameCache;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartStaffStretchRenderGate;
import dev.silvergold.simulatedcoasters.compat.PhysicsStaffClientDragProbe;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartRailSnapEligibility;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackSnap;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartClientEngagement {
    private static final Map<UUID, Boolean> CLIENT_DISENGAGED = new HashMap<UUID, Boolean>();
    private static final Map<UUID, Long> ENDPOINT_COOLDOWN_UNTIL = new HashMap<UUID, Long>();

    private CoasterCartClientEngagement() {
    }

    static void clearSubState(UUID id) {
        CLIENT_DISENGAGED.remove(id);
        ENDPOINT_COOLDOWN_UNTIL.remove(id);
        CoasterCartStaffStretchRenderGate.clearSub(id);
        CoasterCartClientFrameCache.clearSubLevelTrackAffinity(id);
    }

    static void forceDisengaged(UUID subId) {
        CLIENT_DISENGAGED.put(subId, Boolean.TRUE);
    }

    static boolean isClientEngaged(UUID subId) {
        return CLIENT_DISENGAGED.getOrDefault(subId, true) == false;
    }

    static double engagementSearchDistSq(UUID subId, boolean staffGrabbing) {
        boolean wasDisengaged = CLIENT_DISENGAGED.getOrDefault(subId, true);
        return CoasterCartTrackSnap.engagedTrackSearchDistSq(wasDisengaged, staffGrabbing);
    }

    static boolean applyEngagementState(ClientLevel level, CoasterPathGraph graph, SubLevel sub, UUID subId, @Nullable CoasterPathTrackFrame.GraphHit hit, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs) {
        long cooldownUntil;
        long gameTime = level.getGameTime();
        boolean cooldownActive = gameTime < (cooldownUntil = ENDPOINT_COOLDOWN_UNTIL.getOrDefault(subId, 0L).longValue());
        boolean wasDisengaged = CLIENT_DISENGAGED.getOrDefault(subId, true);
        if (hit != null && CoasterCartTrackSnap.shouldDisengageAtOpenEndHit((Level)level, graph, hit, openEndRefs)) {
            ENDPOINT_COOLDOWN_UNTIL.put(subId, gameTime + 5L);
            CLIENT_DISENGAGED.put(subId, Boolean.TRUE);
            return false;
        }
        if (cooldownActive && hit != null) {
            CLIENT_DISENGAGED.put(subId, Boolean.TRUE);
            return false;
        }
        if (hit == null) {
            CLIENT_DISENGAGED.put(subId, Boolean.TRUE);
            return false;
        }
        if (wasDisengaged && !CoasterCartTrackSnap.normalsAllowReengage(sub, (Level)level, hit)) {
            CLIENT_DISENGAGED.put(subId, Boolean.TRUE);
            return false;
        }
        CLIENT_DISENGAGED.put(subId, Boolean.FALSE);
        return true;
    }

    @Deprecated
    public static boolean updateAndIsEngaged(ClientLevel level, SubLevel sub, Vec3 bearingWorld, CoasterPathGraph pathGraph, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs) {
        UUID id = sub.getUniqueId();
        if (sub.isRemoved()) {
            CoasterCartClientEngagement.clearSubState(id);
            return false;
        }
        boolean wasDisengaged = CLIENT_DISENGAGED.getOrDefault(id, true);
        boolean staffGrabbing = PhysicsStaffClientDragProbe.isAnyPhysicsStaffGrabbing((Level)level, sub);
        double searchSq = CoasterCartTrackSnap.engagedTrackSearchDistSq(wasDisengaged, staffGrabbing);
        CoasterPathTrackFrame.GraphHit hit = pathGraph.edges().isEmpty() ? null : CoasterPathTrackFrame.nearestGraphHit((Level)level, pathGraph, bearingWorld, searchSq);
        hit = CoasterCartRailSnapEligibility.filterHitForBearing(sub, (Level)level, hit);
        return CoasterCartClientEngagement.applyEngagementState(level, pathGraph, sub, id, hit, openEndRefs);
    }

    @Deprecated
    @Nullable
    public static CoasterPathTrackFrame.GraphHit nearestHitForVisual(Level level, CoasterPathGraph pathGraph, Vec3 bearingWorld, float partialTick) {
        if (pathGraph.edges().isEmpty()) {
            return null;
        }
        return CoasterPathTrackFrame.nearestGraphHit(level, pathGraph, bearingWorld, 6.25, Double.valueOf(partialTick));
    }

    public static boolean isSnapEngagedBlock(Level level, BlockPos bearingCell) {
        CoasterCartBlock bearingBlock;
        BlockState st = level.getBlockState(bearingCell);
        return st.is((Block)(bearingBlock = (CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()))) && (Boolean)st.getValue((Property)CoasterCartBlock.SNAP_ENGAGED) != false;
    }

    @Deprecated
    @Nullable
    public static CoasterPathTrackFrame.GraphHit nearestHitWhenSnapEngaged(SubLevel bearingSub, Level level, CoasterPathGraph pathGraph, Vec3 bearingLogicalWorld, Vec3 bearingVisualWorld, float partialTick, boolean snapEngaged) {
        if (!snapEngaged || pathGraph.edges().isEmpty()) {
            return null;
        }
        CoasterPathTrackFrame.GraphHit hit = CoasterCartClientEngagement.nearestHitForVisual(level, pathGraph, bearingVisualWorld, partialTick);
        if (hit == null) {
            hit = CoasterPathTrackFrame.nearestGraphHit(level, pathGraph, bearingLogicalWorld, 6.25);
        }
        return CoasterCartRailSnapEligibility.filterHitForBearing(bearingSub, level, hit);
    }
}

