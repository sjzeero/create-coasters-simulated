package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.impl.SubLevelEntityCollisionContext;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SubLevelEntityCollision.class})
public class SubLevelEntityCollisionRivetBlockMixin {
   @Inject(
      method = {"getSubLevelEntityCollisionShape"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void simulatedcoasters$rivetBlockEntityCollisionShape(
      Entity entity,
      Vector3dc boundsCenter,
      Pose3dc subLevelPose,
      BlockState state,
      LevelAccelerator level,
      BlockPos pos,
      LevelReusedVectors sink,
      CallbackInfoReturnable<VoxelShape> cir
   ) {
      if (state.is((Block)SimulatedCoastersBlocks.RIVET.get())) {
         cir.setReturnValue(state.getCollisionShape(level, pos, new SubLevelEntityCollisionContext(entity)));
      }
   }
}
