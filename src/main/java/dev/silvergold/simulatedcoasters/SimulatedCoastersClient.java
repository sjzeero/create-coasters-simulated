/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu
 *  com.simibubi.create.content.kinetics.KineticDebugger
 *  dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer
 *  net.createmod.ponder.api.registration.PonderPlugin
 *  net.createmod.ponder.foundation.PonderIndex
 *  net.minecraft.client.Camera
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.ItemBlockRenderTypes
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleType
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.EventPriority
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.fml.IExtensionPoint
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.common.Mod
 *  net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
 *  net.neoforged.neoforge.client.event.ClientTickEvent$Post
 *  net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent$DebugText
 *  net.neoforged.neoforge.client.event.EntityRenderersEvent$RegisterRenderers
 *  net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered
 *  net.neoforged.neoforge.client.event.ModelEvent$ModifyBakingResult
 *  net.neoforged.neoforge.client.event.ModelEvent$RegisterAdditional
 *  net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
 *  net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
 *  net.neoforged.neoforge.client.event.RenderGuiLayerEvent$Post
 *  net.neoforged.neoforge.client.event.RenderLevelStageEvent
 *  net.neoforged.neoforge.client.event.RenderLevelStageEvent$Stage
 *  net.neoforged.neoforge.client.gui.ConfigurationScreen
 *  net.neoforged.neoforge.client.gui.IConfigScreenFactory
 *  net.neoforged.neoforge.client.gui.VanillaGuiLayers
 *  net.neoforged.neoforge.common.NeoForge
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  net.neoforged.neoforge.event.level.LevelEvent$Unload
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.joml.Matrix4f
 */
package dev.silvergold.simulatedcoasters;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu;
import com.simibubi.create.content.kinetics.KineticDebugger;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.silvergold.simulatedcoasters.PickInteractionGates;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.SimulatedCoastersParticles;
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
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

@Mod(value="simulatedcoasters", dist={Dist.CLIENT})
public class SimulatedCoastersClient {
    private static int lastHotbarSlotIndex = -1;

