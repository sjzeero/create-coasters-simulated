/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.llamalad7.mixinextras.sugar.Local
 *  dev.ryanhcode.sable.api.math.LevelReusedVectors
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision
 *  dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision$CollisionInfo
 *  it.unimi.dsi.fastutil.objects.ObjectSet
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartSeatedEntityCollision;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={SubLevelEntityCollision.class})
public class SubLevelEntityCollisionCartLinkMixin {
    @Inject(method={"collide"}, at={@At(value="INVOKE", target="Ljava/lang/Math;min(II)I", ordinal=0)})
    private static void simulatedcoasters$filterLinkedNeighborCarts(Entity entity, Vec3 collisionMotionMoj, Vec3 velocityMotionMoj, LevelReusedVectors sink, CallbackInfoReturnable<SubLevelEntityCollision.CollisionInfo> cir, @Local ObjectSet<SubLevel> intersecting) {
        CoasterCartSeatedEntityCollision.pruneLinkedNeighborCartSubLevels(entity, intersecting);
    }

    @ModifyExpressionValue(method={"collide"}, at={@At(value="INVOKE", target="Ldev/ryanhcode/sable/ActiveSableCompanion;getVehicleSubLevel(Lnet/minecraft/world/entity/Entity;)Ldev/ryanhcode/sable/sublevel/SubLevel;", remap=false)})
    private static SubLevel simulatedcoasters$expandVehicleSubLevelSkip(SubLevel vehicleSubLevel, Entity entity, Vec3 collisionMotionMoj, Vec3 velocityMotionMoj, LevelReusedVectors sink, @Local SubLevel subLevel) {
        if (CoasterCartSeatedEntityCollision.shouldSkipEntityCartCollision(entity, subLevel)) {
            return subLevel;
        }
        return vehicleSubLevel;
    }
}

