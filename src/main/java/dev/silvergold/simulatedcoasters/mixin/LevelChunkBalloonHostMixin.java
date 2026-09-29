/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import dev.silvergold.simulatedcoasters.balloon.BalloonHostIndex;
import dev.silvergold.simulatedcoasters.balloon.BalloonIntegrity;
import dev.silvergold.simulatedcoasters.rivet.RivetAssemblyMoveContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={LevelChunk.class})
public class LevelChunkBalloonHostMixin {
    @Shadow
    @Final
    Level level;

    @Inject(method={"setBlockState"}, at={@At(value="RETURN")})
    private void simulatedcoasters$balloonPlotBlockChanged(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        Level level;
        if (this.level.isClientSide() || !((level = this.level) instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (RivetAssemblyMoveContext.isActive()) {
            return;
        }
        BlockState previous = (BlockState)cir.getReturnValue();
        if (previous == null) {
            return;
        }
        if (!previous.isAir() && state.isAir()) {
            BalloonHostIndex.get(serverLevel).breakBalloonsOnHostCell(serverLevel, pos);
        }
        if (!state.isAir() && previous != state) {
            BalloonIntegrity.onPlotBlockChanged(serverLevel, pos, state);
        }
    }
}

