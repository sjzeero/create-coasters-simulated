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
   private static final Map<String, Couple<Double>> BY_EDGE = Collections.synchronizedMap(new HashMap<>());
   private static final Map<BezierConnection, Couple<Double>> INSTANCE_LENGTHS = Collections.synchronizedMap(new WeakHashMap<>());
   private static final Map<BezierConnection, Couple<Double>> PREVIEW_BY_CONNECTION = Collections.synchronizedMap(new WeakHashMap<>());
   private static final AtomicLong REVISION = new AtomicLong();

   private CoasterPeerCurveHandleLengths() {
   }

   public static long revision() {
      return REVISION.get();
   }

   public static boolean isCoaster(BezierConnection bc) {
      return bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
   }

   public static String edgeKey(BezierConnection bc) {
      return edgeKeyFromEndpoints((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond());
   }

   public static String edgeKeyFromEndpoints(BlockPos a, BlockPos b) {
      long la = a.asLong();
      long lb = b.asLong();
      return la <= lb ? la + ":" + lb : lb + ":" + la;
   }

   public static BlockPos lexMin(BlockPos a, BlockPos b) {
      return a.asLong() <= b.asLong() ? a : b;
   }

   public static BlockPos lexMax(BlockPos a, BlockPos b) {
      return a.asLong() <= b.asLong() ? b : a;
   }

   @Nullable
   public static Couple<Double> getCanonicalLengths(BezierConnection bc) {
      if (!isCoaster(bc)) {
         return null;
      } else {
         Couple<Double> preview = PREVIEW_BY_CONNECTION.get(bc);
         if (preview != null) {
            return preview;
         } else {
            Couple<Double> inst = INSTANCE_LENGTHS.get(bc);
            return inst != null ? inst : BY_EDGE.get(edgeKey(bc));
         }
      }
   }

   public static double getLengthAtEndpoint(BezierConnection bc, int endpointIndex) {
      Couple<Double> canon = getCanonicalLengths(bc);
      double fallback = fallbackSingleHandleLength(bc);
      if (canon == null) {
         return fallback;
      } else {
         BlockPos lo = lexMin((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond());
         boolean firstIsLo = ((BlockPos)bc.bePositions.getFirst()).equals(lo);
         double h0 = firstIsLo ? (Double)canon.getFirst() : (Double)canon.getSecond();
         double h1 = firstIsLo ? (Double)canon.getSecond() : (Double)canon.getFirst();
         return endpointIndex == 0 ? h0 : h1;
      }
   }

   private static double fallbackSingleHandleLength(BezierConnection bc) {
      return bc.getHandleLength();
   }

   public static Vec3 controlPointWorld(BezierConnection bc, int endpointIndex) {
      Vec3 start = endpointIndex == 0 ? (Vec3)bc.starts.getFirst() : (Vec3)bc.starts.getSecond();
      Vec3 axis = endpointIndex == 0 ? (Vec3)bc.axes.getFirst() : (Vec3)bc.axes.getSecond();
      double h = getLengthAtEndpoint(bc, endpointIndex);
      return start.add(axis.scale(h));
   }

   public static double maxHandleLengthForEdge(Level level, BezierConnection bc) {
      if (!isCoaster(bc)) {
         return Double.MAX_VALUE;
      } else {
         BlockPos p0 = (BlockPos)bc.bePositions.getFirst();
         BlockPos p1 = (BlockPos)bc.bePositions.getSecond();
         Vec3 c0 = CoasterAnchorpointBlockEntity.worldCenter(level, p0);
         Vec3 c1 = CoasterAnchorpointBlockEntity.worldCenter(level, p1);
         double chord = c0.distanceTo(c1);
         return Math.max(1.0E-4, chord * 0.7);
      }
   }

   public static void putCanonical(BezierConnection bc, double hAtLo, double hAtHi) {
      if (isCoaster(bc)) {
         Couple<Double> lengths = Couple.create(sanitizeLength(hAtLo), sanitizeLength(hAtHi));
         BY_EDGE.put(edgeKey(bc), lengths);
         bindInstanceLengths(bc, lengths);
      }
   }

   private static void bindInstanceLengths(BezierConnection bc, Couple<Double> lengths) {
      REVISION.incrementAndGet();
      INSTANCE_LENGTHS.put(bc, lengths);
      BezierConnection other = bc.isPrimary() ? bc.secondary() : bc;
      if (other != bc) {
         INSTANCE_LENGTHS.put(other, lengths);
      }
   }

   public static void putPreviewCanonical(BezierConnection bc, double hAtLo, double hAtHi) {
      if (isCoaster(bc)) {
         REVISION.incrementAndGet();
         PREVIEW_BY_CONNECTION.put(bc, Couple.create(sanitizeLength(hAtLo), sanitizeLength(hAtHi)));
      }
   }

   public static void commitPreviewLengths(BezierConnection bc) {
      if (isCoaster(bc)) {
         REVISION.incrementAndGet();
         Couple<Double> preview = PREVIEW_BY_CONNECTION.remove(bc);
         if (preview != null) {
            BY_EDGE.put(edgeKey(bc), preview);
         }
      }
   }

   public static void removeEdge(BezierConnection bc) {
      if (bc != null) {
         REVISION.incrementAndGet();
         BY_EDGE.remove(edgeKey(bc));
      }
   }

   @Nullable
   public static Couple<Double> takeCanonicalLengthsForRemap(BezierConnection primary) {
      if (isCoaster(primary) && primary.isPrimary()) {
         REVISION.incrementAndGet();
         return BY_EDGE.remove(edgeKey(primary));
      } else {
         return null;
      }
   }

   private static double sanitizeLength(double h) {
      return !Double.isNaN(h) && !(h < 1.0E-4) ? h : 1.0E-4;
   }

   public static void putSymmetricFromRuntime(BezierConnection primary) {
      if (isCoaster(primary) && primary.isPrimary()) {
         if (getCanonicalLengths(primary) == null) {
            String key = edgeKey(primary);
            BY_EDGE.remove(key);
            double h = primary.getHandleLength();
            putCanonical(primary, h, h);
         }
      }
   }

   public static void copyEdgeLengths(BezierConnection fromGraph, BezierConnection toGraph) {
      if (isCoaster(toGraph)) {
         Couple<Double> c = getCanonicalLengths(fromGraph);
         if (c != null) {
            BY_EDGE.put(edgeKey(toGraph), c);
            bindInstanceLengths(toGraph, c);
         }
      }
   }

   public static void carryLengthsToReplacement(BezierConnection from, BezierConnection to) {
      if (isCoaster(from) && isCoaster(to) && edgeKey(from).equals(edgeKey(to))) {
         Couple<Double> preview = PREVIEW_BY_CONNECTION.get(from);
         if (preview != null) {
            putPreviewCanonical(to, (Double)preview.getFirst(), (Double)preview.getSecond());
         } else {
            copyEdgeLengths(from, to);
         }
      }
   }
}
