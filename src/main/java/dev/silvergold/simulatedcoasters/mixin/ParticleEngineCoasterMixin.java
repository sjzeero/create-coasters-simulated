package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.AllBlocks;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ParticleEngine.class})
public abstract class ParticleEngineCoasterMixin {
   @Shadow
   protected ClientLevel level;
   @Shadow
   private RandomSource random;

   @Redirect(
      method = {"crack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/shapes/VoxelShape;bounds()Lnet/minecraft/world/phys/AABB;"
      )
   )
   private AABB simulatedcoasters$boundsOrUnitCube(VoxelShape shape) {
      return shape.isEmpty() ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : shape.bounds();
   }

   @Inject(
      method = {"addBlockHitEffects"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$hitParticlesAtRayForManagedFakeTrack(BlockPos pos, BlockHitResult target, CallbackInfo ci) {
      BlockState state = this.level.getBlockState(pos);
      if (AllBlocks.FAKE_TRACK.has(state) && AnchorPeerFakeTracks.isManagedFakeTrack(this.level, pos)) {
         if (state.getRenderShape() != RenderShape.INVISIBLE && state.shouldSpawnTerrainParticles()) {
            ci.cancel();
            Vec3 loc = target.getLocation();
            double spread = 0.14;
            double d0 = loc.x + (this.random.nextDouble() - 0.5) * spread;
            double d1 = loc.y + (this.random.nextDouble() - 0.5) * spread;
            double d2 = loc.z + (this.random.nextDouble() - 0.5) * spread;
            BlockState particleState = AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState();
            ParticleEngine self = (ParticleEngine)(Object)this;
            self.add(new TerrainParticle(this.level, d0, d1, d2, 0.0, 0.0, 0.0, particleState, pos).updateSprite(particleState, pos).setPower(0.2F).scale(0.6F));
         }
      }
   }
}
