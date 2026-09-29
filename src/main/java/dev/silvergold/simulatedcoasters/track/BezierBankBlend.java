package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class BezierBankBlend {
   private static final float CHAIN_MONOTONIC_EPS = 1.0E-4F;
   private static final float GEOMETRIC_FRAME_ALIGNMENT_DOT_MIN = 0.55F;
   private static final int MAX_JOINT_BANK_CHAIN_ANCHORS = 512;

   private BezierBankBlend() {
   }

   public static float smoothJointBankParameter(float rawT, @Nullable BezierConnection bc) {
      return smoothJointBankParameter(rawT, bc, BezierBankBlendLevel.tryGetClientLevel());
   }

   public static float smoothJointBankParameter(double rawT, @Nullable BezierConnection bc) {
      return smoothJointBankParameter((float)rawT, bc);
   }

   public static float smoothJointBankParameter(double rawT, @Nullable BezierConnection bc, @Nullable Level level) {
      return smoothJointBankParameter((float)rawT, bc, level);
   }

   public static float smoothJointBankParameter(float rawT, @Nullable BezierConnection bc, @Nullable Level level) {
      if (bc == null || !bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
         return rawT;
      } else if (level == null) {
         return rawT;
      } else {
         bc = bc.isPrimary() ? bc : bc.secondary();
         BlockPos a0 = (BlockPos)bc.bePositions.getFirst();
         BlockPos a1 = (BlockPos)bc.bePositions.getSecond();
         float thetaP = jointBankRadiansAt(level, a0);
         float thetaQ = jointBankRadiansAt(level, a1);
         float dTheta = thetaQ - thetaP;
         if (Math.abs(dTheta) < 1.0E-6F) {
            return rawT;
         } else {
            List<BlockPos> chain = orderedAnchorsAlongJointBankChain(level, a0, a1, bc);
            int edgeIdx = edgeStartIndexInChain(chain, a0, a1);
            if (segmentInLongMonotonicRun(level, chain, edgeIdx)) {
               return Mth.clamp(rawT, 0.0F, 1.0F);
            } else if (geometricFaceNormalsAlignedAcrossSegment(level, a0, a1, bc) < 0.55F) {
               return Mth.clamp(rawT, 0.0F, 1.0F);
            } else {
               BezierConnection prevNeighbour = neighbourCoasterCurveAtAnchor(level, a0, bc);
               BezierConnection nextNeighbour = neighbourCoasterCurveAtAnchor(level, a1, bc);
               float thetaPrev = prevNeighbour != null ? jointBankRadiansAt(level, otherEndpoint(prevNeighbour, a0)) : thetaP;
               float thetaNext = nextNeighbour != null ? jointBankRadiansAt(level, otherEndpoint(nextNeighbour, a1)) : thetaQ;
               float mP = 0.0F;
               if (prevNeighbour != null) {
                  BlockPos aPrev = otherEndpoint(prevNeighbour, a0);
                  boolean prevOk = geometricFaceNormalsAlignedAcrossSegment(level, aPrev, a0, prevNeighbour) >= 0.55F;
                  mP = prevOk ? (thetaQ - thetaPrev) * 0.5F : 0.0F;
               }

               float mQ = 0.0F;
               if (nextNeighbour != null) {
                  BlockPos aNext = otherEndpoint(nextNeighbour, a1);
                  boolean nextOk = geometricFaceNormalsAlignedAcrossSegment(level, a1, aNext, nextNeighbour) >= 0.55F;
                  mQ = nextOk ? (thetaNext - thetaP) * 0.5F : 0.0F;
               }

               float t = Mth.clamp(rawT, 0.0F, 1.0F);
               float t2 = t * t;
               float t3 = t2 * t;
               float h00 = 2.0F * t3 - 3.0F * t2 + 1.0F;
               float h10 = t3 - 2.0F * t2 + t;
               float h01 = -2.0F * t3 + 3.0F * t2;
               float h11 = t3 - t2;
               float theta = h00 * thetaP + h10 * mP + h01 * thetaQ + h11 * mQ;
               float alpha = (theta - thetaP) / dTheta;
               return Mth.clamp(alpha, 0.0F, 1.0F);
            }
         }
      }
   }

   public static long mixBankBlendInputSignature(@Nullable Level level, @Nullable BezierConnection bc, long hash) {
      if (level != null && bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
         BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
         BlockPos a0 = (BlockPos)p.bePositions.getFirst();
         BlockPos a1 = (BlockPos)p.bePositions.getSecond();
         hash = mixFloat(hash, jointBankRadiansAt(level, a0));
         hash = mixFloat(hash, jointBankRadiansAt(level, a1));
         hash = mixChainNeighbour(level, a0, p, hash);
         return mixChainNeighbour(level, a1, p, hash);
      } else {
         return hash;
      }
   }

   private static long mixChainNeighbour(Level level, BlockPos anchorPos, BezierConnection self, long hash) {
      BezierConnection neighbour = neighbourCoasterCurveAtAnchor(level, anchorPos, self);
      if (neighbour == null) {
         return mixLong(hash, 0L);
      } else {
         BlockPos far = otherEndpoint(neighbour, anchorPos);
         return mixFloat(mixLong(hash, far.asLong()), jointBankRadiansAt(level, far));
      }
   }

   private static long mixLong(long hash, long value) {
      return (hash ^ value) * 1099511628211L;
   }

   private static long mixFloat(long hash, float value) {
      return mixLong(hash, (long)Float.floatToIntBits(value));
   }

   private static float jointBankRadiansAt(Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be ? be.getJointBankRadians() : 0.0F;
   }

   @Nullable
   private static BezierConnection neighbourCoasterCurveAtAnchor(Level level, BlockPos anchorPos, BezierConnection self) {
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
         if (incident.size() != 2) {
            return null;
         } else {
            BezierConnection a = incident.get(0);
            BezierConnection b = incident.get(1);
            BezierConnection other = sameEdge(a, self) ? b : a;
            return sameEdge(other, self) ? null : other;
         }
      } else {
         return null;
      }
   }

   private static boolean sameEdge(BezierConnection a, BezierConnection b) {
      BlockPos a0 = (BlockPos)a.bePositions.getFirst();
      BlockPos a1 = (BlockPos)a.bePositions.getSecond();
      BlockPos b0 = (BlockPos)b.bePositions.getFirst();
      BlockPos b1 = (BlockPos)b.bePositions.getSecond();
      return a0.equals(b0) && a1.equals(b1) || a0.equals(b1) && a1.equals(b0);
   }

   private static BlockPos otherEndpoint(BezierConnection bc, BlockPos thisEnd) {
      return ((BlockPos)bc.bePositions.getFirst()).equals(thisEnd) ? (BlockPos)bc.bePositions.getSecond() : (BlockPos)bc.bePositions.getFirst();
   }

   private static List<BlockPos> orderedAnchorsAlongJointBankChain(Level level, BlockPos a0, BlockPos a1, BezierConnection segment) {
      ArrayList<BlockPos> anchors = new ArrayList<>();
      anchors.add(a0);
      anchors.add(a1);
      Set<BlockPos> visited = new HashSet<>();
      visited.add(a0);
      visited.add(a1);
      BlockPos left = a0;
      BezierConnection inc = segment;

      while (anchors.size() < 512) {
         BezierConnection nb = neighbourCoasterCurveAtAnchor(level, left, inc);
         if (nb == null) {
            break;
         }

         BlockPos prev = otherEndpoint(nb, left);
         if (!visited.add(prev)) {
            break;
         }

         anchors.add(0, prev);
         left = prev;
         inc = nb;
      }

      BlockPos right = a1;
      inc = segment;

      while (anchors.size() < 512) {
         BezierConnection nbx = neighbourCoasterCurveAtAnchor(level, right, inc);
         if (nbx == null) {
            break;
         }

         BlockPos next = otherEndpoint(nbx, right);
         if (!visited.add(next)) {
            break;
         }

         anchors.add(next);
         right = next;
         inc = nbx;
      }

      return anchors;
   }

   private static int edgeStartIndexInChain(List<BlockPos> chain, BlockPos segStart, BlockPos segEnd) {
      for (int i = 0; i < chain.size() - 1; i++) {
         if (chain.get(i).equals(segStart) && chain.get(i + 1).equals(segEnd)) {
            return i;
         }
      }

      return -1;
   }

   private static boolean segmentInLongMonotonicRun(Level level, List<BlockPos> chain, int edgeStartIdx) {
      if (chain.size() >= 3 && edgeStartIdx >= 0) {
         float[] unwrapped = unwrappedJointBankChain(level, chain);
         if (unwrapped == null) {
            return false;
         } else {
            for (int[] run : maximalWeaklyMonotonicRuns(unwrapped)) {
               int rs = run[0];
               int re = run[1];
               if (re - rs + 1 >= 3 && rs <= edgeStartIdx && edgeStartIdx + 1 <= re) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   @Nullable
   private static float[] unwrappedJointBankChain(Level level, List<BlockPos> orderedAnchors) {
      int n = orderedAnchors.size();
      if (n < 2) {
         return null;
      } else {
         float[] raw = new float[n];

         for (int i = 0; i < n; i++) {
            raw[i] = jointBankRadiansAt(level, orderedAnchors.get(i));
         }

         float[] unwrapped = new float[n];
         unwrapped[0] = raw[0];

         for (int i = 1; i < n; i++) {
            double d = (double)(raw[i] - raw[i - 1]);

            while (d > Math.PI) {
               d -= (float) (Math.PI * 2);
            }

            while (d < -Math.PI) {
               d += (float) (Math.PI * 2);
            }

            unwrapped[i] = unwrapped[i - 1] + (float)d;
         }

         return unwrapped;
      }
   }

   private static List<int[]> maximalWeaklyMonotonicRuns(float[] unwrapped) {
      int n = unwrapped.length;
      ArrayList<int[]> runs = new ArrayList<>();
      if (n < 2) {
         return runs;
      } else {
         int runStart = 0;
         Boolean increasing = null;

         for (int ei = 0; ei < n - 1; ei++) {
            float d = unwrapped[ei + 1] - unwrapped[ei];
            if (!(Math.abs(d) < 1.0E-4F)) {
               boolean inc = d > 0.0F;
               if (increasing == null) {
                  increasing = inc;
               } else if (increasing != inc) {
                  runs.add(new int[]{runStart, ei});
                  runStart = ei;
                  increasing = inc;
               }
            }
         }

         runs.add(new int[]{runStart, n - 1});
         return runs;
      }
   }

   private static float geometricFaceNormalsAlignedAcrossSegment(Level level, BlockPos u, BlockPos v, BezierConnection seg) {
      Vec3 axU = CoasterBezierHandleEdit.tangentIntoFromAnchor(seg, u);
      Vec3 axV = CoasterBezierHandleEdit.tangentIntoFromAnchor(seg, v);
      if (axU != null && axV != null) {
         Vec3 gU = CoasterTrackPlacement.anchorGeometricFaceNormal(level, u, axU);
         Vec3 gV = CoasterTrackPlacement.anchorGeometricFaceNormal(level, v, axV);
         return (float)Math.abs(gU.dot(gV));
      } else {
         return 1.0F;
      }
   }
}
