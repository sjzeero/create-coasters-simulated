package dev.silvergold.simulatedcoasters;

import com.mojang.logging.LogUtils;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.silvergold.simulatedcoasters.balloon.BalloonAssembly;
import dev.silvergold.simulatedcoasters.balloon.BalloonAttachInteraction;
import dev.silvergold.simulatedcoasters.balloon.BalloonPopInteraction;
import dev.silvergold.simulatedcoasters.balloon.BalloonTetherController;
import dev.silvergold.simulatedcoasters.balloon.BalloonWrenchPickupInteraction;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.rivet.RivetCartDragCompensation;
import dev.silvergold.simulatedcoasters.rivet.RivetDisassemblyEnforcer;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.ManagedFakeTrackInteractionBlock;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartShearUnlinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackSnap;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkSync;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathSync;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import org.slf4j.Logger;

@Mod("simulatedcoasters")
public class SimulatedCoasters {
   public static final String MOD_ID = "simulatedcoasters";
   public static final Logger LOGGER = LogUtils.getLogger();
   public static final Items ITEMS = DeferredRegister.createItems("simulatedcoasters");
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "simulatedcoasters");
   public static final DeferredItem<CoasterTrackItem> COASTER_TRACK = ITEMS.registerItem("coaster_track", CoasterTrackItem::new, new Properties().stacksTo(64));
   public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_COASTER_TRACK = ITEMS.registerItem(
      "incomplete_coaster_track", SequencedAssemblyItem::new, new Properties()
   );
   public static final DeferredItem<CoasterCartItem> COASTER_CART = ITEMS.registerItem("coaster_cart", CoasterCartItem::new, new Properties().stacksTo(64));
   public static final DeferredItem<BlockItem> COASTER_ANCHORPOINT = ITEMS.registerSimpleBlockItem(
      "coaster_anchorpoint", SimulatedCoastersBlocks.COASTER_ANCHORPOINT, new Properties().stacksTo(64)
   );
   public static final DeferredItem<Item> RIVET = ITEMS.registerItem("rivet", Item::new, new Properties().stacksTo(64));
   public static final DeferredItem<Item> RED_BALLOON = ITEMS.registerItem("red_balloon", Item::new, new Properties().stacksTo(64));
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_CREATIVE_TAB = CREATIVE_MODE_TABS.register(
      "main",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.simulatedcoasters"))
            .icon(() -> new ItemStack((ItemLike)COASTER_TRACK.get()))
            .displayItems((params, output) -> {
               output.accept((ItemLike)COASTER_TRACK.get());
               output.accept((ItemLike)COASTER_CART.get());
               output.accept((ItemLike)COASTER_ANCHORPOINT.get());
               output.accept((ItemLike)RIVET.get());
               output.accept((ItemLike)RED_BALLOON.get());
            })
            .build()
   );

   public SimulatedCoasters(IEventBus modEventBus, ModContainer modContainer) {
      SimulatedCoastersBlocks.BLOCKS.register(modEventBus);
      SimulatedCoastersBlocks.BLOCK_ENTITY_TYPES.register(modEventBus);
      SimulatedCoastersParticles.register(modEventBus);
      SimulatedCoastersSoundEvents.register(modEventBus);
      ITEMS.register(modEventBus);
      CREATIVE_MODE_TABS.register(modEventBus);
      modEventBus.addListener(this::commonSetup);
      NeoForge.EVENT_BUS.register(this);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ManagedFakeTrackInteractionBlock::onRightClickBlock);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CoasterCartChainLinkInteraction::onRightClickBlock);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CoasterCartShearUnlinkInteraction::onRightClickBlock);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BalloonAttachInteraction::onRightClickBlock);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BalloonWrenchPickupInteraction::onRightClickBlock);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BalloonPopInteraction::onLeftClickBlock);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BalloonPopInteraction::onRightClickBlock);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BalloonPopInteraction::onUseItemOnBlock);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, BalloonPopInteraction::onBlockPlaced);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ManagedFakeTrackInteractionBlock::onUseItemOnBlock);
      NeoForge.EVENT_BUS.addListener(CoasterCartTrackSnap::onPrePhysicsTick);
      NeoForge.EVENT_BUS.addListener(RivetDisassemblyEnforcer::onPrePhysicsTick);
      NeoForge.EVENT_BUS.addListener(RivetCartDragCompensation::onPrePhysicsTick);
      NeoForge.EVENT_BUS.addListener(BalloonTetherController::onPrePhysicsTick);
      NeoForge.EVENT_BUS.addListener(CoasterCartTrainLinkConstraint::onPostPhysicsTick);
      modEventBus.addListener(CoasterPathSync::registerPayload);
      modEventBus.addListener(CoasterCartTrainLinkSync::registerPayload);
      modContainer.registerConfig(Type.COMMON, Config.SPEC);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(
         () -> {
            BlockMovementChecks.registerMovementAllowedCheck(
               (state, level, pos) -> state.getBlock() instanceof CoasterAnchorpointBlock ? CheckResult.FAIL : CheckResult.PASS
            );
            SubLevelHeatMapManager.addSplitListener((level, bounds, blocks) -> {
               if (level instanceof ServerLevel serverLevel) {
                  BalloonAssembly.refreshAllBalloonHosts(serverLevel);
               }
            });
         }
      );
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
   }
}
