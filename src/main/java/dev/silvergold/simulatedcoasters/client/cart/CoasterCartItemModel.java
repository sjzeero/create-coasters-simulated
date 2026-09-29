package dev.silvergold.simulatedcoasters.client.cart;

import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartItemModel {
   public static final ModelResourceLocation INVENTORY_MRL = ModelResourceLocation.inventory(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_cart")
   );
   public static final ModelResourceLocation ITEM_WHEEL_AXIS_MRL = ModelResourceLocation.standalone(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "block/coaster_cart_wheel_axis_item")
   );

   private CoasterCartItemModel() {
   }

   public static void registerStandaloneModels(RegisterAdditional event) {
      event.register(ITEM_WHEEL_AXIS_MRL);
   }

   public static void bakeCompositeItemModel(ModifyBakingResult event) {
      BakedModel inventoryBase = (BakedModel)event.getModels().get(INVENTORY_MRL);
      BlockState topState = ((CoasterCartBlock)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()).defaultBlockState();
      ModelResourceLocation topMrl = BlockModelShaper.stateToModelLocation(topState);
      BakedModel top = (BakedModel)event.getModels().get(topMrl);
      BakedModel bottom = (BakedModel)event.getModels().get(CoasterCartBottomSliceRenderer.STANDALONE_BOTTOM_MRL);
      BakedModel wheel = (BakedModel)event.getModels().get(ITEM_WHEEL_AXIS_MRL);
      if (inventoryBase != null && top != null && bottom != null && wheel != null) {
         event.getModels().put(INVENTORY_MRL, new CoasterCartCompositeItemBakedModel(top, bottom, wheel, inventoryBase));
      }
   }
}
