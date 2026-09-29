package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision.CollisionInfo;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartSeatedEntityCollision;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SubLevelEntityCollision.class})
public class SubLevelEntityCollisionCartLinkMixin {
   @Inject(
      method = {"collide"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/lang/Math;min(II)I",
         ordinal = 0
      )}
   )
   private static void simulatedcoasters$filterLinkedNeighborCarts(
      Entity entity,
      Vec3 collisionMotionMoj,
      Vec3 velocityMotionMoj,
      LevelReusedVectors sink,
      CallbackInfoReturnable<CollisionInfo> cir,
      @Local ObjectSet<SubLevel> intersecting
   ) {
      CoasterCartSeatedEntityCollision.pruneLinkedNeighborCartSubLevels(entity, intersecting);
   }

   @ModifyExpressionValue(
      method = {"collide"},
      at = {@At(
         value = "INVOKE",
         target = "Ldev/ryanhcode/sable/ActiveSableCompanion;getVehicleSubLevel(Lnet/minecraft/world/entity/Entity;)Ldev/ryanhcode/sable/sublevel/SubLevel;",
         remap = false
      )}
   )
   private static SubLevel simulatedcoasters$expandVehicleSubLevelSkip(
      SubLevel vehicleSubLevel, Entity entity, Vec3 collisionMotionMoj, Vec3 velocityMotionMoj, LevelReusedVectors sink, @Local SubLevel subLevel
   ) {
      return CoasterCartSeatedEntityCollision.shouldSkipEntityCartCollision(entity, subLevel) ? subLevel : vehicleSubLevel;
   }
}
