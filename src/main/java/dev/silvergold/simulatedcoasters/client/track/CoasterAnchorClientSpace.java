/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix3f
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4d
 *  org.joml.Matrix4dc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3d
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterAnchorClientSpace {
    private CoasterAnchorClientSpace() {
    }

    @Nullable
    public static Pose3dc renderPoseForAnchor(Level level, BlockPos anchorHost, float partialTick) {
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)anchorHost);
        if (sub == null) {
            return null;
        }
        if (sub instanceof ClientSubLevel) {
            ClientSubLevel clientSub = (ClientSubLevel)sub;
            return clientSub.renderPose(partialTick);
        }
        return sub.logicalPose();
    }

    public static Vec3 toRenderWorld(Level level, BlockPos anchorHost, Vec3 plotLocal) {
        return CoasterAnchorClientSpace.toRenderWorld(CoasterAnchorClientSpace.renderPoseForAnchor(level, anchorHost, CoasterAnchorClientSpace.currentPartialTick(level)), plotLocal);
    }

    public static Vec3 toRenderWorld(Level level, BlockPos anchorHost, Vec3 plotLocal, float partialTick) {
        return CoasterAnchorClientSpace.toRenderWorld(CoasterAnchorClientSpace.renderPoseForAnchor(level, anchorHost, partialTick), plotLocal);
    }

    public static Vec3 toRenderWorld(@Nullable Pose3dc renderPose, Vec3 plotLocal) {
        return renderPose == null ? plotLocal : renderPose.transformPosition(plotLocal);
    }

    public static Vec3 toPlotLocal(Level level, BlockPos anchorHost, Vec3 renderWorld) {
        return CoasterAnchorClientSpace.toPlotLocal(CoasterAnchorClientSpace.renderPoseForAnchor(level, anchorHost, CoasterAnchorClientSpace.currentPartialTick(level)), renderWorld);
    }

    public static Vec3 toPlotLocal(Level level, BlockPos anchorHost, Vec3 renderWorld, float partialTick) {
        return CoasterAnchorClientSpace.toPlotLocal(CoasterAnchorClientSpace.renderPoseForAnchor(level, anchorHost, partialTick), renderWorld);
    }

    public static Vec3 toPlotLocal(@Nullable Pose3dc renderPose, Vec3 renderWorld) {
        return renderPose == null ? renderWorld : renderPose.transformPositionInverse(renderWorld);
    }

    public static Vec3 toRenderDirection(Level level, BlockPos anchorHost, Vec3 plotLocalDir) {
        return CoasterAnchorClientSpace.toRenderDirection(CoasterAnchorClientSpace.renderPoseForAnchor(level, anchorHost, CoasterAnchorClientSpace.currentPartialTick(level)), plotLocalDir);
    }

    public static Vec3 toRenderDirection(Level level, BlockPos anchorHost, Vec3 plotLocalDir, float partialTick) {
        return CoasterAnchorClientSpace.toRenderDirection(CoasterAnchorClientSpace.renderPoseForAnchor(level, anchorHost, partialTick), plotLocalDir);
    }

    public static Vec3 toRenderDirection(@Nullable Pose3dc renderPose, Vec3 plotLocalDir) {
        if (renderPose == null || plotLocalDir.lengthSqr() < 1.0E-12) {
            return plotLocalDir;
        }
        Vector3d v = new Vector3d(plotLocalDir.x, plotLocalDir.y, plotLocalDir.z);
        renderPose.orientation().transform(v);
        return new Vec3(v.x, v.y, v.z);
    }

    public static void premultiplyRenderPose(PoseStack poseStack, Pose3dc renderPose) {
        Matrix4d baked = new Matrix4d();
        renderPose.bakeIntoMatrix(baked);
        Matrix4f renderF = new Matrix4f((Matrix4dc)baked);
        PoseStack.Pose entry = poseStack.last();
        Matrix4f world = new Matrix4f((Matrix4fc)renderF).mul((Matrix4fc)entry.pose());
        entry.pose().set((Matrix4fc)world);
        Matrix3f rot = new Matrix3f((Matrix4fc)renderF);
        rot.mul((Matrix3fc)entry.normal());
        entry.normal().set((Matrix3fc)rot);
    }

    public static Vec3 toPlotLocalDirection(Level level, BlockPos anchorHost, Vec3 renderWorldDir) {
        if (renderWorldDir.lengthSqr() < 1.0E-12) {
            return renderWorldDir;
        }
        Vec3 refWorld = CoasterAnchorClientSpace.toRenderWorld(level, anchorHost, CoasterAnchorpointBlockEntity.worldCenter(level, anchorHost));
        Vec3 plotBase = CoasterAnchorClientSpace.toPlotLocal(level, anchorHost, refWorld);
        Vec3 plotTip = CoasterAnchorClientSpace.toPlotLocal(level, anchorHost, refWorld.add(renderWorldDir));
        return plotTip.subtract(plotBase);
    }

    public static Vec3 anchorCenterRenderWorld(Level level, BlockPos anchorPos) {
        return CoasterAnchorClientSpace.toRenderWorld(level, anchorPos, CoasterAnchorpointBlockEntity.worldCenter(level, anchorPos));
    }

    public static Vec3 anchorCenterRenderWorld(Level level, BlockPos anchorPos, float partialTick) {
        return CoasterAnchorClientSpace.toRenderWorld(level, anchorPos, CoasterAnchorpointBlockEntity.worldCenter(level, anchorPos), partialTick);
    }

    public static Vec3 anchorShaftHostRenderWorld(Level level, BlockPos anchorPos, float partialTick) {
        return CoasterAnchorClientSpace.toRenderWorld(level, anchorPos, Vec3.atCenterOf((Vec3i)anchorPos), partialTick);
    }

    public static Vec3 anchorShaftHostRenderWorld(Level level, BlockPos anchorPos) {
        return CoasterAnchorClientSpace.anchorShaftHostRenderWorld(level, anchorPos, CoasterAnchorClientSpace.currentPartialTick(level));
    }

    public static boolean anchorHostsWithinPreviewSpan(Level level, BlockPos anchorA, BlockPos anchorB) {
        return CoasterAnchorClientSpace.anchorCenterRenderWorld(level, anchorA).distanceToSqr(CoasterAnchorClientSpace.anchorCenterRenderWorld(level, anchorB)) <= (double)CoasterTrackGauge.maxCoasterCurveEndpointSpanBlocksSq();
    }

    public static Vec3 pathCenterlineRenderWorld(Level level, BlockPos anchorHost, Vec3 plotLocal) {
        return CoasterAnchorClientSpace.toRenderWorld(level, anchorHost, CoasterPathGraphManager.pathCenterlineWorld(plotLocal));
    }

    public static Vec3 pathCenterlineRenderWorld(Level level, BlockPos anchorHost, Vec3 plotLocal, float partialTick) {
        return CoasterAnchorClientSpace.toRenderWorld(level, anchorHost, CoasterPathGraphManager.pathCenterlineWorld(plotLocal), partialTick);
    }

    private static float currentPartialTick(Level level) {
        if (level.isClientSide()) {
            return AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        }
        return 0.0f;
    }
}

