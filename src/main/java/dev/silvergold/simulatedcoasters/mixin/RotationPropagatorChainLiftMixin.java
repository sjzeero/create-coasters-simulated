/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.kinetics.RotationPropagator
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftKineticConflict;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={RotationPropagator.class})
public class RotationPropagatorChainLiftMixin {
    @Redirect(method={"propagateNewSource"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private static boolean simulatedcoasters$protectChainLiftAnchor(Level level, BlockPos pos, boolean drop) {
        return ChainLiftKineticConflict.redirectDestroyAwayFromAnchor(level, pos, drop);
    }
}