    public SimulatedCoastersClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (IExtensionPoint)((IConfigScreenFactory)ConfigurationScreen::new));
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
            return;
        }
        Inventory inv = player.getInventory();
        int sel = inv.selected;
        if (lastHotbarSlotIndex >= 0 && sel != lastHotbarSlotIndex) {
            boolean hadChainLinkSelection;
            ItemStack prev = inv.getItem(lastHotbarSlotIndex);
            boolean hadTrackSelection = CoasterTrackItem.hasPendingConnection(prev);
            boolean hadCartLinkSelection = CoasterCartItem.hasLinkSelection(prev);
            boolean bl = hadChainLinkSelection = prev.is(Items.CHAIN) && CoasterCartLinkSelection.hasLinkTarget(prev);
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
                PacketDistributor.sendToServer((CustomPacketPayload)new ClearCoasterTrackHotbarSelectionPayload(lastHotbarSlotIndex), (CustomPacketPayload[])new CustomPacketPayload[0]);
            }
        }
        lastHotbarSlotIndex = sel;
    }

    private static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer((BlockEntityType)SimulatedCoastersBlocks.COASTER_ANCHORPOINT_BE.get(), ChainLiftAnchorShaftRenderer::new);
    }

    private static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet((ParticleType)SimulatedCoastersParticles.TRACK_SNAP_SPARK.get(), TrackSnapSparkParticle.Provider::new);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track_placement"), CoasterTrackPlacementOverlayRenderer.OVERLAY);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            RadialWrenchMenu.registerBlacklistedBlock((ResourceLocation)SimulatedCoastersBlocks.COASTER_ANCHORPOINT.getId());
            PonderIndex.addPlugin((PonderPlugin)new CoastersPonderPlugin());
            OpenEndExtensionPreviewProvider.installClient(BezierHandleDragManager::openEndExtensionPreviewBlocks);
            RivetItemTooltips.register();
            BalloonItemTooltips.register();
            CoasterAnchorpointShaftModel.registerClientInit();
            ItemBlockRenderTypes.setRenderLayer((Block)((Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()), (RenderType)RenderType.cutout());
            SimpleBlockEntityVisualizer.builder((BlockEntityType)((BlockEntityType)SimulatedCoastersBlocks.COASTER_ANCHORPOINT_BE.get())).factory(AnchorPeerTrackCurveVisual::new).skipVanillaRender(be -> false).apply();
        });
    }

    private static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        CoasterCartBottomSliceRenderer.registerStandaloneModel(event);
        CoasterCartWheelAxisRenderer.registerStandaloneModels(event);
        CoasterCartItemModel.registerStandaloneModels(event);
        CoasterTrackCenterBeamModel.registerAdditional(event);
        CoasterTrackDyedBezierModels.registerAdditional(event);
        CoasterTrackAnchorJunctionModel.registerAdditional(event);
        CoasterTrackAnchorGirderModel.registerAdditional(event);
    }

    private static void onModelBake(ModelEvent.ModifyBakingResult event) {
        CoasterCartBottomSliceRenderer.cacheBottomModel(event);
        CoasterCartWheelAxisRenderer.cacheModels(event);
        CoasterCartItemModel.bakeCompositeItemModel(event);
    }

    private static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals((Object)VanillaGuiLayers.SELECTED_ITEM_NAME)) {
            return;
        }
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

    private static void onClientTickPost(ClientTickEvent.Post event) {
        ClientLevel cl;
        Minecraft mc = Minecraft.getInstance();
        SimulatedCoastersClient.tickClearHotbarSelectionWhenHotbarChanges(mc);
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
        ClientLevel clientLevel = mc.level;
        if (clientLevel instanceof ClientLevel) {
            cl = clientLevel;
            RivetPlacementAnimationClient.clientTick(cl);
        } else {
            RivetPlacementAnimationClient.clientTick(null);
        }
        CoasterPathGraphManagerClient.clientTick();
        clientLevel = mc.level;
        if (clientLevel instanceof ClientLevel) {
            cl = clientLevel;
            AnchorJunctionVisualRefresh.clientTickPost(cl);
            CoasterCartRailSoundController.clientTick(cl);
        }
        if (KineticDebugger.isF3DebugModeActive() && mc.level != null) {
            CoasterBezierPlacementSpineDebugRenderer.render(mc, (Level)mc.level);
        }
    }

    private static void onDebugOverlayText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!KineticDebugger.isF3DebugModeActive()) {
            return;
        }
        int edgeCount = CoasterPathGraphDebugRenderer.getLastContinuityHighlightEdgeCount();
        List<Vec3> endpoints = CoasterPathGraphDebugRenderer.getLastContinuityTraceEndpointWorldPositions();
        event.getLeft().add(String.format("[Simulated Coasters] path trace edges: %d", edgeCount));
        if (edgeCount == 0) {
            event.getLeft().add("  trace endpoints: (aim crosshair at coaster anchor)");
            return;
        }
        if (endpoints.isEmpty()) {
            event.getLeft().add("  trace endpoints: (none \u2014 closed loop or single vertex)");
            return;
        }
        event.getLeft().add(String.format("  trace endpoints (%d):", endpoints.size()));
        for (Vec3 p : endpoints) {
            event.getLeft().add(String.format("    %.4f %.4f %.4f", p.x, p.y, p.z));
        }
    }

    private static void onRenderLevelAfterBlockEntities(RenderLevelStageEvent event) {
        float f;
        float f2;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel clientLevel = mc.level;
        if (clientLevel instanceof ClientLevel) {
            ClientLevel cl = clientLevel;
            f2 = event.getPartialTick().getGameTimeDeltaPartialTick(cl.tickRateManager().runsNormally());
        } else {
            f2 = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        }
        float outlinePt = f2;
        CoasterCartPlacementPreviewRenderer.renderFrame(mc, outlinePt);
        CoasterCartHoverOutlineClient.renderFrame(mc, outlinePt);
        CoasterCartLinkSelectionOutlineRenderer.renderFrame(mc, outlinePt);
        CoasterCartChainLinkOutlineClient.renderFrame(mc, outlinePt);
        CoasterCartShearUnlinkOutlineClient.renderFrame(mc, outlinePt);
        CoasterCartEditOutlineClient.renderFrame(mc, outlinePt);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        DeltaTracker delta = event.getPartialTick();
        ClientLevel clientLevel2 = mc.level;
        if (clientLevel2 instanceof ClientLevel) {
            ClientLevel cl = clientLevel2;
            CoasterCartClientFrameCache.beginRenderFrame(cl, delta.getGameTimeDeltaPartialTick(cl.tickRateManager().runsNormally()));
        }
        CoasterCartWorldRenderer.render(mc, poseStack, buffers, camera, delta);
        CoasterCartTrainCouplingRenderer.render(mc, poseStack, buffers, camera, delta);
        ClientLevel clientLevel3 = mc.level;
        if (clientLevel3 instanceof ClientLevel) {
            ClientLevel cl = clientLevel3;
            f = delta.getGameTimeDeltaPartialTick(cl.tickRateManager().runsNormally());
        } else {
            f = delta.getGameTimeDeltaPartialTick(false);
        }
        float partialTick = f;
        ChainLiftSpanRenderer.render(mc, poseStack, buffers, camera, partialTick);
        BalloonTetherRenderer.render(mc, poseStack, buffers, camera, partialTick);
        buffers.endBatch();
    }

    private static void onClientLevelUnload(LevelEvent.Unload event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof ClientLevel) {
            ClientLevel cl = (ClientLevel)levelAccessor;
            CoasterCartClientMembership.clear();
            CoasterCartTrainLinkClientState.clearDimension((ResourceKey<Level>)cl.dimension());
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
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        PoseStack ms = event.getPoseStack();
        ms.pushPose();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Matrix4f cameraRotation = event.getModelViewMatrix();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        BezierHandleOverlay.renderWrenchRangeCurves(mc, ms, (MultiBufferSource)buf, cam, cameraRotation);
        buf.endBatch(BezierHandleOverlayRenderTypes.SEE_THROUGH_LINES);
        BezierHandleOverlayRenderTypes.endTangentHandleBillboardBatches(buf);
        ms.popPose();
    }

    private static void onRightClickBlockWrenchAnchor(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos clicked = event.getPos();
        Player player = event.getEntity();
        if (CoasterCartEditMode.tryActivateFromInteract(level, player, clicked)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        BlockPos anchorTarget = null;
        if (level.getBlockEntity(clicked) instanceof CoasterAnchorpointBlockEntity) {
            anchorTarget = clicked;
        }
        if (anchorTarget != null && BezierHandleEditMode.tryActivateFromInteract(level, player, anchorTarget)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void onUseKeyBeforeVanillaForBezierHandle(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || !BezierHandleDragManager.shouldSuppressVanillaUse(Minecraft.getInstance())) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
    }
}

