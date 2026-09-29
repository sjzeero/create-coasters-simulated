package dev.silvergold.simulatedcoasters.mixin.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   targets = {"dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity"},
   remap = false
)
public abstract class MergingGlueBlockEntityRivetMixin {
   private static final int MERGE_DURATION = 10;
   private static final int MERGE_WAIT = 2;
   @Shadow
   private Rotation endRotation;
   @Shadow
   private BlockPos partnerPosition;
   @Shadow
   private boolean isController;
   @Shadow
   private int ageTicks;

   @Shadow
   private void breakGlue() {
   }

   @Shadow
   private void disassembleToPartner(Rotation rotation) {
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Ldev/ryanhcode/sable/sublevel/ServerSubLevel;getMassTracker()Ldev/ryanhcode/sable/api/physics/mass/MassData;",
         ordinal = 0
      )},
      cancellable = true,
      require = 0
   )
   private void simulatedcoasters$mergeForeignSubLevelIntoRivet(CallbackInfo ci) {
      BlockEntity self = (BlockEntity)(Object)this;
      Level level = self.getLevel();
      if (level != null && !level.isClientSide()) {
         if (this.isController && this.ageTicks > 12 && this.partnerPosition != null) {
            SubLevel selfSub = Sable.HELPER.getContaining(level, self.getBlockPos());
            SubLevel partnerSub = Sable.HELPER.getContaining(level, this.partnerPosition);
            boolean selfIsRivet = RivetHostKey.isRivetSubLevel(selfSub);
            boolean partnerIsRivet = RivetHostKey.isRivetSubLevel(partnerSub);
            if (selfIsRivet != partnerIsRivet) {
               BlockEntity partnerBe = level.getBlockEntity(this.partnerPosition);
               if (partnerBe != null && partnerBe != self && !partnerBe.isRemoved()) {
                  this.breakGlue();
                  if (partnerIsRivet) {
                     this.disassembleToPartner(this.endRotation);
                  } else {
                     ((MergingGlueBlockEntityRivetMixin)(Object)partnerBe).disassembleToPartner(this.endRotation);
                  }

                  ci.cancel();
               }
            }
         }
      }
   }
}
