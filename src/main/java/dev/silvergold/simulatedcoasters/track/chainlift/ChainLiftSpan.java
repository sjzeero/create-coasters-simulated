/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftCommittedSegment;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftEndRef;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftSpan {
    private ChainLiftSpan() {
    }

    @Nullable
    public static CoasterPathGraphSpans.SpanPath resolveEffective(Level level, BlockPos startAnchor, ChainLiftEndRef endRef, double committedLength, BlockPos firstHop, double partialTickForRender) {
        if (committedLength <= 0.0) {
            return null;
        }
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        @Nullable CoasterPathGraphSpans.SpanPath ideal = ChainLiftSpan.resolveIdealPath(level, graph, startAnchor, endRef, partialTickForRender);
        if (ideal != null) {
            return CoasterPathGraphSpans.truncate(ideal, committedLength, level, partialTickForRender);
        }
        return ChainLiftSpan.reachableForwardSpan(level, graph, startAnchor, firstHop, committedLength, partialTickForRender);
    }

    public static BlockPos firstHopNode(CoasterPathGraphSpans.SpanPath path, BlockPos startAnchor) {
        if (path.segments().isEmpty()) {
            return BlockPos.ZERO;
        }
        CoasterPathGraphSpans.PathSegment first = path.segments().getFirst();
        CoasterPathEdge edge = first.edge();
        if (edge.from().equals((Object)startAnchor)) {
            return edge.to();
        }
        if (edge.to().equals((Object)startAnchor)) {
            return edge.from();
        }
        return ChainLiftSpan.exitNodeAtT(edge, first.tStart());
    }

    public static List<ChainLiftCommittedSegment> segmentsFromPath(CoasterPathGraphSpans.SpanPath path) {
        ArrayList<ChainLiftCommittedSegment> out = new ArrayList<ChainLiftCommittedSegment>(path.segments().size());
        for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
            out.add(ChainLiftCommittedSegment.from(segment));
        }
        return List.copyOf(out);
    }

    @Nullable
    public static CoasterPathGraphSpans.SpanPath rebindCommittedSpan(Level level, List<ChainLiftCommittedSegment> stored, double committedLength, double partialTickForRender) {
        if (stored.isEmpty() || committedLength <= 0.0) {
            return null;
        }
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        ArrayList<CoasterPathGraphSpans.PathSegment> segments = new ArrayList<CoasterPathGraphSpans.PathSegment>(stored.size());
        for (ChainLiftCommittedSegment storedSegment : stored) {
            @Nullable CoasterPathEdge edge = ChainLiftSpan.findEdge(graph, storedSegment.edgeFrom(), storedSegment.edgeTo());
            if (edge == null) {
                return null;
            }
            segments.add(new CoasterPathGraphSpans.PathSegment(edge, storedSegment.tStart(), storedSegment.tEnd()));
        }
        double total = 0.0;
        for (CoasterPathGraphSpans.PathSegment segment : segments) {
            total += CoasterPathGraphSpans.arcLengthBetween(level, segment.edge(), segment.tStart(), segment.tEnd(), partialTickForRender);
        }
        return CoasterPathGraphSpans.truncate(new CoasterPathGraphSpans.SpanPath(List.copyOf(segments), total), committedLength, level, partialTickForRender);
    }

    @Nullable
    public static CoasterPathGraphSpans.SpanPath resolveIdealPath(Level level, CoasterPathGraph graph, BlockPos startAnchor, ChainLiftEndRef endRef, double partialTickForRender) {
        return switch (endRef.kind()) {
            default -> throw new MatchException(null, null);
            case ChainLiftEndRef.Kind.ANCHOR -> {
                BlockPos end = endRef.endAnchor();
                if (end.equals((Object)BlockPos.ZERO) || end.equals((Object)startAnchor)) {
                    yield null;
                }
                yield CoasterPathGraphSpans.spanPathBetweenAnchors(level, graph, startAnchor, end, partialTickForRender);
            }
            case ChainLiftEndRef.Kind.CURVE -> {
                @Nullable CoasterPathEdge edge = ChainLiftSpan.findEdge(graph, endRef.edgeFrom(), endRef.edgeTo());
                if (edge == null) {
                    yield null;
                }
                CoasterPathTrackFrame.GraphHit hit = new CoasterPathTrackFrame.GraphHit(Vec3.ZERO, edge, endRef.endT());
                yield CoasterPathGraphSpans.spanPathFromAnchor(level, graph, startAnchor, hit, partialTickForRender);
            }
        };
    }

    @Nullable
    private static CoasterPathGraphSpans.SpanPath reachableForwardSpan(Level level, CoasterPathGraph graph, BlockPos startAnchor, BlockPos firstHop, double maxLength, double partialTickForRender) {
        double exitT;
        double entryT;
        CoasterPathEdge edge;
        double walked;
        double fullSegLen;
        if (firstHop == null || firstHop.equals((Object)BlockPos.ZERO) || firstHop.equals((Object)startAnchor)) {
            return null;
        }
        ArrayList<CoasterPathGraphSpans.PathSegment> segments = new ArrayList<CoasterPathGraphSpans.PathSegment>();
        BlockPos prev = startAnchor;
        BlockPos current = firstHop;
        int hops = 0;
        int maxHops = Math.max(4, graph.edges().size() + 1);
        for (walked = 0.0; current != null && walked < maxLength - 1.0E-9 && ++hops <= maxHops && (edge = ChainLiftSpan.findEdge(graph, prev, current)) != null && !((fullSegLen = CoasterPathGraphSpans.arcLengthBetween(level, edge, entryT = edge.from().equals((Object)prev) ? 0.0 : 1.0, exitT = edge.from().equals((Object)current) ? 0.0 : 1.0, partialTickForRender)) < 1.0E-9); walked += fullSegLen) {
            double remaining = maxLength - walked;
            if (fullSegLen <= remaining + 1.0E-9) {
                segments.add(new CoasterPathGraphSpans.PathSegment(edge, entryT, exitT));
            } else {
                double frac = remaining / fullSegLen;
                double cutT = Mth.lerp((double)frac, (double)entryT, (double)exitT);
                segments.add(new CoasterPathGraphSpans.PathSegment(edge, entryT, cutT));
                walked = maxLength;
                break;
            }
            BlockPos next = ChainLiftSpan.pickNextNode(graph, current, prev);
            if (next == null || next.equals((Object)current) || next.equals((Object)prev)) break;
            prev = current;
            current = next;
        }
        return segments.isEmpty() ? null : new CoasterPathGraphSpans.SpanPath(List.copyOf(segments), walked);
    }

    private static BlockPos exitNodeAtT(CoasterPathEdge edge, double tStart) {
        return tStart <= 0.5 ? edge.to() : edge.from();
    }

    @Nullable
    private static BlockPos pickNextNode(CoasterPathGraph graph, BlockPos current, BlockPos prev) {
        @Nullable BlockPos next = null;
        for (CoasterPathEdge edge : graph.edges()) {
            BlockPos candidate;
            if (edge.from().equals((Object)current)) {
                candidate = edge.to();
            } else {
                if (!edge.to().equals((Object)current)) continue;
                candidate = edge.from();
            }
            if (candidate.equals((Object)prev)) continue;
            if (next != null && !next.equals((Object)candidate)) {
                return null;
            }
            next = candidate;
        }
        return next;
    }

    @Nullable
    private static CoasterPathEdge findEdge(CoasterPathGraph graph, BlockPos from, BlockPos to) {
        if (from.equals((Object)BlockPos.ZERO) || to.equals((Object)BlockPos.ZERO)) {
            return null;
        }
        for (CoasterPathEdge edge : graph.edges()) {
            if (edge.from().equals((Object)from) && edge.to().equals((Object)to)) {
                return edge;
            }
            if (!edge.from().equals((Object)to) || !edge.to().equals((Object)from)) continue;
            return edge;
        }
        return null;
    }
}

