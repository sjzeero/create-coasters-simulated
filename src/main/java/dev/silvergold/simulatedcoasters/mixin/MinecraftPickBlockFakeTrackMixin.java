package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.client.track.TrackOutlineBezierAccess;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public class MinecraftPickBlockFakeTrackMixin {
   @WrapOperation(
      method = {"pickBlock"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;"
      )}
   )
   private ItemStack simulatedcoasters$managedFakeTrackPickCoasterTrack(
      BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player, Operation<ItemStack> original
   ) {
      if (AnchorPeerCurveHit.isActive()) {
         BezierConnection bc = AnchorPeerCurveHit.curve();
         if (bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
            return new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
         }
      }

      BezierPointSelection trackSel = TrackBlockOutline.result;
      if (trackSel != null) {
         BezierConnection bc = TrackOutlineBezierAccess.primaryBezierForSelection(trackSel);
         if (bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
            return new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
         }
      }

      ItemStack stack = (ItemStack)original.call(new Object[]{state, target, level, pos, player});
      if (level instanceof Level world) {
         return !AnchorPeerFakeTracks.isManagedFakeTrack(world, pos) ? stack : new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
      } else {
         return stack;
      }
   }

   @Inject(
      method = {"addCustomNbtData"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$skipFakeTrackCtrlPickComponents(ItemStack stack, BlockEntity blockEntity, RegistryAccess registryAccess, CallbackInfo ci) {
      Level beLevel = blockEntity.getLevel();
      if (beLevel != null && AnchorPeerFakeTracks.isManagedFakeTrack(beLevel, blockEntity.getBlockPos())) {
         ci.cancel();
      }
   }
}
