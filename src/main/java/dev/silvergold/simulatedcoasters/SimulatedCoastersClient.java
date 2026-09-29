package dev.silvergold.simulatedcoasters;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu;
import com.simibubi.create.content.kinetics.KineticDebugger;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.silvergold.simulatedcoasters.client.balloon.BalloonTetherRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartBottomSliceRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartChainLinkOutlineClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientFrameCache;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientMembership;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartEditHud;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartEditMode;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartEditOutlineClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartHoverOutlineClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartItemModel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartLinkSelectionOutlineRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartLinkSnapClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartMidTrackPlacementClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartPlacementPreviewRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartShearUnlinkOutlineClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainCouplingRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainLinkClientState;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartWheelAxisRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartWorldRenderer;
import dev.silvergold.simulatedcoasters.client.item.BalloonItemTooltips;
import dev.silvergold.simulatedcoasters.client.item.RivetItemTooltips;
import dev.silvergold.simulatedcoasters.client.item.RivetPlacementHud;
import dev.silvergold.simulatedcoasters.client.item.RivetPlacementOutlineClient;
import dev.silvergold.simulatedcoasters.client.particle.TrackSnapSparkParticle;
import dev.silvergold.simulatedcoasters.client.rivet.RivetPlacementAnimationClient;
import dev.silvergold.simulatedcoasters.client.sound.CoasterCartRailSoundController;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerTrackCurveVisual;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleCurvePreviewOutliner;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleEditMode;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleOverlay;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleOverlayRenderTypes;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftAnchorOutlineClient;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftAnchorShaftRenderer;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftRibbonMeshCache;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftSpanRenderer;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorpointShaftModel;
import dev.silvergold.simulatedcoasters.client.track.CoasterBezierPlacementSpineDebugRenderer;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveDyeApplyClient;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveUndyeApplyClient;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorGirderModel;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorJunctionModel;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackCenterBeamModel;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackDyedBezierModels;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackOpenEndpointOutlineClient;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackPlacementNeighborSmoothSuppressClient;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackPlacementOverlayRenderer;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackPlacementSmoothHud;
import dev.silvergold.simulatedcoasters.client.track.CoasterWrenchCurveAnchorOutlineClient;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.ponder.CoastersPonderPlugin;
import dev.silvergold.simulatedcoasters.track.ClearCoasterTrackHotbarSelectionPayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.OpenEndExtensionPreviewProvider;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorJunctionVisualRefresh;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurvePendingRemoval;
import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphDebugRenderer;
import java.util.List;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent.DebugText;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

@Mod(
   value = "simulatedcoasters",
   dist = {Dist.CLIENT}
)
public class SimulatedCoastersClient {
   private static int lastHotbarSlotIndex = -1;

