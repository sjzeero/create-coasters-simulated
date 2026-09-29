/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision$CollisionInfo
 *  dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision$FirstCollisionInfo
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartSeatedEntityCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={LivingEntity.class})
public abstract class LivingEntitySeatedCartCollisionMixin {
    @WrapOperation(method={"sable$beforeAnimation"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;sable$computeCollisionEffects(Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision$CollisionInfo;Ldev/ryanhcode/sable/sublevel/SubLevel;Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision$FirstCollisionInfo;)V")}, remap=false)
    private void simulatedcoasters$skipLinkedCartCollisionEffects(LivingEntity self, SubLevelEntityCollision.CollisionInfo info, SubLevel subLevel, SubLevelEntityCollision.FirstCollisionInfo collisionInfo, Operation<Void> original) {
        if (CoasterCartSeatedEntityCollision.shouldSkipEntityCartCollision((Entity)self, subLevel)) {
            return;
        }
        original.call(new Object[]{self, info, subLevel, collisionInfo});
    }
}

