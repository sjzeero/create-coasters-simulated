package dev.silvergold.simulatedcoasters.physics;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;

public final class UniversalDragCompensation {
   private UniversalDragCompensation() {
   }

   public static void applyPartial(ServerLevel level, SubLevelPhysicsSystem physics, ServerSubLevel sub, double dt, double fraction) {
      if (!(dt <= 0.0) && !(fraction <= 0.0)) {
         double c = DimensionPhysicsData.getUniversalDrag(level);
         if (!(c <= 0.0)) {
            double factor = fraction * c * dt;
            if (!(factor <= 0.0)) {
               PhysicsPipeline pipeline = physics.getPipeline();
               Vector3d v = pipeline.getLinearVelocity(sub, new Vector3d());
               Vector3d w = pipeline.getAngularVelocity(sub, new Vector3d());
               Vector3d dv = new Vector3d(v).mul(factor);
               Vector3d dw = new Vector3d(w).mul(factor);
               if (!(dv.lengthSquared() < 1.0E-22) || !(dw.lengthSquared() < 1.0E-22)) {
                  pipeline.addLinearAndAngularVelocity(sub, dv, dw);
               }
            }
         }
      }
   }
}
