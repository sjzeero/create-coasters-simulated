/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision$CollisionInfo
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartSeatedEntityCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Entity.class})
public class EntitySubLevelCollisionCartPostMixin {
    @Inject(method={"sable$collideRedirect"}, at={@At(value="INVOKE", target="Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision;collide(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ldev/ryanhcode/sable/api/math/LevelReusedVectors;)Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision$CollisionInfo;", shift=At.Shift.AFTER)}, remap=false)
    private void simulatedcoasters$stripLinkedCartCollisionResults(Vec3 collisionMotion, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity)this;
        SubLevelEntityCollision.CollisionInfo info = ((EntityMovementExtension)self).sable$getCollisionInfo();
        if (info == null || info.firstCollisions == null) {
            return;
        }
        info.firstCollisions.entrySet().removeIf(e -> CoasterCartSeatedEntityCollision.shouldSkipEntityCartCollision(self, (SubLevel)e.getKey()));
        if (info.firstCollisions.isEmpty()) {
            info.subLevelHorizontalCollision = false;
        }
    }
}

