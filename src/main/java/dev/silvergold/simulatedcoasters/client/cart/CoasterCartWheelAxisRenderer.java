/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.SubLevelAccess
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.client.Camera
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.block.ModelBlockRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.client.resources.model.ModelResourceLocation
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.client.event.ModelEvent$ModifyBakingResult
 *  net.neoforged.neoforge.client.event.ModelEvent$RegisterAdditional
 *  net.neoforged.neoforge.client.model.data.ModelData
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaternionf
 */
package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientFrameCache;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartWorldRenderer;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartWheelAxisRenderer {
    public static final ModelResourceLocation WHEEL_AXIS_MRL = ModelResourceLocation.standalone((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/coaster_cart_wheel_axis"));
    public static float ENGAGED_AXLE_BASE_BODY_X = 0.0f;
    public static float ENGAGED_AXLE_BASE_BODY_Y = 0.0f;
    public static float ENGAGED_AXLE_BASE_BODY_Z = 0.0f;
    public static float ENGAGED_FRONT_AXLE_BODY_X = 0.0f;
    public static float ENGAGED_FRONT_AXLE_BODY_Y = 0.1f;
    public static float ENGAGED_FRONT_AXLE_BODY_Z = -0.7f;
    public static float ENGAGED_BACK_AXLE_BODY_X = 0.0f;
    public static float ENGAGED_BACK_AXLE_BODY_Y = 0.1f;
    public static float ENGAGED_BACK_AXLE_BODY_Z = 0.7f;
    public static float PLACEHOLDER_PIVOT_X = 0.5f;
    public static float PLACEHOLDER_PIVOT_Y = 0.5f;
    public static float PLACEHOLDER_PIVOT_Z = 0.5f;
    public static float SPIN_RADIANS_PER_BLOCK = 0.0f;
    public static float PLACEHOLDER_WHEEL_RADIUS_BLOCKS = 0.2f;
    public static float SPIN_SIGN = -1.0f;
    public static float LOWER_WHEEL_ROW_BODY_Y_OFFSET = -0.55f;
    public static float LOWER_WHEEL_ROW_SPIN_SIGN = -1.0f;
    private static final double WHEEL_SPIN_MAX_DISTANCE_BLOCKS = 20.0;
    private static final double WHEEL_SPIN_MAX_DIST_SQ = 400.0;
    private static final double WHEEL_DRAW_MAX_DISTANCE_BLOCKS = 48.0;
    private static final double WHEEL_DRAW_MAX_DIST_SQ = 2304.0;
    private static BakedModel wheelAxisModel;
    private static final Map<WheelCellKey, Double> spinRadiansByCell;
    public static float ITEM_INVENTORY_WHEEL_Z_SPREAD;
    public static float ITEM_INVENTORY_WHEEL_X_INSET;
    public static float ITEM_INVENTORY_WHEEL_MESH_SCALE_X;
    public static float ITEM_INVENTORY_WHEEL_MESH_SCALE_Z;

    private CoasterCartWheelAxisRenderer() {
    }

    public static void registerStandaloneModels(ModelEvent.RegisterAdditional event) {
        event.register(WHEEL_AXIS_MRL);
    }

    public static void cacheModels(ModelEvent.ModifyBakingResult event) {
        wheelAxisModel = (BakedModel)event.getModels().get(WHEEL_AXIS_MRL);
    }

    static boolean hasModel() {
        return wheelAxisModel != null;
    }

    public static void renderStandingAt(PoseStack poseStack, MultiBufferSource buffers, BlockState tintState, float cr, float cg, float cb, Level level, BlockPos corner, double camX, double camY, double camZ) {
        if (wheelAxisModel == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ModelBlockRenderer modelRenderer = mc.getBlockRenderer().getModelRenderer();
        RandomSource renderRand = RandomSource.create((long)43L);
        int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)corner);
        Vec3 cornerWorld = Vec3.atLowerCornerOf((Vec3i)corner);
        CoasterCartWheelAxisRenderer.forEachDisengagedWheelPlacement((lx, ly, lz, meshScaleX, meshScaleY, meshScaleZ) -> CoasterCartWheelAxisRenderer.renderOneAxis(poseStack, modelRenderer, buffers, tintState, wheelAxisModel, cr, cg, cb, light, renderRand, cornerWorld, (float)camX, (float)camY, (float)camZ, new Quaternionf(), false, 0.0, lx, ly, lz));
    }

    @Nullable
    static SubWheelState prepareSubWheelState(SubLevel sub, CoasterCartClientFrameCache.RenderFrame frame, float dtSec, float camX, float camY, float camZ) {
        double radPerBlock;
        Vec3 forward;
        double distSq = frame.sampleVisual().distanceToSqr((double)camX, (double)camY, (double)camZ);
        if (distSq > 2304.0) {
            return null;
        }
        boolean withinSpinRange = distSq <= 400.0;
        boolean trackLocked = frame.trackLocked();
        if (withinSpinRange && trackLocked && frame.trackBasis() != null) {
            CoasterPathTrackFrame.TrackBasis basis = frame.trackBasis();
            Vec3 fwdRaw = basis.forward();
            forward = fwdRaw.lengthSqr() > 1.0E-12 ? fwdRaw.normalize() : fwdRaw;
            radPerBlock = SPIN_RADIANS_PER_BLOCK > 1.0E-6f ? (double)SPIN_RADIANS_PER_BLOCK : 1.0 / Math.max(1.0E-4, (double)PLACEHOLDER_WHEEL_RADIUS_BLOCKS);
        } else if (withinSpinRange && frame.plotPoseAlignedExtras()) {
            Vec3 fwdRaw = CoasterCartClientFrameCache.bodyTravelForwardWorld(frame.qBody());
            forward = fwdRaw.lengthSqr() > 1.0E-12 ? fwdRaw.normalize() : fwdRaw;
            radPerBlock = SPIN_RADIANS_PER_BLOCK > 1.0E-6f ? (double)SPIN_RADIANS_PER_BLOCK : 1.0 / Math.max(1.0E-4, (double)PLACEHOLDER_WHEEL_RADIUS_BLOCKS);
        } else {
            forward = Vec3.ZERO;
            radPerBlock = 0.0;
        }
        return new SubWheelState(sub.getUniqueId(), trackLocked, forward, radPerBlock, frame, camX, camY, camZ, dtSec);
    }

    static double advanceSpin(Level level, SubLevel sub, SubWheelState state, BlockPos.MutableBlockPos mut) {
        long cellPacked = BlockPos.asLong((int)mut.getX(), (int)mut.getY(), (int)mut.getZ());
        WheelCellKey key = new WheelCellKey(state.subId, cellPacked);
        double dAngleCell = 0.0;
        if (state.forward.lengthSqr() > 1.0E-12) {
            Vec3 velCell = Sable.HELPER.getVelocity(level, (SubLevelAccess)sub, Vec3.atCenterOf((Vec3i)mut));
            double parallelCell = velCell.dot(state.forward);
            double dsCell = parallelCell * (double)state.dtSec;
            dAngleCell = dsCell * state.radPerBlock * (double)SPIN_SIGN;
        }
        return spinRadiansByCell.merge(key, dAngleCell, Double::sum);
    }

    static void renderCell(PoseStack poseStack, ModelBlockRenderer modelRenderer, MultiBufferSource buffers, BlockState tintState, float cr, float cg, float cb, RandomSource renderRand, Level level, SubLevel sub, SubWheelState state, BlockPos.MutableBlockPos mut) {
        if (wheelAxisModel == null) {
            return;
        }
        double spin = CoasterCartWheelAxisRenderer.advanceSpin(level, sub, state, mut);
        Pose3d visualPose = state.visualPose;
        Vec3 lightSampleWorld = visualPose.transformPosition(Vec3.atCenterOf((Vec3i)mut));
        BlockPos lightPos = BlockPos.containing((Position)lightSampleWorld);
        int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)lightPos);
        float wheelYComp = 0.1875f;
        float lxF = ENGAGED_AXLE_BASE_BODY_X + ENGAGED_FRONT_AXLE_BODY_X;
        float lxB = ENGAGED_AXLE_BASE_BODY_X + ENGAGED_BACK_AXLE_BODY_X;
        float lyF = 0.1875f + ENGAGED_AXLE_BASE_BODY_Y + ENGAGED_FRONT_AXLE_BODY_Y;
        float lzF = ENGAGED_AXLE_BASE_BODY_Z + ENGAGED_FRONT_AXLE_BODY_Z;
        float lyB = 0.1875f + ENGAGED_AXLE_BASE_BODY_Y + ENGAGED_BACK_AXLE_BODY_Y;
        float lzB = ENGAGED_AXLE_BASE_BODY_Z + ENGAGED_BACK_AXLE_BODY_Z;
        float lyFLower = lyF + LOWER_WHEEL_ROW_BODY_Y_OFFSET;
        float lyBLower = lyB + LOWER_WHEEL_ROW_BODY_Y_OFFSET;
        double spinLower = spin * (double)LOWER_WHEEL_ROW_SPIN_SIGN;
        if (state.trackLocked && state.qPath != null && state.pathAnchorBottom != null) {
            CoasterCartWheelAxisRenderer.renderOneAxis(poseStack, modelRenderer, buffers, tintState, wheelAxisModel, cr, cg, cb, light, renderRand, state.pathAnchorBottom, state.camX, state.camY, state.camZ, state.qPath, true, spin, lxF, lyF, lzF);
            CoasterCartWheelAxisRenderer.renderOneAxis(poseStack, modelRenderer, buffers, tintState, wheelAxisModel, cr, cg, cb, light, renderRand, state.pathAnchorBottom, state.camX, state.camY, state.camZ, state.qPath, true, spin, lxB, lyB, lzB);
            CoasterCartWheelAxisRenderer.renderOneAxis(poseStack, modelRenderer, buffers, tintState, wheelAxisModel, cr, cg, cb, light, renderRand, state.pathAnchorBottom, state.camX, state.camY, state.camZ, state.qPath, true, spinLower, lxF, lyFLower, lzF);
            CoasterCartWheelAxisRenderer.renderOneAxis(poseStack, modelRenderer, buffers, tintState, wheelAxisModel, cr, cg, cb, light, renderRand, state.pathAnchorBottom, state.camX, state.camY, state.camZ, state.qPath, true, spinLower, lxB, lyBLower, lzB);
        } else {
            Vec3 cornerWorld = visualPose.transformPosition(Vec3.atLowerCornerOf((Vec3i)mut));
            CoasterCartWheelAxisRenderer.renderOneAxis(poseStack, modelRenderer, buffers, tintState, wheelAxisModel, cr, cg, cb, light, renderRand, cornerWorld, state.camX, state.camY, state.camZ, state.qBody, false, spin, lxF, lyF, lzF);
            CoasterCartWheelAxisRenderer.renderOneAxis(poseStack, modelRenderer, buffers, tintState, wheelAxisModel, cr, cg, cb, light, renderRand, cornerWorld, state.camX, state.camY, state.camZ, state.qBody, false, spin, lxB, lyB, lzB);
            CoasterCartWheelAxisRenderer.renderOneAxis(poseStack, modelRenderer, buffers, tintState, wheelAxisModel, cr, cg, cb, light, renderRand, cornerWorld, state.camX, state.camY, state.camZ, state.qBody, false, spinLower, lxF, lyFLower, lzF);
            CoasterCartWheelAxisRenderer.renderOneAxis(poseStack, modelRenderer, buffers, tintState, wheelAxisModel, cr, cg, cb, light, renderRand, cornerWorld, state.camX, state.camY, state.camZ, state.qBody, false, spinLower, lxB, lyBLower, lzB);
        }
    }

    public static void forEachDisengagedWheelPlacement(WheelPlacementConsumer consumer) {
        CoasterCartWheelAxisRenderer.forEachDisengagedWheelPlacement(consumer, 1.0f, 0.0f, 1.0f, 1.0f);
    }

    public static void forEachDisengagedWheelPlacement(WheelPlacementConsumer consumer, float zSpread, float xInset, float meshScaleX, float meshScaleZ) {
        float wheelYComp = 0.1875f;
        float lyUpper = wheelYComp + ENGAGED_AXLE_BASE_BODY_Y + ENGAGED_FRONT_AXLE_BODY_Y;
        float lyLower = lyUpper + LOWER_WHEEL_ROW_BODY_Y_OFFSET;
        float lzFront = (ENGAGED_AXLE_BASE_BODY_Z + ENGAGED_FRONT_AXLE_BODY_Z) * zSpread;
        float lzBack = (ENGAGED_AXLE_BASE_BODY_Z + ENGAGED_BACK_AXLE_BODY_Z) * zSpread;
        float lxFront = CoasterCartWheelAxisRenderer.lerpTowardBlockCenterX(ENGAGED_AXLE_BASE_BODY_X + ENGAGED_FRONT_AXLE_BODY_X, xInset);
        float lxBack = CoasterCartWheelAxisRenderer.lerpTowardBlockCenterX(ENGAGED_AXLE_BASE_BODY_X + ENGAGED_BACK_AXLE_BODY_X, xInset);
        consumer.accept(lxFront, lyUpper, lzFront, meshScaleX, 1.0f, meshScaleZ);
        consumer.accept(lxBack, lyUpper, lzBack, meshScaleX, 1.0f, meshScaleZ);
        consumer.accept(lxFront, lyLower, lzFront, meshScaleX, 1.0f, meshScaleZ);
        consumer.accept(lxBack, lyLower, lzBack, meshScaleX, 1.0f, meshScaleZ);
    }

    private static float lerpTowardBlockCenterX(float bodyX, float inset) {
        return bodyX + (0.5f - bodyX) * inset;
    }

    public static void forEachItemInventoryWheelPlacement(WheelPlacementConsumer consumer) {
        CoasterCartWheelAxisRenderer.forEachDisengagedWheelPlacement(consumer, ITEM_INVENTORY_WHEEL_Z_SPREAD, ITEM_INVENTORY_WHEEL_X_INSET, ITEM_INVENTORY_WHEEL_MESH_SCALE_X, ITEM_INVENTORY_WHEEL_MESH_SCALE_Z);
    }

    @Deprecated
    public static void render(Minecraft mc, PoseStack poseStack, MultiBufferSource.BufferSource buffers, Camera camera, DeltaTracker deltaTracker) {
        CoasterCartWorldRenderer.render(mc, poseStack, buffers, camera, deltaTracker);
    }

    private static void renderOneAxis(PoseStack poseStack, ModelBlockRenderer modelRenderer, MultiBufferSource buffers, BlockState tintState, BakedModel model, float cr, float cg, float cb, int light, RandomSource renderRand, Vec3 worldOrigin, float camX, float camY, float camZ, Quaternionf orientation, boolean railLockedBottomFrame, double spinRad, float localX, float localY, float localZ) {
        poseStack.pushPose();
        poseStack.translate((float)worldOrigin.x - camX, (float)worldOrigin.y - camY, (float)worldOrigin.z - camZ);
        poseStack.mulPose(orientation);
        if (railLockedBottomFrame) {
            poseStack.translate(-0.5f, -0.5f, -0.5f);
        }
        poseStack.translate(localX, localY, localZ);
        poseStack.translate(PLACEHOLDER_PIVOT_X, PLACEHOLDER_PIVOT_Y, PLACEHOLDER_PIVOT_Z);
        poseStack.mulPose(new Quaternionf().rotationX((float)spinRad));
        poseStack.translate(-PLACEHOLDER_PIVOT_X, -PLACEHOLDER_PIVOT_Y, -PLACEHOLDER_PIVOT_Z);
        for (RenderType rt : model.getRenderTypes(tintState, renderRand, ModelData.EMPTY)) {
            VertexConsumer consumer = buffers.getBuffer(rt);
            modelRenderer.renderModel(poseStack.last(), consumer, tintState, model, cr, cg, cb, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, rt);
        }
        poseStack.popPose();
    }

    static {
        spinRadiansByCell = new HashMap<WheelCellKey, Double>();
        ITEM_INVENTORY_WHEEL_Z_SPREAD = 0.8f;
        ITEM_INVENTORY_WHEEL_X_INSET = 0.35f;
        ITEM_INVENTORY_WHEEL_MESH_SCALE_X = 0.78f;
        ITEM_INVENTORY_WHEEL_MESH_SCALE_Z = 0.82f;
    }

    @FunctionalInterface
    public static interface WheelPlacementConsumer {
        public void accept(float var1, float var2, float var3, float var4, float var5, float var6);
    }

    static final class SubWheelState {
        final UUID subId;
        final boolean trackLocked;
        final Vec3 forward;
        final double radPerBlock;
        final Pose3d visualPose;
        final Quaternionf qBody;
        final Quaternionf qPath;
        final Vec3 pathAnchorBottom;
        final float camX;
        final float camY;
        final float camZ;
        final float dtSec;

        SubWheelState(UUID subId, boolean trackLocked, Vec3 forward, double radPerBlock, CoasterCartClientFrameCache.RenderFrame frame, float camX, float camY, float camZ, float dtSec) {
            this.subId = subId;
            this.trackLocked = trackLocked;
            this.forward = forward;
            this.radPerBlock = radPerBlock;
            this.visualPose = frame.visualPose();
            this.qBody = frame.qBody();
            this.qPath = frame.qPath();
            this.pathAnchorBottom = frame.pathAnchorBottom();
            this.camX = camX;
            this.camY = camY;
            this.camZ = camZ;
            this.dtSec = dtSec;
        }
    }

    private record WheelCellKey(UUID subId, long cellPacked) {
    }
}

