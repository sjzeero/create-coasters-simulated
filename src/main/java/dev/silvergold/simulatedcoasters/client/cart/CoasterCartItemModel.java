/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.block.BlockModelShaper
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.client.resources.model.ModelResourceLocation
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.client.event.ModelEvent$ModifyBakingResult
 *  net.neoforged.neoforge.client.event.ModelEvent$RegisterAdditional
 */
package dev.silvergold.simulatedcoasters.client.cart;

import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartBottomSliceRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartCompositeItemBakedModel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartItemModel {
    public static final ModelResourceLocation INVENTORY_MRL = ModelResourceLocation.inventory((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_cart"));
    public static final ModelResourceLocation ITEM_WHEEL_AXIS_MRL = ModelResourceLocation.standalone((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/coaster_cart_wheel_axis_item"));

    private CoasterCartItemModel() {
    }

    public static void registerStandaloneModels(ModelEvent.RegisterAdditional event) {
        event.register(ITEM_WHEEL_AXIS_MRL);
    }

    public static void bakeCompositeItemModel(ModelEvent.ModifyBakingResult event) {
        BakedModel inventoryBase = (BakedModel)event.getModels().get(INVENTORY_MRL);
        BlockState topState = ((CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get())).defaultBlockState();
        ModelResourceLocation topMrl = BlockModelShaper.stateToModelLocation((BlockState)topState);
        BakedModel top = (BakedModel)event.getModels().get(topMrl);
        BakedModel bottom = (BakedModel)event.getModels().get(CoasterCartBottomSliceRenderer.STANDALONE_BOTTOM_MRL);
        BakedModel wheel = (BakedModel)event.getModels().get(ITEM_WHEEL_AXIS_MRL);
        if (inventoryBase == null || top == null || bottom == null || wheel == null) {
            return;
        }
        event.getModels().put(INVENTORY_MRL, new CoasterCartCompositeItemBakedModel(top, bottom, wheel, inventoryBase));
    }
}

