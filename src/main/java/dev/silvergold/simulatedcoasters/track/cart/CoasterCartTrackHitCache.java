/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

final class CoasterCartTrackHitCache {
    private static final double FULL_SEARCH_MOVEMENT_BLOCKS = 0.35;
    private static final double FULL_SEARCH_MOVEMENT_DIST_SQ = 0.12249999999999998;
    private static final double ENDPOINT_T_MARGIN = 0.06;
    private static final double ENGAGED_T_REBUILD_EPS = 1.0E-5;
    private static final Map<ServerSubLevel, CartHitEntry> CART_HITS = Collections.synchronizedMap(new IdentityHashMap());
    private static final Map<ServerSubLevel, EngagedTickEntry> ENGAGED_TICK = Collections.synchronizedMap(new IdentityHashMap());
    private static final Map<ResourceKey<Level>, OpenEndTickEntry> OPEN_ENDS_BY_DIM = new HashMap<ResourceKey<Level>, OpenEndTickEntry>();

    private CoasterCartTrackHitCache() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs(ServerLevel level, CoasterPathGraph graph) {
        if (graph.edges().isEmpty()) {
            return List.of();
        }
        long gameTime = level.getGameTime();
        int revision = graph.revision();
        ResourceKey dim = level.dimension();
        Map<ResourceKey<Level>, OpenEndTickEntry> map = OPEN_ENDS_BY_DIM;
        synchronized (map) {
            OpenEndTickEntry cached = OPEN_ENDS_BY_DIM.get(dim);
            if (cached != null && cached.gameTime == gameTime && cached.graphRevision == revision) {
                return cached.refs;
            }
            List<CoasterPathTraceEndpoints.OpenEndRef> refs = CoasterPathTraceEndpoints.pathOpenEndRefs((Level)level, graph, CoasterPathTraceEndpoints.allEdgeIndices(graph));
            OPEN_ENDS_BY_DIM.put((ResourceKey<Level>)dim, new OpenEndTickEntry(gameTime, revision, refs));
            return refs;
        }
    }

    static EngagedTickFrame engagedTickFrame(ServerLevel level, CoasterPathGraph graph, ServerSubLevel cart, CoasterPathTrackFrame.GraphHit graphHit, boolean negateEdgeTangent, boolean subLevelTrack) {
        CoasterPathTrackFrame.TrackBasis basis;
        long gameTime = level.getGameTime();
        int revision = graph.revision();
        EngagedTickEntry cached = ENGAGED_TICK.get(cart);
        if (cached != null && cached.gameTime == gameTime && cached.graphRevision == revision && cached.negateEdgeTangent == negateEdgeTangent) {
            if (!subLevelTrack) {
                return cached.frame;
            }
            if (CoasterCartTrackHitCache.sameEngagedHit(cached.frame.graphHit(), graphHit)) {
                return cached.frame;
            }
        }
        Vec3 trackUp = (basis = CoasterCartTrackHitCache.alignedTrackBasis(CoasterPathTrackFrame.trackBasisAtHit((Level)level, graphHit), negateEdgeTangent)).up().lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : basis.up().normalize();
        Vec3 snapTarget = graphHit.point().add(trackUp.scale(0.3125));
        EngagedTickFrame frame = new EngagedTickFrame(graphHit, basis, new Vector3d(snapTarget.x, snapTarget.y, snapTarget.z));
        ENGAGED_TICK.put(cart, new EngagedTickEntry(gameTime, revision, negateEdgeTangent, frame));
        return frame;
    }

    @Nullable
    static CoasterPathTrackFrame.GraphHit nearestGraphHit(ServerLevel level, CoasterPathGraph graph, ServerSubLevel cart, Vec3 bearingWorld, double maxDistSq) {
        return CoasterCartTrackHitCache.nearestGraphHit(level, graph, cart, bearingWorld, maxDistSq, new boolean[1]);
    }

    @Nullable
    static CoasterPathTrackFrame.GraphHit nearestGraphHit(ServerLevel level, CoasterPathGraph graph, ServerSubLevel cart, Vec3 bearingWorld, double maxDistSq, boolean[] outSubLevelTrack) {
        CoasterPathTrackFrame.GraphHit hit;
        if (graph.edges().isEmpty()) {
            return null;
        }
        long gameTime = level.getGameTime();
        int revision = graph.revision();
        EngagedTickEntry engaged = ENGAGED_TICK.get(cart);
        if (engaged != null && engaged.gameTime == gameTime && engaged.graphRevision == revision) {
            boolean isSub;
            CartHitEntry cartEntry = CART_HITS.get(cart);
            boolean bl = isSub = cartEntry != null && cartEntry.graphRevision() == revision && cartEntry.subLevelTrack();
            if (!isSub) {
                outSubLevelTrack[0] = false;
                return engaged.frame.graphHit();
            }
            @Nullable CoasterPathTrackFrame.GraphHit refined = CoasterCartTrackHitCache.refineEngagedHit((Level)level, graph, engaged.frame.graphHit(), bearingWorld, maxDistSq, cart);
            outSubLevelTrack[0] = true;
            if (refined != null) {
                CoasterCartTrackHitCache.putCartHit(cart, refined, graph, gameTime, bearingWorld, true);
                return refined;
            }
            return engaged.frame.graphHit();
        }
        CartHitEntry cached = CART_HITS.get(cart);
        if (cached != null && cached.graphRevision == revision) {
            CoasterPathTrackFrame.GraphHit refined;
            if (cached.gameTime == gameTime) {
                refined = CoasterCartTrackHitCache.refineOnCachedEdge((Level)level, graph, cached, bearingWorld, maxDistSq);
                if (refined != null) {
                    outSubLevelTrack[0] = cached.subLevelTrack();
                    return refined;
                }
            } else if (!cached.subLevelTrack() && bearingWorld.distanceToSqr(cached.bearingX(), cached.bearingY(), cached.bearingZ()) <= 0.12249999999999998 && (refined = CoasterCartTrackHitCache.refineOnCachedEdge((Level)level, graph, cached, bearingWorld, maxDistSq)) != null && !CoasterCartTrackHitCache.nearEdgeEndpoint(refined.t())) {
                CoasterCartTrackHitCache.putCartHit(cart, refined, graph, gameTime, bearingWorld, false);
                outSubLevelTrack[0] = false;
                return refined;
            }
        }
        if ((hit = CoasterPathTrackFrame.nearestGraphHit((Level)level, graph, bearingWorld, maxDistSq)) != null) {
            boolean isSub = CoasterPathGraphManager.edgeHostedOnSubLevel((Level)level, hit.edge());
            CoasterCartTrackHitCache.putCartHit(cart, hit, graph, gameTime, bearingWorld, isSub);
            outSubLevelTrack[0] = isSub;
        } else {
            CoasterCartTrackHitCache.clearCartHit(cart);
            outSubLevelTrack[0] = false;
        }
        return hit;
    }

