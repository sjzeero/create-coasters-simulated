/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackBlockEntity
 *  com.simibubi.create.content.trains.track.TrackBlockOutline
 *  com.simibubi.create.content.trains.track.TrackBlockOutline$BezierPointSelection
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.createmod.catnip.math.VoxelShaper
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={TrackBlockOutline.class})
public class TrackBlockOutlineCoasterMixin {
    @Inject(method={"pickCurves"}, at={@At(value="HEAD")})
    private static void simulatedcoasters$pickCurvesEnter(CallbackInfo ci) {
        AnchorPeerCurveHit.clear();
        CoasterTrackCurvePickContext.enterPickCurves();
    }

    @Inject(method={"pickCurves"}, at={@At(value="RETURN")})
    private static void simulatedcoasters$pickCurvesExit(CallbackInfo ci) {
        AnchorPeerCurvePick.refineAfterCreatePass(Minecraft.getInstance());
        CoasterTrackCurvePickContext.exitPickCurves();
    }

    @WrapOperation(method={"pickCurves"}, at={@At(value="INVOKE", target="Lcom/simibubi/create/content/trains/track/BezierConnection;getNormal(D)Lnet/minecraft/world/phys/Vec3;")})
    private static Vec3 simulatedcoasters$stashTForCurvePickAnchor(BezierConnection bc, double t, Operation<Vec3> original, @Local(name={"be"}) TrackBlockEntity be) {
        CoasterTrackCurvePickContext.stashPickCurvesBezierSample(bc, be, t);
        return (Vec3)original.call(new Object[]{bc, t});
    }

    @WrapOperation(method={"pickCurves"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", ordinal=0)})
    private static Vec3 simulatedcoasters$liftCurvePickAnchor(Vec3 a, Vec3 b, Operation<Vec3> original) {
        Vec3 mid = (Vec3)original.call(new Object[]{a, b});
        return CoasterTrackCurvePickContext.curvePickSpineMidToBakedRailMid(mid);
    }

    @Redirect(method={"pickCurves"}, at=@At(value="INVOKE", target="Lnet/createmod/catnip/math/VoxelShaper;get(Lnet/minecraft/core/Direction;)Lnet/minecraft/world/phys/shapes/VoxelShape;", ordinal=0))
    private static VoxelShape simulatedcoasters$curvePickUsesCoasterProfile(VoxelShaper shaper, Direction direction) {
        return shaper.get(direction);
    }

    @Redirect(method={"drawCurveSelection"}, at=@At(value="INVOKE", target="Lnet/createmod/catnip/math/VoxelShaper;get(Lnet/minecraft/core/Direction;)Lnet/minecraft/world/phys/shapes/VoxelShape;", ordinal=0))
    private static VoxelShape simulatedcoasters$curveDrawUsesCoasterProfile(VoxelShaper shaper, Direction direction) {
        BezierConnection bc;
        TrackBlockOutline.BezierPointSelection result = TrackBlockOutline.result;
        if (result != null && (bc = TrackOutlineBezierAccess.primaryBezierForSelection(result)) != null && bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return CoasterCurvePickGeometry.curvePickCrossSection();
        }
        return shaper.get(direction);
    }

    @Inject(method={"drawCurveSelection"}, at={@At(value="HEAD")}, cancellable=true)
    private static void simulatedcoasters$drawCoasterCurveSelection(PoseStack ms, MultiBufferSource buffer, Vec3 camera, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        if (CoasterCartPlacementPreviewRenderer.suppressDefaultCurveHighlight(mc)) {
            return;
        }
        if (mc.level == null) {
            return;
        }
        BezierConnection bc = CoasterCurveDyeOutlineClient.resolvePreviewHostedPrimary(mc);
        if (bc == null) {
            if (AnchorPeerCurveHit.isActive()) {
                AnchorPeerCurveHit.clear();
            }
            return;
        }
        @Nullable BlockPos sublevelAnchor = CoasterCurveDyeOutlineClient.resolvePreviewSublevelAnchor(mc, bc);
        if (!AnchorPeerCurveOutlineSupport.hasSaneOutlineBounds(bc)) {
            return;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
        float outlineR = 0.0f;
        float outlineG = 0.0f;
        float outlineB = 0.0f;
        float outlineA = 0.4f;
        @Nullable Integer dyeRgb = CoasterCurveDyeOutlineClient.heldDyeDiffuseRgb(mc);
        if (dyeRgb != null) {
            outlineR = (float)(dyeRgb >> 16 & 0xFF) / 255.0f;
            outlineG = (float)(dyeRgb >> 8 & 0xFF) / 255.0f;
            outlineB = (float)(dyeRgb & 0xFF) / 255.0f;
            outlineA = 0.85f;
        }
        CoasterCurveSelectionOutlineRenderer.render(ms, buffer, camera, (Level)mc.level, bc, sublevelAnchor, pt, outlineR, outlineG, outlineB, outlineA);
        ci.cancel();
    }
}

