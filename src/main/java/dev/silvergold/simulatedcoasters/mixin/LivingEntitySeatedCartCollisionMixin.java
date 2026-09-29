package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision.CollisionInfo;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision.FirstCollisionInfo;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartSeatedEntityCollision;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public abstract class LivingEntitySeatedCartCollisionMixin {
   @WrapOperation(
      method = {"sable$beforeAnimation"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;sable$computeCollisionEffects(Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision$CollisionInfo;Ldev/ryanhcode/sable/sublevel/SubLevel;Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision$FirstCollisionInfo;)V"
      )},
      remap = false
   )
   private void simulatedcoasters$skipLinkedCartCollisionEffects(
      LivingEntity self, CollisionInfo info, SubLevel subLevel, FirstCollisionInfo collisionInfo, Operation<Void> original
   ) {
      if (!CoasterCartSeatedEntityCollision.shouldSkipEntityCartCollision(self, subLevel)) {
         original.call(new Object[]{self, info, subLevel, collisionInfo});
      }
   }
}