    static void remove(ServerSubLevel cart) {
        CoasterCartTrackHitCache.clearCartHit(cart);
        CoasterCartTrackHitCache.clearEngagedTick(cart);
    }

    static void clearEngagedTick(ServerSubLevel cart) {
        ENGAGED_TICK.remove(cart);
    }

    private static void clearCartHit(ServerSubLevel cart) {
        CART_HITS.remove(cart);
    }

    private static CoasterPathTrackFrame.TrackBasis alignedTrackBasis(CoasterPathTrackFrame.TrackBasis raw, boolean negateEdgeTangent) {
        if (!negateEdgeTangent) {
            return raw;
        }
        return new CoasterPathTrackFrame.TrackBasis(raw.forward().scale(-1.0), raw.right().scale(-1.0), raw.up());
    }

    private static void putCartHit(ServerSubLevel cart, CoasterPathTrackFrame.GraphHit hit, CoasterPathGraph graph, long gameTime, Vec3 bearingWorld, boolean subLevelTrack) {
        int edgeIndex = graph.indexOfEdge(hit.edge());
        if (edgeIndex < 0) {
            return;
        }
        CART_HITS.put(cart, new CartHitEntry(edgeIndex, hit.t(), graph.revision(), gameTime, bearingWorld.x, bearingWorld.y, bearingWorld.z, subLevelTrack));
    }

    @Nullable
    private static CoasterPathTrackFrame.GraphHit refineEngagedHit(Level level, CoasterPathGraph graph, CoasterPathTrackFrame.GraphHit engagedHit, Vec3 bearingWorld, double maxDistSq, ServerSubLevel cart) {
        int edgeIndex = graph.indexOfEdge(engagedHit.edge());
        if (edgeIndex < 0) {
            return null;
        }
        CartHitEntry prior = CART_HITS.get(cart);
        CartHitEntry seed = prior != null && prior.edgeIndex() == edgeIndex && prior.graphRevision() == graph.revision() ? prior : new CartHitEntry(edgeIndex, engagedHit.t(), graph.revision(), 0L, bearingWorld.x, bearingWorld.y, bearingWorld.z, true);
        return CoasterCartTrackHitCache.refineOnCachedEdge(level, graph, seed, bearingWorld, maxDistSq);
    }

    @Nullable
    private static CoasterPathTrackFrame.GraphHit refineOnCachedEdge(Level level, CoasterPathGraph graph, CartHitEntry cached, Vec3 bearingWorld, double maxDistSq) {
        CoasterPathTrackFrame.GraphHit hit;
        List<CoasterPathEdge> edges = graph.edges();
        if (cached.edgeIndex() < 0 || cached.edgeIndex() >= edges.size()) {
            return null;
        }
        CoasterPathEdge edge = edges.get(cached.edgeIndex());
        Vec3 lastBearing = cached.bearingX() == 0.0 && cached.bearingY() == 0.0 && cached.bearingZ() == 0.0 ? null : new Vec3(cached.bearingX(), cached.bearingY(), cached.bearingZ());
        CoasterPathTrackFrame.GraphHit graphHit = hit = cached.subLevelTrack() ? CoasterPathTrackFrame.closestHitOnEdgeForEngagedSnap(level, edge, bearingWorld, cached.t(), lastBearing) : CoasterPathTrackFrame.closestHitOnEdge(level, edge, bearingWorld);
        if (hit.point().distanceToSqr(bearingWorld) > maxDistSq) {
            return null;
        }
        return hit;
    }

    private static boolean sameEngagedHit(CoasterPathTrackFrame.GraphHit a, CoasterPathTrackFrame.GraphHit b) {
        if (a.edge() != b.edge()) {
            return false;
        }
        return Math.abs(a.t() - b.t()) <= 1.0E-5;
    }

    private static boolean nearEdgeEndpoint(double t) {
        return t <= 0.06 || t >= 0.94;
    }

    private record OpenEndTickEntry(long gameTime, int graphRevision, List<CoasterPathTraceEndpoints.OpenEndRef> refs) {
    }

    private record EngagedTickEntry(long gameTime, int graphRevision, boolean negateEdgeTangent, EngagedTickFrame frame) {
    }

    record EngagedTickFrame(CoasterPathTrackFrame.GraphHit graphHit, CoasterPathTrackFrame.TrackBasis trackBasis, Vector3d snapTargetWorld) {
    }

    private record CartHitEntry(int edgeIndex, double t, int graphRevision, long gameTime, double bearingX, double bearingY, double bearingZ, boolean subLevelTrack) {
    }
}

