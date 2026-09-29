/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Transformation
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.block.model.ItemOverrides
 *  net.minecraft.client.renderer.block.model.ItemTransforms
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.core.Direction
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.client.ChunkRenderTypeSet
 *  net.neoforged.neoforge.client.model.QuadTransformers
 *  net.neoforged.neoforge.client.model.data.ModelData
 *  org.joml.Matrix4f
 */
package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartWheelAxisRenderer;
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

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartCompositeItemBakedModel
implements BakedModel {
    private static final ThreadLocal<ItemDisplayContext> ACTIVE_DISPLAY = new ThreadLocal();
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
        }
        boolean inventoryWheelPack = ACTIVE_DISPLAY.get() == ItemDisplayContext.GUI;
        ArrayList<BakedQuad> out = new ArrayList<BakedQuad>();
        CoasterCartCompositeItemBakedModel.appendModelQuads(out, this.top, rand, data, renderType);
        CoasterCartCompositeItemBakedModel.appendModelQuads(out, this.bottom, rand, data, renderType);
        CoasterCartCompositeItemBakedModel.appendWheelQuads(out, this.wheelAxis, rand, data, renderType, inventoryWheelPack);
        return out;
    }

    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand) {
        if (side != null) {
            return List.of();
        }
        ArrayList<BakedQuad> out = new ArrayList<BakedQuad>();
        RandomSource fixed = RandomSource.create((long)0L);
        ModelData data = ModelData.EMPTY;
        boolean inventoryWheelPack = ACTIVE_DISPLAY.get() == ItemDisplayContext.GUI;
        for (RenderType type : this.getRenderTypes(ItemStack.EMPTY, false)) {
            CoasterCartCompositeItemBakedModel.appendModelQuads(out, this.top, fixed, data, type);
            CoasterCartCompositeItemBakedModel.appendModelQuads(out, this.bottom, fixed, data, type);
            CoasterCartCompositeItemBakedModel.appendWheelQuads(out, this.wheelAxis, fixed, data, type, inventoryWheelPack);
        }
        return out;
    }

    private static void appendWheelQuads(List<BakedQuad> out, BakedModel wheelAxis, RandomSource rand, ModelData data, RenderType type, boolean inventoryWheelPack) {
        CoasterCartWheelAxisRenderer.WheelPlacementConsumer place = (x, y, z, meshScaleX, meshScaleY, meshScaleZ) -> {
            Transformation xf = new Transformation(new Matrix4f().translation(x, y, z).scale(meshScaleX, meshScaleY, meshScaleZ));
            CoasterCartCompositeItemBakedModel.appendTransformedModelQuads(out, wheelAxis, rand, data, type, xf);
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
        ArrayList<BakedQuad> slice = new ArrayList<BakedQuad>();
        CoasterCartCompositeItemBakedModel.appendModelQuads(slice, model, rand, data, type);
        out.addAll(QuadTransformers.applying((Transformation)xf).process(slice));
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
        return ChunkRenderTypeSet.union((ChunkRenderTypeSet[])new ChunkRenderTypeSet[]{this.top.getRenderTypes(state, rand, data), this.bottom.getRenderTypes(state, rand, data), this.wheelAxis.getRenderTypes(state, rand, data)});
    }

    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        LinkedHashSet types = new LinkedHashSet();
        types.addAll(this.top.getRenderTypes(itemStack, fabulous));
        types.addAll(this.bottom.getRenderTypes(itemStack, fabulous));
        types.addAll(this.wheelAxis.getRenderTypes(itemStack, fabulous));
        return List.copyOf(types);
    }
}

