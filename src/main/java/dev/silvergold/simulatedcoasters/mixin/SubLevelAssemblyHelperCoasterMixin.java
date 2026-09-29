package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonAssembly;
import dev.silvergold.simulatedcoasters.rivet.RivetAssembly;
import dev.silvergold.simulatedcoasters.rivet.RivetAssemblyMoveContext;
import dev.silvergold.simulatedcoasters.rivet.RivetDisassemblyEnforcer;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.assembly.CoasterBezierAssemblyRotationFix;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.glue.CoasterGlueAssemblyRailSplitter;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SubLevelAssemblyHelper.class})
public class SubLevelAssemblyHelperCoasterMixin {
   private static final ThreadLocal<UUID> RIVET_MOVE_SOURCE_SUBLEVEL = new ThreadLocal<>();

   @Inject(
      method = {"assembleBlocks"},
      at = {@At(
         value = "INVOKE",
         target = "Ldev/ryanhcode/sable/api/SubLevelAssemblyHelper;moveOtherStuff(Lnet/minecraft/server/level/ServerLevel;Ldev/ryanhcode/sable/api/SubLevelAssemblyHelper$AssemblyTransform;Ljava/lang/Iterable;Ldev/ryanhcode/sable/companion/math/BoundingBox3ic;)V",
         shift = Shift.BEFORE
      )}
   )
   private static void simulatedcoasters$splitCoasterRailsCrossingGlueBeforeMove(
      ServerLevel level, BlockPos anchor, Iterable<BlockPos> blocks, BoundingBox3ic bounds, CallbackInfoReturnable<ServerSubLevel> cir
   ) {
      CoasterGlueAssemblyRailSplitter.splitRailsSpanningGlueBoundary(level, blocks);
   }

   @Inject(
      method = {"moveBlocks"},
      at = {@At("HEAD")}
   )
   private static void simulatedcoasters$beginRivetAssemblyMove(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
      RivetAssemblyMoveContext.begin();
      UUID sourceSubLevelId = null;

      for (BlockPos src : blocks) {
         if (src != null) {
            SubLevel sub = Sable.HELPER.getContaining(level, src);
            if (sub != null) {
               sourceSubLevelId = sub.getUniqueId();
               break;
            }
         }
      }

      RIVET_MOVE_SOURCE_SUBLEVEL.set(sourceSubLevelId);
   }

   @Inject(
      method = {"moveBlocks"},
      at = {@At("HEAD")}
   )
   private static void simulatedcoasters$detachChainLiftKineticsBeforeMove(
      ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci
   ) {
      for (BlockPos src : blocks) {
         if (src != null && level.getBlockEntity(src) instanceof CoasterAnchorpointBlockEntity anchor) {
            anchor.prepareKineticForAssemblyMove();
         }
      }
   }

   @Inject(
      method = {"moveBlocks"},
      at = {@At("TAIL")}
   )
   private static void simulatedcoasters$breakWorldGridCartBlocksAfterMove(
      ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci
   ) {
      CoasterCartResolve.breakCartBlocksAfterAssemblyMove(level, transform, blocks);
   }

   @Inject(
      method = {"moveBlocks"},
      at = {@At("TAIL")}
   )
   private static void simulatedcoasters$rebindRivetsAfterMove(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
      try {
         RivetAssembly.onHostBlocksMoved(level, transform, blocks, RIVET_MOVE_SOURCE_SUBLEVEL.get());
         BalloonAssembly.onHostBlocksMoved(level, transform, blocks, RIVET_MOVE_SOURCE_SUBLEVEL.get());
         RivetDisassemblyEnforcer.cleanupRivetBlocksAfterMove(level, transform, blocks);
      } finally {
         RIVET_MOVE_SOURCE_SUBLEVEL.remove();
         RivetAssemblyMoveContext.end();
      }
   }

   @Inject(
      method = {"moveBlocks"},
      at = {@At("TAIL")}
   )
   private static void simulatedcoasters$fixCoasterBeziersAfterMove(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
      CoasterBezierAssemblyRotationFix.afterMoveBlocks(level, transform, blocks);
   }
}
