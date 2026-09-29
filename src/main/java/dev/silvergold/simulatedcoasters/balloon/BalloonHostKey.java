package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public record BalloonHostKey(UUID hostSubLevelId, BlockPos cell) {
   public static final BlockPos BALLOON_BLOCK_CELL = BlockPos.ZERO;

   public BalloonHostKey(UUID hostSubLevelId, BlockPos cell) {
      cell = cell.immutable();
      this.hostSubLevelId = hostSubLevelId;
      this.cell = cell;
   }

   @Nullable
   public static BalloonHostKey of(Level level, BlockPos clicked) {
      SubLevel sub = CoasterCartResolve.findSubLevelAt(level, clicked);
      if (sub != null && !sub.isRemoved()) {
         BlockPos plotCell = CoasterCartResolve.plotLocalCellAt(level, sub, clicked);
         if (plotCell == null) {
            return null;
         } else if (sub instanceof ServerSubLevel hostSub) {
            return hostSub.getPlot().getEmbeddedLevelAccessor().getBlockState(plotCell).isAir() ? null : new BalloonHostKey(sub.getUniqueId(), plotCell);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static BlockPos normalizeHostPlotCell(ServerSubLevel hostSub, BlockPos stored) {
      if (isLikelyPlotLocal(stored) && hasBlockAt(hostSub, stored)) {
         return stored;
      } else {
         BlockPos fromCenterOffset = stored.subtract(hostSub.getPlot().getCenterBlock());
         if (hasBlockAt(hostSub, fromCenterOffset)) {
            return fromCenterOffset;
         } else {
            if (hostSub.getLevel() != null) {
               BlockPos fromPlaySpace = CoasterCartResolve.plotLocalCellAt(hostSub.getLevel(), hostSub, stored);
               if (fromPlaySpace != null && hasBlockAt(hostSub, fromPlaySpace)) {
                  return fromPlaySpace;
               }
            }

            return stored;
         }
      }
   }

   @Nullable
   public static BalloonHostKey relocateHostKey(ServerLevel level, BalloonBlockEntity be, @Nullable ServerSubLevel balloonSub) {
      ServerSubLevel hostSub = be.resolveHostSubLevel(level);
      if (hostSub != null) {
         BlockPos plotLocal = normalizeHostPlotCell(hostSub, be.hostPos());
         return new BalloonHostKey(hostSub.getUniqueId(), plotLocal);
      } else {
         BlockPos lattice = be.hostLatticePos();
         if (lattice != null) {
            BalloonHostKey fromLattice = of(level, lattice);
            if (fromLattice != null) {
               return fromLattice;
            }
         }

         if (balloonSub != null) {
            BalloonHostKey fromTether = locateByStringTie(level, balloonSub);
            if (fromTether != null) {
               return fromTether;
            }
         }

         SubLevelContainer raw = SubLevelContainer.getContainer(level);
         if (raw instanceof ServerSubLevelContainer container) {
            ArrayList<BalloonHostKey> candidates = new ArrayList<>();

            for (SubLevel sub : container.getAllSubLevels()) {
               if (sub instanceof ServerSubLevel) {
                  ServerSubLevel ssl = (ServerSubLevel)sub;
                  if (!ssl.isRemoved() && !BalloonResolve.isBalloonSubLevel(ssl)) {
                     BlockPos plotLocal = normalizeHostPlotCell(ssl, be.hostPos());
                     if (hasBlockAt(ssl, plotLocal)) {
                        candidates.add(new BalloonHostKey(ssl.getUniqueId(), plotLocal));
                     }
                  }
               }
            }

            if (candidates.isEmpty()) {
               return null;
            } else if (candidates.size() == 1) {
               return (BalloonHostKey)candidates.get(0);
            } else if (balloonSub == null) {
               return (BalloonHostKey)candidates.get(0);
            } else {
               Vector3d stringWorld = balloonSub.logicalPose()
                  .transformPosition(BalloonPlacement.stringTiePlot(balloonSub, BALLOON_BLOCK_CELL), new Vector3d());
               BalloonHostKey best = null;
               double bestDistSq = Double.MAX_VALUE;
               double maxDistSq = 64.0;

               for (BalloonHostKey candidate : candidates) {
                  ServerSubLevel host = BalloonPlacement.resolveHostSub(level, candidate.hostSubLevelId());
                  if (host != null) {
                     Vector3d anchor = BalloonPlacement.hostAnchorWorld(host, candidate.cell());
                     if (anchor != null) {
                        double distSq = anchor.distanceSquared(stringWorld);
                        if (distSq < bestDistSq && distSq <= maxDistSq) {
                           bestDistSq = distSq;
                           best = candidate;
                        }
                     }
                  }
               }

               return best != null ? best : (BalloonHostKey)candidates.get(0);
            }
         } else {
            return null;
         }
      }
   }

   @Nullable
   public static BalloonHostKey locateByStringTie(ServerLevel level, ServerSubLevel balloonSub) {
      Vector3d stringWorld = balloonSub.logicalPose().transformPosition(BalloonPlacement.stringTiePlot(balloonSub, BALLOON_BLOCK_CELL), new Vector3d());
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (!(raw instanceof ServerSubLevelContainer container)) {
         return null;
      } else {
         BalloonHostKey best = null;
         double bestScore = Double.MAX_VALUE;
         double maxHorizSq = 144.0;

         for (SubLevel sub : container.getAllSubLevels()) {
            if (sub instanceof ServerSubLevel hostSub && !hostSub.isRemoved() && !BalloonResolve.isBalloonSubLevel(hostSub)) {
               EmbeddedPlotLevelAccessor embedded = hostSub.getPlot().getEmbeddedLevelAccessor();
               BoundingBox3ic bounds = hostSub.getPlot().getBoundingBox();
               BlockPos center = hostSub.getPlot().getCenterBlock();

               for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                  for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                     for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                        BlockPos plotLocal = new BlockPos(x, y, z).subtract(center);
                        if (!embedded.getBlockState(plotLocal).isAir()) {
                           Vector3d anchor = BalloonPlacement.hostAnchorWorld(hostSub, plotLocal);
                           if (anchor != null) {
                              double dy = stringWorld.y - anchor.y;
                              if (!(dy < -0.5)) {
                                 double horizSq = (anchor.x - stringWorld.x) * (anchor.x - stringWorld.x)
                                    + (anchor.z - stringWorld.z) * (anchor.z - stringWorld.z);
                                 if (!(horizSq > maxHorizSq)) {
                                    double score = horizSq + dy * dy * 0.25;
                                    if (score < bestScore) {
                                       bestScore = score;
                                       best = new BalloonHostKey(hostSub.getUniqueId(), plotLocal);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return best;
      }
   }

   private static boolean isLikelyPlotLocal(BlockPos pos) {
      return Math.abs(pos.getX()) < 4096 && Math.abs(pos.getZ()) < 4096;
   }

   private static boolean hasBlockAt(ServerSubLevel hostSub, BlockPos plotCell) {
      return !hostSub.getPlot().getEmbeddedLevelAccessor().getBlockState(plotCell).isAir();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else {
         return !(obj instanceof BalloonHostKey other) ? false : this.hostSubLevelId.equals(other.hostSubLevelId) && this.cell.equals(other.cell);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.hostSubLevelId, this.cell);
   }
}
