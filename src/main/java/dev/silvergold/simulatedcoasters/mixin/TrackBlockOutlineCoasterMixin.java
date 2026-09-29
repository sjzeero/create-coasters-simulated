package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartPlacementPreviewRenderer;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveOutlineSupport;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurvePick;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveDyeOutlineClient;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveSelectionOutlineRenderer;
import dev.silvergold.simulatedcoasters.client.track.TrackOutlineBezierAccess;
import dev.silvergold.simulatedcoasters.track.CoasterCurvePickGeometry;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurvePickContext;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TrackBlockOutline.class})
public class TrackBlockOutlineCoasterMixin {
   @Inject(
      method = {"pickCurves"},
      at = {@At("HEAD")}
   )
   private static void simulatedcoasters$pickCurvesEnter(CallbackInfo ci) {
      AnchorPeerCurveHit.clear();
      CoasterTrackCurvePickContext.enterPickCurves();
   }

   @Inject(
      method = {"pickCurves"},
      at = {@At("RETURN")}
   )
   private static void simulatedcoasters$pickCurvesExit(CallbackInfo ci) {
      AnchorPeerCurvePick.refineAfterCreatePass(Minecraft.getInstance());
      CoasterTrackCurvePickContext.exitPickCurves();
   }

   @WrapOperation(
      method = {"pickCurves"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/trains/track/BezierConnection;getNormal(D)Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private static Vec3 simulatedcoasters$stashTForCurvePickAnchor(
      BezierConnection bc, double t, Operation<Vec3> original, @Local(name = {"be"}) TrackBlockEntity be
   ) {
      CoasterTrackCurvePickContext.stashPickCurvesBezierSample(bc, be, t);
      return (Vec3)original.call(new Object[]{bc, t});
   }

   @WrapOperation(
      method = {"pickCurves"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
         ordinal = 0
      )}
   )
   private static Vec3 simulatedcoasters$liftCurvePickAnchor(Vec3 a, Vec3 b, Operation<Vec3> original) {
      Vec3 mid = (Vec3)original.call(new Object[]{a, b});
      return CoasterTrackCurvePickContext.curvePickSpineMidToBakedRailMid(mid);
   }

   @Redirect(
      method = {"pickCurves"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/math/VoxelShaper;get(Lnet/minecraft/core/Direction;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
         ordinal = 0
      )
   )
   private static VoxelShape simulatedcoasters$curvePickUsesCoasterProfile(VoxelShaper shaper, Direction direction) {
      return shaper.get(direction);
   }

   @Redirect(
      method = {"drawCurveSelection"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/math/VoxelShaper;get(Lnet/minecraft/core/Direction;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
         ordinal = 0
      )
   )
   private static VoxelShape simulatedcoasters$curveDrawUsesCoasterProfile(VoxelShaper shaper, Direction direction) {
      BezierPointSelection result = TrackBlockOutline.result;
      if (result != null) {
         BezierConnection bc = TrackOutlineBezierAccess.primaryBezierForSelection(result);
         if (bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
            return CoasterCurvePickGeometry.curvePickCrossSection();
         }
      }

      return shaper.get(direction);
   }

   @Inject(
      method = {"drawCurveSelection"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void simulatedcoasters$drawCoasterCurveSelection(PoseStack ms, MultiBufferSource buffer, Vec3 camera, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.options.hideGui && mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         if (!CoasterCartPlacementPreviewRenderer.suppressDefaultCurveHighlight(mc)) {
            if (mc.level != null) {
               BezierConnection bc = CoasterCurveDyeOutlineClient.resolvePreviewHostedPrimary(mc);
               if (bc == null) {
                  if (AnchorPeerCurveHit.isActive()) {
                     AnchorPeerCurveHit.clear();
                  }
               } else {
                  BlockPos sublevelAnchor = CoasterCurveDyeOutlineClient.resolvePreviewSublevelAnchor(mc, bc);
                  if (AnchorPeerCurveOutlineSupport.hasSaneOutlineBounds(bc)) {
                     float pt = AnimationTickHolder.getPartialTicks(mc.level);
                     float outlineR = 0.0F;
                     float outlineG = 0.0F;
                     float outlineB = 0.0F;
                     float outlineA = 0.4F;
                     Integer dyeRgb = CoasterCurveDyeOutlineClient.heldDyeDiffuseRgb(mc);
                     if (dyeRgb != null) {
                        outlineR = (float)(dyeRgb >> 16 & 0xFF) / 255.0F;
                        outlineG = (float)(dyeRgb >> 8 & 0xFF) / 255.0F;
                        outlineB = (float)(dyeRgb & 0xFF) / 255.0F;
                        outlineA = 0.85F;
                     }

                     CoasterCurveSelectionOutlineRenderer.render(ms, buffer, camera, mc.level, bc, sublevelAnchor, pt, outlineR, outlineG, outlineB, outlineA);
                     ci.cancel();
                  }
               }
            }
         }
      }
   }
}
