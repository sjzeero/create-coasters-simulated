package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
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
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartWheelAxisRenderer {
   public static final ModelResourceLocation WHEEL_AXIS_MRL = ModelResourceLocation.standalone(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "block/coaster_cart_wheel_axis")
   );
   public static float ENGAGED_AXLE_BASE_BODY_X = 0.0F;
   public static float ENGAGED_AXLE_BASE_BODY_Y = 0.0F;
   public static float ENGAGED_AXLE_BASE_BODY_Z = 0.0F;
   public static float ENGAGED_FRONT_AXLE_BODY_X = 0.0F;
   public static float ENGAGED_FRONT_AXLE_BODY_Y = 0.1F;
   public static float ENGAGED_FRONT_AXLE_BODY_Z = -0.7F;
   public static float ENGAGED_BACK_AXLE_BODY_X = 0.0F;
   public static float ENGAGED_BACK_AXLE_BODY_Y = 0.1F;
   public static float ENGAGED_BACK_AXLE_BODY_Z = 0.7F;
   public static float PLACEHOLDER_PIVOT_X = 0.5F;
   public static float PLACEHOLDER_PIVOT_Y = 0.5F;
   public static float PLACEHOLDER_PIVOT_Z = 0.5F;
   public static float SPIN_RADIANS_PER_BLOCK = 0.0F;
   public static float PLACEHOLDER_WHEEL_RADIUS_BLOCKS = 0.2F;
   public static float SPIN_SIGN = -1.0F;
   public static float LOWER_WHEEL_ROW_BODY_Y_OFFSET = -0.55F;
   public static float LOWER_WHEEL_ROW_SPIN_SIGN = -1.0F;
   private static final double WHEEL_SPIN_MAX_DISTANCE_BLOCKS = 20.0;
   private static final double WHEEL_SPIN_MAX_DIST_SQ = 400.0;
   private static final double WHEEL_DRAW_MAX_DISTANCE_BLOCKS = 48.0;
   private static final double WHEEL_DRAW_MAX_DIST_SQ = 2304.0;
   private static BakedModel wheelAxisModel;
   private static final Map<CoasterCartWheelAxisRenderer.WheelCellKey, Double> spinRadiansByCell = new HashMap<>();
   public static float ITEM_INVENTORY_WHEEL_Z_SPREAD = 0.8F;
   public static float ITEM_INVENTORY_WHEEL_X_INSET = 0.35F;
   public static float ITEM_INVENTORY_WHEEL_MESH_SCALE_X = 0.78F;
   public static float ITEM_INVENTORY_WHEEL_MESH_SCALE_Z = 0.82F;

   private CoasterCartWheelAxisRenderer() {
   }

   public static void registerStandaloneModels(RegisterAdditional event) {
      event.register(WHEEL_AXIS_MRL);
   }

   public static void cacheModels(ModifyBakingResult event) {
      wheelAxisModel = (BakedModel)event.getModels().get(WHEEL_AXIS_MRL);
   }

   static boolean hasModel() {
      return wheelAxisModel != null;
   }

   public static void renderStandingAt(
      PoseStack poseStack,
      MultiBufferSource buffers,
      BlockState tintState,
      float cr,
      float cg,
      float cb,
      Level level,
      BlockPos corner,
      double camX,
      double camY,
      double camZ
   ) {
      if (wheelAxisModel != null) {
         Minecraft mc = Minecraft.getInstance();
         ModelBlockRenderer modelRenderer = mc.getBlockRenderer().getModelRenderer();
         RandomSource renderRand = RandomSource.create(43L);
         int light = LevelRenderer.getLightColor(level, corner);
         Vec3 cornerWorld = Vec3.atLowerCornerOf(corner);
         forEachDisengagedWheelPlacement(
            (lx, ly, lz, meshScaleX, meshScaleY, meshScaleZ) -> renderOneAxis(
                  poseStack,
                  modelRenderer,
                  buffers,
                  tintState,
                  wheelAxisModel,
                  cr,
                  cg,
                  cb,
                  light,
                  renderRand,
                  cornerWorld,
                  (float)camX,
                  (float)camY,
                  (float)camZ,
                  new Quaternionf(),
                  false,
                  0.0,
                  lx,
                  ly,
                  lz
               )
         );
      }
   }

   @Nullable
   static CoasterCartWheelAxisRenderer.SubWheelState prepareSubWheelState(
      SubLevel sub, CoasterCartClientFrameCache.RenderFrame frame, float dtSec, float camX, float camY, float camZ
   ) {
      double distSq = frame.sampleVisual().distanceToSqr((double)camX, (double)camY, (double)camZ);
      if (distSq > 2304.0) {
         return null;
      } else {
         boolean withinSpinRange = distSq <= 400.0;
         boolean trackLocked = frame.trackLocked();
         Vec3 forward;
         double radPerBlock;
         if (withinSpinRange && trackLocked && frame.trackBasis() != null) {
            CoasterPathTrackFrame.TrackBasis basis = frame.trackBasis();
            Vec3 fwdRaw = basis.forward();
            forward = fwdRaw.lengthSqr() > 1.0E-12 ? fwdRaw.normalize() : fwdRaw;
            radPerBlock = SPIN_RADIANS_PER_BLOCK > 1.0E-6F ? (double)SPIN_RADIANS_PER_BLOCK : 1.0 / Math.max(1.0E-4, (double)PLACEHOLDER_WHEEL_RADIUS_BLOCKS);
         } else if (withinSpinRange && frame.plotPoseAlignedExtras()) {
            Vec3 fwdRaw = CoasterCartClientFrameCache.bodyTravelForwardWorld(frame.qBody());
            forward = fwdRaw.lengthSqr() > 1.0E-12 ? fwdRaw.normalize() : fwdRaw;
            radPerBlock = SPIN_RADIANS_PER_BLOCK > 1.0E-6F ? (double)SPIN_RADIANS_PER_BLOCK : 1.0 / Math.max(1.0E-4, (double)PLACEHOLDER_WHEEL_RADIUS_BLOCKS);
         } else {
            forward = Vec3.ZERO;
            radPerBlock = 0.0;
         }

         return new CoasterCartWheelAxisRenderer.SubWheelState(sub.getUniqueId(), trackLocked, forward, radPerBlock, frame, camX, camY, camZ, dtSec);
      }
   }

   static double advanceSpin(Level level, SubLevel sub, CoasterCartWheelAxisRenderer.SubWheelState state, MutableBlockPos mut) {
      long cellPacked = BlockPos.asLong(mut.getX(), mut.getY(), mut.getZ());
      CoasterCartWheelAxisRenderer.WheelCellKey key = new CoasterCartWheelAxisRenderer.WheelCellKey(state.subId, cellPacked);
      double dAngleCell = 0.0;
      if (state.forward.lengthSqr() > 1.0E-12) {
         Vec3 velCell = Sable.HELPER.getVelocity(level, sub, Vec3.atCenterOf(mut));
         double parallelCell = velCell.dot(state.forward);
         double dsCell = parallelCell * (double)state.dtSec;
         dAngleCell = dsCell * state.radPerBlock * (double)SPIN_SIGN;
      }

      return spinRadiansByCell.merge(key, dAngleCell, Double::sum);
   }

   static void renderCell(
      PoseStack poseStack,
      ModelBlockRenderer modelRenderer,
      MultiBufferSource buffers,
      BlockState tintState,
      float cr,
      float cg,
      float cb,
      RandomSource renderRand,
      Level level,
      SubLevel sub,
      CoasterCartWheelAxisRenderer.SubWheelState state,
      MutableBlockPos mut
   ) {
      if (wheelAxisModel != null) {
         double spin = advanceSpin(level, sub, state, mut);
         Pose3d visualPose = state.visualPose;
         Vec3 lightSampleWorld = visualPose.transformPosition(Vec3.atCenterOf(mut));
         BlockPos lightPos = BlockPos.containing(lightSampleWorld);
         int light = LevelRenderer.getLightColor(level, lightPos);
         float wheelYComp = 0.1875F;
         float lxF = ENGAGED_AXLE_BASE_BODY_X + ENGAGED_FRONT_AXLE_BODY_X;
         float lxB = ENGAGED_AXLE_BASE_BODY_X + ENGAGED_BACK_AXLE_BODY_X;
         float lyF = 0.1875F + ENGAGED_AXLE_BASE_BODY_Y + ENGAGED_FRONT_AXLE_BODY_Y;
         float lzF = ENGAGED_AXLE_BASE_BODY_Z + ENGAGED_FRONT_AXLE_BODY_Z;
         float lyB = 0.1875F + ENGAGED_AXLE_BASE_BODY_Y + ENGAGED_BACK_AXLE_BODY_Y;
         float lzB = ENGAGED_AXLE_BASE_BODY_Z + ENGAGED_BACK_AXLE_BODY_Z;
         float lyFLower = lyF + LOWER_WHEEL_ROW_BODY_Y_OFFSET;
         float lyBLower = lyB + LOWER_WHEEL_ROW_BODY_Y_OFFSET;
         double spinLower = spin * (double)LOWER_WHEEL_ROW_SPIN_SIGN;
         if (state.trackLocked && state.qPath != null && state.pathAnchorBottom != null) {
            renderOneAxis(
               poseStack,
               modelRenderer,
               buffers,
               tintState,
               wheelAxisModel,
               cr,
               cg,
               cb,
               light,
               renderRand,
               state.pathAnchorBottom,
               state.camX,
               state.camY,
               state.camZ,
               state.qPath,
               true,
               spin,
               lxF,
               lyF,
               lzF
            );
            renderOneAxis(
               poseStack,
               modelRenderer,
               buffers,
               tintState,
               wheelAxisModel,
               cr,
               cg,
               cb,
               light,
               renderRand,
               state.pathAnchorBottom,
               state.camX,
               state.camY,
               state.camZ,
               state.qPath,
               true,
               spin,
               lxB,
               lyB,
               lzB
            );
            renderOneAxis(
               poseStack,
               modelRenderer,
               buffers,
               tintState,
               wheelAxisModel,
               cr,
               cg,
               cb,
               light,
               renderRand,
               state.pathAnchorBottom,
               state.camX,
               state.camY,
               state.camZ,
               state.qPath,
               true,
               spinLower,
               lxF,
               lyFLower,
               lzF
            );
            renderOneAxis(
               poseStack,
               modelRenderer,
               buffers,
               tintState,
               wheelAxisModel,
               cr,
               cg,
               cb,
               light,
               renderRand,
               state.pathAnchorBottom,
               state.camX,
               state.camY,
               state.camZ,
               state.qPath,
               true,
               spinLower,
               lxB,
               lyBLower,
               lzB
            );
         } else {
            Vec3 cornerWorld = visualPose.transformPosition(Vec3.atLowerCornerOf(mut));
            renderOneAxis(
               poseStack,
               modelRenderer,
               buffers,
               tintState,
               wheelAxisModel,
               cr,
               cg,
               cb,
               light,
               renderRand,
               cornerWorld,
               state.camX,
               state.camY,
               state.camZ,
               state.qBody,
               false,
               spin,
               lxF,
               lyF,
               lzF
            );
            renderOneAxis(
               poseStack,
               modelRenderer,
               buffers,
               tintState,
               wheelAxisModel,
               cr,
               cg,
               cb,
               light,
               renderRand,
               cornerWorld,
               state.camX,
               state.camY,
               state.camZ,
               state.qBody,
               false,
               spin,
               lxB,
               lyB,
               lzB
            );
            renderOneAxis(
               poseStack,
               modelRenderer,
               buffers,
               tintState,
               wheelAxisModel,
               cr,
               cg,
               cb,
               light,
               renderRand,
               cornerWorld,
               state.camX,
               state.camY,
               state.camZ,
               state.qBody,
               false,
               spinLower,
               lxF,
               lyFLower,
               lzF
            );
            renderOneAxis(
               poseStack,
               modelRenderer,
               buffers,
               tintState,
               wheelAxisModel,
               cr,
               cg,
               cb,
               light,
               renderRand,
               cornerWorld,
               state.camX,
               state.camY,
               state.camZ,
               state.qBody,
               false,
               spinLower,
               lxB,
               lyBLower,
               lzB
            );
         }
      }
   }

   public static void forEachDisengagedWheelPlacement(CoasterCartWheelAxisRenderer.WheelPlacementConsumer consumer) {
      forEachDisengagedWheelPlacement(consumer, 1.0F, 0.0F, 1.0F, 1.0F);
   }

   public static void forEachDisengagedWheelPlacement(
      CoasterCartWheelAxisRenderer.WheelPlacementConsumer consumer, float zSpread, float xInset, float meshScaleX, float meshScaleZ
   ) {
      float wheelYComp = 0.1875F;
      float lyUpper = wheelYComp + ENGAGED_AXLE_BASE_BODY_Y + ENGAGED_FRONT_AXLE_BODY_Y;
      float lyLower = lyUpper + LOWER_WHEEL_ROW_BODY_Y_OFFSET;
      float lzFront = (ENGAGED_AXLE_BASE_BODY_Z + ENGAGED_FRONT_AXLE_BODY_Z) * zSpread;
      float lzBack = (ENGAGED_AXLE_BASE_BODY_Z + ENGAGED_BACK_AXLE_BODY_Z) * zSpread;
      float lxFront = lerpTowardBlockCenterX(ENGAGED_AXLE_BASE_BODY_X + ENGAGED_FRONT_AXLE_BODY_X, xInset);
      float lxBack = lerpTowardBlockCenterX(ENGAGED_AXLE_BASE_BODY_X + ENGAGED_BACK_AXLE_BODY_X, xInset);
      consumer.accept(lxFront, lyUpper, lzFront, meshScaleX, 1.0F, meshScaleZ);
      consumer.accept(lxBack, lyUpper, lzBack, meshScaleX, 1.0F, meshScaleZ);
      consumer.accept(lxFront, lyLower, lzFront, meshScaleX, 1.0F, meshScaleZ);
      consumer.accept(lxBack, lyLower, lzBack, meshScaleX, 1.0F, meshScaleZ);
   }

   private static float lerpTowardBlockCenterX(float bodyX, float inset) {
      return bodyX + (0.5F - bodyX) * inset;
   }

   public static void forEachItemInventoryWheelPlacement(CoasterCartWheelAxisRenderer.WheelPlacementConsumer consumer) {
      forEachDisengagedWheelPlacement(
         consumer, ITEM_INVENTORY_WHEEL_Z_SPREAD, ITEM_INVENTORY_WHEEL_X_INSET, ITEM_INVENTORY_WHEEL_MESH_SCALE_X, ITEM_INVENTORY_WHEEL_MESH_SCALE_Z
      );
   }

   @Deprecated
   public static void render(Minecraft mc, PoseStack poseStack, BufferSource buffers, Camera camera, DeltaTracker deltaTracker) {
      CoasterCartWorldRenderer.render(mc, poseStack, buffers, camera, deltaTracker);
   }

   private static void renderOneAxis(
      PoseStack poseStack,
      ModelBlockRenderer modelRenderer,
      MultiBufferSource buffers,
      BlockState tintState,
      BakedModel model,
      float cr,
      float cg,
      float cb,
      int light,
      RandomSource renderRand,
      Vec3 worldOrigin,
      float camX,
      float camY,
      float camZ,
      Quaternionf orientation,
      boolean railLockedBottomFrame,
      double spinRad,
      float localX,
      float localY,
      float localZ
   ) {
      poseStack.pushPose();
      poseStack.translate((float)worldOrigin.x - camX, (float)worldOrigin.y - camY, (float)worldOrigin.z - camZ);
      poseStack.mulPose(orientation);
      if (railLockedBottomFrame) {
         poseStack.translate(-0.5F, -0.5F, -0.5F);
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

      SubWheelState(
         UUID subId,
         boolean trackLocked,
         Vec3 forward,
         double radPerBlock,
         CoasterCartClientFrameCache.RenderFrame frame,
         float camX,
         float camY,
         float camZ,
         float dtSec
      ) {
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

   private static record WheelCellKey(UUID subId, long cellPacked) {
   }

   @FunctionalInterface
   public interface WheelPlacementConsumer {
      void accept(float var1, float var2, float var3, float var4, float var5, float var6);
   }
}
