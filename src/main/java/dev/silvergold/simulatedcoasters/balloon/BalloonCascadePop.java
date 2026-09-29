package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class BalloonCascadePop {
   private static final int CASCADE_DELAY_TICKS = 3;
   private static final List<BalloonCascadePop.Pending> PENDING = new ArrayList<>();

   private BalloonCascadePop() {
   }

   public static void onBalloonPopped(ServerLevel level, @Nullable UUID hostSubLevelId, Vector3d poppedPos, UUID poppedBalloonId) {
      if (hostSubLevelId != null) {
         ServerSubLevel next = nearestOtherBalloonOnHost(level, hostSubLevelId, poppedPos, poppedBalloonId);
         if (next != null) {
            schedule(level.dimension(), next.getUniqueId());
         }
      }
   }

   private static void schedule(ResourceKey<Level> dimension, UUID balloonId) {
      for (BalloonCascadePop.Pending p : PENDING) {
         if (p.dimension.equals(dimension) && p.balloonId.equals(balloonId)) {
            return;
         }
      }

      PENDING.add(new BalloonCascadePop.Pending(dimension, balloonId, 3));
   }

   private static boolean isScheduled(ResourceKey<Level> dimension, UUID balloonId) {
      for (BalloonCascadePop.Pending p : PENDING) {
         if (p.dimension.equals(dimension) && p.balloonId.equals(balloonId)) {
            return true;
         }
      }

      return false;
   }

   public static void clearDimension(ResourceKey<Level> dimension) {
      PENDING.removeIf(p -> p.dimension.equals(dimension));
   }

   public static void serverTick(MinecraftServer server) {
      if (!PENDING.isEmpty()) {
         List<Runnable> due = new ArrayList<>();
         Iterator<BalloonCascadePop.Pending> it = PENDING.iterator();

         while (it.hasNext()) {
            BalloonCascadePop.Pending p = it.next();
            if (--p.ticksLeft <= 0) {
               it.remove();
               ServerLevel level = server.getLevel(p.dimension);
               if (level != null) {
                  ServerSubLevel balloon = BalloonResolve.findBalloonById(level, p.balloonId);
                  if (balloon != null) {
                     due.add(() -> BalloonRemoval.pop(level, balloon));
                  }
               }
            }
         }

         for (Runnable r : due) {
            r.run();
         }
      }
   }

   @Nullable
   private static ServerSubLevel nearestOtherBalloonOnHost(ServerLevel level, UUID hostSubLevelId, Vector3d fromPos, UUID excludeBalloonId) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         ServerSubLevel best = null;
         double bestDistSq = Double.MAX_VALUE;

         for (SubLevel sub : container.getAllSubLevels()) {
            if (sub instanceof ServerSubLevel) {
               ServerSubLevel balloon = (ServerSubLevel)sub;
               if (!balloon.isRemoved()
                  && !balloon.getUniqueId().equals(excludeBalloonId)
                  && BalloonResolve.isBalloonSubLevel(balloon)
                  && !BalloonRemoval.isRemoving(balloon.getUniqueId())
                  && !isScheduled(level.dimension(), balloon.getUniqueId())) {
                  UUID host = hostSubLevelIdOf(level, balloon);
                  if (host != null && host.equals(hostSubLevelId)) {
                     double distSq = balloon.logicalPose().position().distanceSquared(fromPos);
                     if (distSq < bestDistSq) {
                        bestDistSq = distSq;
                        best = balloon;
                     }
                  }
               }
            }
         }

         return best;
      } else {
         return null;
      }
   }

   @Nullable
   public static UUID hostSubLevelIdOf(ServerLevel level, ServerSubLevel balloon) {
      if (balloon.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof BalloonBlockEntity be) {
         ServerSubLevel host = be.resolveHostSubLevel(level);
         return host != null ? host.getUniqueId() : be.hostSubLevelId();
      } else {
         return null;
      }
   }

   private static final class Pending {
      final ResourceKey<Level> dimension;
      final UUID balloonId;
      int ticksLeft;

      Pending(ResourceKey<Level> dimension, UUID balloonId, int ticksLeft) {
         this.dimension = dimension;
         this.balloonId = balloonId;
         this.ticksLeft = ticksLeft;
      }
   }
}
