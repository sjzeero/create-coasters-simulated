package dev.silvergold.simulatedcoasters.track.chainlift;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftKineticConflict {
   private static final ThreadLocal<Boolean> REDIRECTING = ThreadLocal.withInitial(() -> false);

   private ChainLiftKineticConflict() {
   }

   public static boolean redirectDestroyAwayFromAnchor(Level level, BlockPos pos, boolean drop) {
      if (!level.isClientSide() && !Boolean.TRUE.equals(REDIRECTING.get())) {
         if (level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity anchor && anchor.hasChainLiftShaft()) {
            KineticBlockEntity weaker = findWeakerShaftNeighbour(level, anchor);
            if (weaker == null) {
               return level.destroyBlock(pos, drop);
            }

            REDIRECTING.set(true);

            boolean var5;
            try {
               var5 = level.destroyBlock(weaker.getBlockPos(), drop);
            } finally {
               REDIRECTING.set(false);
            }

            return var5;
         }

         return level.destroyBlock(pos, drop);
      } else {
         return level.destroyBlock(pos, drop);
      }
   }

   @Nullable
   private static KineticBlockEntity findWeakerShaftNeighbour(Level level, CoasterAnchorpointBlockEntity anchor) {
      Axis axis = anchor.getChainLiftShaftAxis();
      if (axis == null) {
         return null;
      } else {
         KineticBlockEntity weaker = null;
         float weakerAbsSpeed = Float.POSITIVE_INFINITY;
         long weakerPosKey = Long.MAX_VALUE;

         for (Direction face : Direction.values()) {
            if (face.getAxis() == axis) {
               BlockPos neighbourPos = anchor.getBlockPos().relative(face);
               BlockEntity absSpeed = level.getBlockEntity(neighbourPos);
               if (absSpeed instanceof KineticBlockEntity) {
                  KineticBlockEntity neighbour = (KineticBlockEntity)absSpeed;
                  if (!(neighbour instanceof CoasterAnchorpointBlockEntity)
                     && (RotationPropagator.isConnected(anchor, neighbour) || RotationPropagator.isConnected(neighbour, anchor))) {
                     float absSpeedx = Math.abs(neighbour.getTheoreticalSpeed());
                     long posKey = neighbourPos.asLong();
                     boolean weakerSpeed = absSpeedx < weakerAbsSpeed - 1.0E-4F;
                     boolean tieBreak = Math.abs(absSpeedx - weakerAbsSpeed) <= 1.0E-4F && posKey < weakerPosKey;
                     if (weaker == null || weakerSpeed || tieBreak) {
                        weaker = neighbour;
                        weakerAbsSpeed = absSpeedx;
                        weakerPosKey = posKey;
                     }
                  }
               }
            }
         }

         return weaker;
      }
   }
}
