package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.Config;
import dev.silvergold.simulatedcoasters.physics.UniversalDragCompensation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class RivetCartDragCompensation {
   private RivetCartDragCompensation() {
   }

   public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
      double fraction = Config.universalDragCompensationFraction();
      if (!(fraction <= 0.0)) {
         SubLevelPhysicsSystem physics = event.getPhysicsSystem();
         ServerLevel level = physics.getLevel();
         SubLevelContainer raw = SubLevelContainer.getContainer(level);
         if (raw instanceof ServerSubLevelContainer container) {
            double dt = event.getTimeStep();

            for (ServerSubLevel sub : container.getAllSubLevels()) {
               if (!sub.isRemoved() && RivetHostKey.isRivetSubLevel(sub)) {
                  RivetBlockEntity be = rivetBlockEntity(sub);
                  if (be != null && be.compensatesCartHostUniversalDrag()) {
                     UniversalDragCompensation.applyPartial(level, physics, sub, dt, fraction);
                  }
               }
            }
         }
      }
   }

   @Nullable
   private static RivetBlockEntity rivetBlockEntity(ServerSubLevel rivetSub) {
      EmbeddedPlotLevelAccessor embedded = rivetSub.getPlot().getEmbeddedLevelAccessor();
      BlockEntity var3 = embedded.getBlockEntity(BlockPos.ZERO);
      return var3 instanceof RivetBlockEntity ? (RivetBlockEntity)var3 : null;
   }
}
