package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartCompositeItemBakedModel implements BakedModel {
   private static final ThreadLocal<ItemDisplayContext> ACTIVE_DISPLAY = new ThreadLocal<>();
   private final BakedModel top;
   private final BakedModel bottom;
   private final BakedModel wheelAxis;
   private final BakedModel transformsDelegate;

   public CoasterCartCompositeItemBakedModel(BakedModel top, BakedModel bottom, BakedModel wheelAxis, BakedModel transformsDelegate) {
      this.top = top;
      this.bottom = bottom;
      this.wheelAxis = wheelAxis;
      this.transformsDelegate = transformsDelegate;
   }

   public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
      if (side != null) {
         return List.of();
      } else {
         boolean inventoryWheelPack = ACTIVE_DISPLAY.get() == ItemDisplayContext.GUI;
         List<BakedQuad> out = new ArrayList<>();
         appendModelQuads(out, this.top, rand, data, renderType);
         appendModelQuads(out, this.bottom, rand, data, renderType);
         appendWheelQuads(out, this.wheelAxis, rand, data, renderType, inventoryWheelPack);
         return out;
      }
   }

   public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
      if (side != null) {
         return List.of();
      } else {
         List<BakedQuad> out = new ArrayList<>();
         RandomSource fixed = RandomSource.create(0L);
         ModelData data = ModelData.EMPTY;
         boolean inventoryWheelPack = ACTIVE_DISPLAY.get() == ItemDisplayContext.GUI;

         for (RenderType type : this.getRenderTypes(ItemStack.EMPTY, false)) {
            appendModelQuads(out, this.top, fixed, data, type);
            appendModelQuads(out, this.bottom, fixed, data, type);
            appendWheelQuads(out, this.wheelAxis, fixed, data, type, inventoryWheelPack);
         }

         return out;
      }
   }

   private static void appendWheelQuads(
      List<BakedQuad> out, BakedModel wheelAxis, RandomSource rand, ModelData data, RenderType type, boolean inventoryWheelPack
   ) {
      CoasterCartWheelAxisRenderer.WheelPlacementConsumer place = (x, y, z, meshScaleX, meshScaleY, meshScaleZ) -> {
         Transformation xf = new Transformation(new Matrix4f().translation(x, y, z).scale(meshScaleX, meshScaleY, meshScaleZ));
         appendTransformedModelQuads(out, wheelAxis, rand, data, type, xf);
      };
      if (inventoryWheelPack) {
         CoasterCartWheelAxisRenderer.forEachItemInventoryWheelPlacement(place);
      } else {
         CoasterCartWheelAxisRenderer.forEachDisengagedWheelPlacement(place);
      }
   }

   private static void appendModelQuads(List<BakedQuad> out, BakedModel model, RandomSource rand, ModelData data, RenderType type) {
      out.addAll(model.getQuads(null, null, rand, data, type));

      for (Direction direction : Direction.values()) {
         out.addAll(model.getQuads(null, direction, rand, data, type));
      }
   }

   private static void appendTransformedModelQuads(List<BakedQuad> out, BakedModel model, RandomSource rand, ModelData data, RenderType type, Transformation xf) {
      List<BakedQuad> slice = new ArrayList<>();
      appendModelQuads(slice, model, rand, data, type);
      out.addAll(QuadTransformers.applying(xf).process(slice));
   }

   public boolean useAmbientOcclusion() {
      return this.top.useAmbientOcclusion();
   }

   public boolean isGui3d() {
      return true;
   }

   public boolean usesBlockLight() {
      return false;
   }

   public boolean isCustomRenderer() {
      return false;
   }

   public TextureAtlasSprite getParticleIcon() {
      return this.top.getParticleIcon();
   }

   public ItemTransforms getTransforms() {
      return this.transformsDelegate.getTransforms();
   }

   public ItemOverrides getOverrides() {
      return this.transformsDelegate.getOverrides();
   }

   public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean leftHand) {
      ACTIVE_DISPLAY.set(transformType);
      this.transformsDelegate.applyTransform(transformType, poseStack, leftHand);
      return this;
   }

   public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
      return ChunkRenderTypeSet.union(
         new ChunkRenderTypeSet[]{
            this.top.getRenderTypes(state, rand, data), this.bottom.getRenderTypes(state, rand, data), this.wheelAxis.getRenderTypes(state, rand, data)
         }
      );
   }

   public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
      LinkedHashSet<RenderType> types = new LinkedHashSet<>();
      types.addAll(this.top.getRenderTypes(itemStack, fabulous));
      types.addAll(this.bottom.getRenderTypes(itemStack, fabulous));
      types.addAll(this.wheelAxis.getRenderTypes(itemStack, fabulous));
      return List.copyOf(types);
   }
}
