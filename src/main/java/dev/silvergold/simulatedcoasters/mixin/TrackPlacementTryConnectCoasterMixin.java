package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.trains.track.TrackPlacement;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({TrackPlacement.class})
public class TrackPlacementTryConnectCoasterMixin {
   @Shadow
   private static PlacementInfo cached;
   @Shadow
   private static BlockPos hoveringPos;
   @Shadow
   private static boolean hoveringMaxed;
   @Shadow
   private static int hoveringAngle;
   @Shadow
   private static ItemStack lastItem;

   @Inject(
      method = {"tryConnect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void simulatedcoasters$denyCreateTryConnectWhenHoldingCoasterItem(
      Level level,
      Player player,
      BlockPos pos2,
      BlockState state2,
      ItemStack stack,
      boolean girder,
      boolean maximiseTurn,
      CallbackInfoReturnable<PlacementInfo> cir
   ) {
      if (stack.getItem() instanceof CoasterTrackItem) {
         Vec3 lookVec = player.getLookAngle();
         int lookAngle = (int)(22.5 + (double)(AngleHelper.deg(Mth.atan2(lookVec.z, lookVec.x)) % 360.0F)) / 8;
         if (level.isClientSide()
            && cached != null
            && pos2.equals(hoveringPos)
            && stack.equals(lastItem)
            && hoveringMaxed == maximiseTurn
            && lookAngle == hoveringAngle) {
            cir.setReturnValue(cached);
         } else {
            PlacementInfo deny = blocked(stack);
            assignClientCache(level, pos2, stack, maximiseTurn, lookAngle, deny);
            cir.setReturnValue(deny);
         }
      }
   }

   private static void assignClientCache(Level level, BlockPos pos2, ItemStack stack, boolean maximiseTurn, int lookAngle, PlacementInfo deny) {
      if (level.isClientSide()) {
         cached = deny;
         hoveringMaxed = maximiseTurn;
         hoveringAngle = lookAngle;
         hoveringPos = pos2;
         lastItem = stack;
      }
   }

   private static PlacementInfo blocked(ItemStack stack) {
      return new PlacementInfo(CoasterTrackMaterials.COASTER).tooJumbly();
   }
}
