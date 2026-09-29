/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.math.LevelReusedVectors
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.physics.impl.SubLevelEntityCollisionContext
 *  dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision
 *  dev.ryanhcode.sable.util.LevelAccelerator
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.joml.Vector3dc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.impl.SubLevelEntityCollisionContext;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={SubLevelEntityCollision.class})
public class SubLevelEntityCollisionRivetBlockMixin {
    @Inject(method={"getSubLevelEntityCollisionShape"}, at={@At(value="HEAD")}, cancellable=true)
    private static void simulatedcoasters$rivetBlockEntityCollisionShape(Entity entity, Vector3dc boundsCenter, Pose3dc subLevelPose, BlockState state, LevelAccelerator level, BlockPos pos, LevelReusedVectors sink, CallbackInfoReturnable<VoxelShape> cir) {
        if (state.is((Block)SimulatedCoastersBlocks.RIVET.get())) {
            cir.setReturnValue((Object)state.getCollisionShape((BlockGetter)level, pos, (CollisionContext)new SubLevelEntityCollisionContext(entity)));
        }
    }
}