   public SimulatedCoastersClient(IEventBus modEventBus, ModContainer container) {
      container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
      modEventBus.addListener(SimulatedCoastersClient::onClientSetup);
      modEventBus.addListener(SimulatedCoastersClient::onRegisterEntityRenderers);
      modEventBus.addListener(SimulatedCoastersClient::onRegisterParticleProviders);
      modEventBus.addListener(SimulatedCoastersClient::onRegisterGuiLayers);
      modEventBus.addListener(SimulatedCoastersClient::onRegisterAdditionalModels);
      modEventBus.addListener(SimulatedCoastersClient::onModelBake);
      NeoForge.EVENT_BUS.addListener(SimulatedCoastersClient::onRenderGuiLayerPost);
      NeoForge.EVENT_BUS.addListener(SimulatedCoastersClient::onClientTickPost);
      NeoForge.EVENT_BUS.addListener(SimulatedCoastersClient::onRenderLevelAfterBlockEntities);
      NeoForge.EVENT_BUS.addListener(SimulatedCoastersClient::onRenderWorldBezierHandles);
      NeoForge.EVENT_BUS.addListener(SimulatedCoastersClient::onDebugOverlayText);
      NeoForge.EVENT_BUS.addListener(SimulatedCoastersClient::onRightClickBlockWrenchAnchor);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CoasterCurveDyeApplyClient::onUseItemKey);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CoasterCurveUndyeApplyClient::onUseItemKey);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CoasterCartMidTrackPlacementClient::onUseItemKey);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CoasterTrackOpenEndpointOutlineClient::onUseItemKey);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, CoasterWrenchCurveAnchorOutlineClient::onUseItemKey);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ChainLiftAnchorOutlineClient::onUseItemKey);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, RivetPlacementOutlineClient::onUseItemKey);
      NeoForge.EVENT_BUS.addListener(RivetPlacementOutlineClient::onRightClickBlock);
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, SimulatedCoastersClient::onUseKeyBeforeVanillaForBezierHandle);
      NeoForge.EVENT_BUS.addListener(SimulatedCoastersClient::onClientLevelUnload);
   }

   private static void tickClearHotbarSelectionWhenHotbarChanges(Minecraft mc) {
      LocalPlayer player = mc.player;
      if (player == null) {
         lastHotbarSlotIndex = -1;
      } else {
         Inventory inv = player.getInventory();
         int sel = inv.selected;
         if (lastHotbarSlotIndex >= 0 && sel != lastHotbarSlotIndex) {
            ItemStack prev = inv.getItem(lastHotbarSlotIndex);
            boolean hadTrackSelection = CoasterTrackItem.hasPendingConnection(prev);
            boolean hadCartLinkSelection = CoasterCartItem.hasLinkSelection(prev);
            boolean hadChainLinkSelection = prev.is(Items.CHAIN) && CoasterCartLinkSelection.hasLinkTarget(prev);
            if (hadTrackSelection) {
               CoasterTrackItem.clearPendingConnection(prev);
            }

            if (hadCartLinkSelection) {
               CoasterCartItem.clearLinkSelection(prev);
            }

            if (hadChainLinkSelection) {
               CoasterCartLinkSelection.clearLinkTarget(prev);
            }

            if (hadTrackSelection || hadCartLinkSelection || hadChainLinkSelection) {
               PacketDistributor.sendToServer(new ClearCoasterTrackHotbarSelectionPayload(lastHotbarSlotIndex), new CustomPacketPayload[0]);
            }
         }

         lastHotbarSlotIndex = sel;
      }
   }

   private static void onRegisterEntityRenderers(RegisterRenderers event) {
      event.registerBlockEntityRenderer((BlockEntityType)SimulatedCoastersBlocks.COASTER_ANCHORPOINT_BE.get(), ChainLiftAnchorShaftRenderer::new);
   }

   private static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
      event.registerSpriteSet((ParticleType)SimulatedCoastersParticles.TRACK_SNAP_SPARK.get(), TrackSnapSparkParticle.Provider::new);
   }

   private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
      event.registerAbove(
         VanillaGuiLayers.HOTBAR,
         ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_track_placement"),
         CoasterTrackPlacementOverlayRenderer.OVERLAY
      );
   }

   private static void onClientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(
         () -> {
            RadialWrenchMenu.registerBlacklistedBlock(SimulatedCoastersBlocks.COASTER_ANCHORPOINT.getId());
            PonderIndex.addPlugin(new CoastersPonderPlugin());
            OpenEndExtensionPreviewProvider.installClient(BezierHandleDragManager::openEndExtensionPreviewBlocks);
            RivetItemTooltips.register();
            BalloonItemTooltips.register();
            CoasterAnchorpointShaftModel.registerClientInit();
            ItemBlockRenderTypes.setRenderLayer((Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get(), RenderType.cutout());
            SimpleBlockEntityVisualizer.builder(SimulatedCoastersBlocks.COASTER_ANCHORPOINT_BE.get())
               .factory(AnchorPeerTrackCurveVisual::new)
               .skipVanillaRender(be -> false)
               .apply();
         }
      );
   }

   private static void onRegisterAdditionalModels(RegisterAdditional event) {
      CoasterCartBottomSliceRenderer.registerStandaloneModel(event);
      CoasterCartWheelAxisRenderer.registerStandaloneModels(event);
      CoasterCartItemModel.registerStandaloneModels(event);
      CoasterTrackCenterBeamModel.registerAdditional(event);
      CoasterTrackDyedBezierModels.registerAdditional(event);
      CoasterTrackAnchorJunctionModel.registerAdditional(event);
      CoasterTrackAnchorGirderModel.registerAdditional(event);
   }

   private static void onModelBake(ModifyBakingResult event) {
      CoasterCartBottomSliceRenderer.cacheBottomModel(event);
      CoasterCartWheelAxisRenderer.cacheModels(event);
      CoasterCartItemModel.bakeCompositeItemModel(event);
   }

   private static void onRenderGuiLayerPost(Post event) {
      if (event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) {
         Minecraft mc = Minecraft.getInstance();
         if (BezierHandleDragManager.shouldShowBezierSnapControlHint()) {
            BezierHandleDragManager.renderBezierSnapControlHintHud(mc, event.getGuiGraphics());
         } else if (BezierHandleDragManager.shouldShowBezierEditAnchorStatusHud(mc)) {
            BezierHandleDragManager.renderBezierEditAnchorStatusHud(mc, event.getGuiGraphics());
         } else if (CoasterCartEditHud.shouldShow(mc)) {
            CoasterCartEditHud.render(mc, event.getGuiGraphics());
         } else if (CoasterTrackPlacementSmoothHud.shouldShow()) {
            CoasterTrackPlacementSmoothHud.render(mc, event.getGuiGraphics());
         } else if (RivetPlacementHud.shouldShow(mc)) {
            RivetPlacementHud.render(mc, event.getGuiGraphics());
         }
      }
   }

   private static void onClientTickPost(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      tickClearHotbarSelectionWhenHotbarChanges(mc);
      PickInteractionGates.clientTick(mc);
      BezierHandleEditMode.clientTick(mc);
      BezierHandleDragManager.clientTick(mc);
      BezierHandleCurvePreviewOutliner.clientTick(mc);
      ChainLiftAnchorOutlineClient.clientTick(mc);
      CoasterTrackPlacement.clientTick();
      CoasterTrackPlacementNeighborSmoothSuppressClient.clientTick(mc);
      CoasterWrenchCurveAnchorOutlineClient.clientTick(mc);
      CoasterCartLinkSnapClient.clientTick(mc);
      CoasterCartPlacementPreviewRenderer.clientTick(mc);
      CoasterCartChainLinkOutlineClient.clientTick(mc);
      CoasterCartEditMode.clientTick(mc);
      CoasterCartEditHud.clientTick(mc);
      RivetPlacementOutlineClient.clientTick(mc);
      RivetPlacementHud.clientTick(mc);
      ClientLevel var3 = mc.level;
      if (var3 instanceof ClientLevel) {
         RivetPlacementAnimationClient.clientTick(var3);
      } else {
         RivetPlacementAnimationClient.clientTick(null);
      }

      CoasterPathGraphManagerClient.clientTick();
      var3 = mc.level;
      if (var3 instanceof ClientLevel) {
         AnchorJunctionVisualRefresh.clientTickPost(var3);
         CoasterCartRailSoundController.clientTick(var3);
      }

      if (KineticDebugger.isF3DebugModeActive() && mc.level != null) {
         CoasterBezierPlacementSpineDebugRenderer.render(mc, mc.level);
      }
   }

   private static void onDebugOverlayText(DebugText event) {
      if (KineticDebugger.isF3DebugModeActive()) {
         int edgeCount = CoasterPathGraphDebugRenderer.getLastContinuityHighlightEdgeCount();
         List<Vec3> endpoints = CoasterPathGraphDebugRenderer.getLastContinuityTraceEndpointWorldPositions();
         event.getLeft().add(String.format("[Simulated Coasters] path trace edges: %d", edgeCount));
         if (edgeCount == 0) {
            event.getLeft().add("  trace endpoints: (aim crosshair at coaster anchor)");
         } else if (endpoints.isEmpty()) {
            event.getLeft().add("  trace endpoints: (none — closed loop or single vertex)");
         } else {
            event.getLeft().add(String.format("  trace endpoints (%d):", endpoints.size()));

            for (Vec3 p : endpoints) {
               event.getLeft().add(String.format("    %.4f %.4f %.4f", p.x, p.y, p.z));
            }
         }
      }
   }

   private static void onRenderLevelAfterBlockEntities(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_BLOCK_ENTITIES) {
         Minecraft mc = Minecraft.getInstance();
         ClientLevel poseStack = mc.level;
         float outlinePt = poseStack instanceof ClientLevel
            ? event.getPartialTick().getGameTimeDeltaPartialTick(poseStack.tickRateManager().runsNormally())
            : event.getPartialTick().getGameTimeDeltaPartialTick(false);
         CoasterCartPlacementPreviewRenderer.renderFrame(mc, outlinePt);
         CoasterCartHoverOutlineClient.renderFrame(mc, outlinePt);
         CoasterCartLinkSelectionOutlineRenderer.renderFrame(mc, outlinePt);
         CoasterCartChainLinkOutlineClient.renderFrame(mc, outlinePt);
         CoasterCartShearUnlinkOutlineClient.renderFrame(mc, outlinePt);
         CoasterCartEditOutlineClient.renderFrame(mc, outlinePt);
         BufferSource buffers = mc.renderBuffers().bufferSource();
         PoseStack poseStackx = event.getPoseStack();
         Camera camera = event.getCamera();
         DeltaTracker delta = event.getPartialTick();
         ClientLevel cl = mc.level;
         if (cl instanceof ClientLevel) {
            CoasterCartClientFrameCache.beginRenderFrame(cl, delta.getGameTimeDeltaPartialTick(cl.tickRateManager().runsNormally()));
         }

         CoasterCartWorldRenderer.render(mc, poseStackx, buffers, camera, delta);
         CoasterCartTrainCouplingRenderer.render(mc, poseStackx, buffers, camera, delta);
         ClientLevel var9 = mc.level;
         float partialTick = var9 instanceof ClientLevel
            ? delta.getGameTimeDeltaPartialTick(var9.tickRateManager().runsNormally())
            : delta.getGameTimeDeltaPartialTick(false);
         ChainLiftSpanRenderer.render(mc, poseStackx, buffers, camera, partialTick);
         BalloonTetherRenderer.render(mc, poseStackx, buffers, camera, partialTick);
         buffers.endBatch();
      }
   }

   private static void onClientLevelUnload(Unload event) {
      if (event.getLevel() instanceof ClientLevel cl) {
         CoasterCartClientMembership.clear();
         CoasterCartTrainLinkClientState.clearDimension(cl.dimension());
         CoasterCartEditMode.clear();
         CoasterCartEditHud.clearOnLevelUnload();
         RivetPlacementHud.clearOnLevelUnload();
         ChainLiftAnchorOutlineClient.clearOnLevelUnload();
         AnchorPeerCurvePendingRemoval.clear();
         ChainLiftClientIndex.clearAll();
         ChainLiftRibbonMeshCache.clearAll();
         RivetPlacementAnimationClient.clearAll();
      }
   }

   private static void onRenderWorldBezierHandles(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_LEVEL) {
         Minecraft mc = Minecraft.getInstance();
         PoseStack ms = event.getPoseStack();
         ms.pushPose();
         Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
         Matrix4f cameraRotation = event.getModelViewMatrix();
         BufferSource buf = mc.renderBuffers().bufferSource();
         BezierHandleOverlay.renderWrenchRangeCurves(mc, ms, buf, cam, cameraRotation);
         buf.endBatch(BezierHandleOverlayRenderTypes.SEE_THROUGH_LINES);
         BezierHandleOverlayRenderTypes.endTangentHandleBillboardBatches(buf);
         ms.popPose();
      }
   }

   private static void onRightClickBlockWrenchAnchor(RightClickBlock event) {
      if (event.getLevel().isClientSide()) {
         Level level = event.getLevel();
         BlockPos clicked = event.getPos();
         Player player = event.getEntity();
         if (CoasterCartEditMode.tryActivateFromInteract(level, player, clicked)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
         } else {
            BlockPos anchorTarget = null;
            if (level.getBlockEntity(clicked) instanceof CoasterAnchorpointBlockEntity) {
               anchorTarget = clicked;
            }

            if (anchorTarget != null && BezierHandleEditMode.tryActivateFromInteract(level, player, anchorTarget)) {
               event.setCanceled(true);
               event.setCancellationResult(InteractionResult.SUCCESS);
            }
         }
      }
   }

   private static void onUseKeyBeforeVanillaForBezierHandle(InteractionKeyMappingTriggered event) {
      if (event.isUseItem() && BezierHandleDragManager.shouldSuppressVanillaUse(Minecraft.getInstance())) {
         event.setCanceled(true);
         event.setSwingHand(false);
      }
   }
}
