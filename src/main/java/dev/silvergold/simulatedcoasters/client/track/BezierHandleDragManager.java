/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  com.simibubi.create.AllKeys
 *  com.simibubi.create.AllSoundEvents
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.network.chat.TextColor
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.util.FastColor$ARGB32
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3f
 *  org.lwjgl.glfw.GLFW
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleEditMode;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorClientSpace;
import dev.silvergold.simulatedcoasters.track.BezierHandleEditPayload;
import dev.silvergold.simulatedcoasters.track.BezierLiftEditPayload;
import dev.silvergold.simulatedcoasters.track.BezierOpenEndExtensionEditPayload;
import dev.silvergold.simulatedcoasters.track.BezierTiltEditPayload;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.List;
import java.util.Objects;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

@OnlyIn(value=Dist.CLIENT)
public final class BezierHandleDragManager {
    public static final double HANDLE_EXTRA_REACH_BLOCKS = 22.0;
    private static final double MIN_VIEW_FORWARD = 0.3;
    private static final double MAX_VIEW_FORWARD = 24.0;
    private static final double DEFAULT_VIEW_FORWARD = 3.0;
    private static double handleDragTickSoundAccum;
    @Nullable
    private static Vec3 lastHandleDragSoundWorldPos;
    private static double lastHandleDragSoundScalar;
    private static final double HANDLE_DRAG_SOUND_BLOCKS_PER_TICK = 0.08;
    private static final double HANDLE_DRAG_SOUND_MIN_DELTA_BLOCKS = 0.002;
    private static boolean dragging;
    private static boolean draggingTilt;
    private static boolean draggingLift;
    private static boolean draggingOpenEndpoint;
    private static boolean useKeyWasDown;
    private static HandlePick hoverPick;
    private static TiltHover hoverTilt;
    private static LiftHover hoverLift;
    private static OpenEndpointHover hoverOpenEndpoint;
    private static BlockPos dragPrimaryHost;
    private static BlockPos dragRemoteEnd;
    private static int dragEndpointIndex;
    private static BlockPos dragEndpointPos;
    @Nullable
    private static String dragActiveEdgeKey;
    private static BlockPos dragTiltAnchorPos;
    private static BlockPos dragLiftAnchorPos;
    private static BlockPos dragOpenEndpointAnchorPos;
    private static float openEndpointPreviewExtensionBlocks;
    private static double dragViewForwardDistance;
    private static double lastCursorX;
    private static double lastCursorY;
    private static CoasterBezierHandleEdit.PreviewResult preview;
    private static CoasterBezierHandleEdit.PreviewResult tiltPreview;
    private static CoasterBezierHandleEdit.PreviewResult liftPreview;
    private static boolean suppressHandleGrabUntilUseRelease;

    private BezierHandleDragManager() {
    }

    static void onEditModeActivated() {
        suppressHandleGrabUntilUseRelease = true;
    }

    public static void clientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            BezierHandleDragManager.cancelInner(mc, true);
            return;
        }
        if (!BezierHandleEditMode.isActive()) {
            BezierHandleDragManager.cancelInner(mc, true);
            return;
        }
        if (!AllItems.WRENCH.isIn(mc.player.getMainHandItem()) && !AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
            BezierHandleDragManager.cancelInner(mc, true);
            return;
        }
        long window = mc.getWindow().getWindow();
        boolean useDown = mc.options.keyUse.isDown();
        double[] ax = new double[1];
        double[] ay = new double[1];
        GLFW.glfwGetCursorPos((long)window, (double[])ax, (double[])ay);
        double cx = ax[0];
        double cy = ay[0];
        if (suppressHandleGrabUntilUseRelease && !useDown) {
            suppressHandleGrabUntilUseRelease = false;
        }
        if (draggingOpenEndpoint) {
            lastCursorX = cx;
            lastCursorY = cy;
            BezierHandleDragManager.updateOpenEndpointPreviewFromVirtualTarget(mc);
            if (useKeyWasDown && !useDown) {
                BezierHandleDragManager.releaseCommit(mc);
            }
            useKeyWasDown = useDown;
            return;
        }
        if (draggingTilt) {
            lastCursorX = cx;
            lastCursorY = cy;
            BezierHandleDragManager.updateTiltPreviewFromVirtualTarget(mc);
            if (useKeyWasDown && !useDown) {
                BezierHandleDragManager.releaseCommit(mc);
            }
            useKeyWasDown = useDown;
            return;
        }
        if (draggingLift) {
            lastCursorX = cx;
            lastCursorY = cy;
            BezierHandleDragManager.updateLiftPreviewFromVirtualTarget(mc);
            if (useKeyWasDown && !useDown) {
                BezierHandleDragManager.releaseCommit(mc);
            }
            useKeyWasDown = useDown;
            return;
        }
        if (dragging) {
            lastCursorX = cx;
            lastCursorY = cy;
            BezierHandleDragManager.updatePreviewFromVirtualTarget(mc);
            if (useKeyWasDown && !useDown) {
                BezierHandleDragManager.releaseCommit(mc);
            }
            useKeyWasDown = useDown;
            return;
        }
        BezierHandleDragManager.resolveHover(mc);
        if (useDown && !useKeyWasDown && !suppressHandleGrabUntilUseRelease) {
            BezierHandleDragManager.tryBeginDrag(mc, cx, cy);
        }
        useKeyWasDown = useDown;
        lastCursorX = cx;
        lastCursorY = cy;
    }

    private static void resolveHover(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            hoverPick = null;
            hoverTilt = null;
            hoverLift = null;
            hoverOpenEndpoint = null;
            return;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 ro = mc.player.getEyePosition(pt);
        Vec3 rd = BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera());
        HandlePick hp = BezierHandleDragManager.rayPickClosestTangentHandle(mc);
        TiltHover th = BezierHandleDragManager.rayPickClosestTiltHandle(mc);
        LiftHover lh = BezierHandleDragManager.rayPickClosestLiftHandle(mc);
        OpenEndpointHover oe = BezierHandleDragManager.rayPickOpenEndpointHandle(mc);
        Double tHp = hp != null ? Double.valueOf(BezierHandleDragManager.raySphere(ro, rd, hp.handleTipWorld(), BezierHandleDragManager.pickRadiusWorld(ro, hp.handleTipWorld()))) : null;
        Double tTh = th != null ? Double.valueOf(BezierHandleDragManager.raySphere(ro, rd, th.tipWorld(), BezierHandleDragManager.pickRadiusWorld(ro, th.tipWorld()))) : null;
        Double tLh = lh != null ? Double.valueOf(BezierHandleDragManager.raySphere(ro, rd, lh.tipWorld(), BezierHandleDragManager.pickRadiusWorld(ro, lh.tipWorld()))) : null;
        Double tOe = oe != null ? Double.valueOf(BezierHandleDragManager.raySphere(ro, rd, oe.tipWorld(), BezierHandleDragManager.pickRadiusWorld(ro, oe.tipWorld()))) : null;
        double best = Double.POSITIVE_INFINITY;
        int winner = -1;
        if (tHp != null && tHp >= 0.0 && tHp < best) {
            best = tHp;
            winner = 0;
        }
        if (tTh != null && tTh >= 0.0 && tTh < best) {
            best = tTh;
            winner = 1;
        }
        if (tLh != null && tLh >= 0.0 && tLh < best) {
            best = tLh;
            winner = 2;
        }
        if (tOe != null && tOe >= 0.0 && tOe < best) {
            best = tOe;
            winner = 3;
        }
        hoverPick = winner == 0 ? hp : null;
        hoverTilt = winner == 1 ? th : null;
        hoverLift = winner == 2 ? lh : null;
        hoverOpenEndpoint = winner == 3 ? oe : null;
    }

    private static Vec3 cameraLookDir(Camera cam) {
        Vector3f lv = cam.getLookVector();
        return new Vec3((double)lv.x(), (double)lv.y(), (double)lv.z()).normalize();
    }

    private static double pickRadiusWorld(Vec3 eye, Vec3 tip) {
        double d = eye.distanceTo(tip);
        return Mth.clamp((double)Math.max(0.16, d * 0.052), (double)0.16, (double)2.25);
    }

    public static HandlePick rayPickClosestHandle(Minecraft mc) {
        return BezierHandleDragManager.rayPickClosestTangentHandle(mc);
    }

    public static TiltHover getHoverTilt() {
        return hoverTilt;
    }

    public static LiftHover getHoverLift() {
        return hoverLift;
    }

    @Nullable
    public static LiftHover rayPickClosestLiftHandle(Minecraft mc) {
        Vec3 face;
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return null;
        }
        BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
        if (editAnchor == null) {
            return null;
        }
        if (!CoasterBezierHandleEdit.anchorEligibleForLiftEdit((Level)level, editAnchor)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(editAnchor);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 ro = mc.player.getEyePosition(pt);
        Vec3 rd = BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera());
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves((Level)level, be);
        if (incident.isEmpty()) {
            return null;
        }
        Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), editAnchor);
        if (t == null) {
            return null;
        }
        Vec3 center = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, editAnchor, CoasterAnchorpointBlockEntity.worldCenter((Level)level, editAnchor));
        Vec3 tip = center.add((face = CoasterAnchorClientSpace.toRenderDirection((Level)level, editAnchor, CoasterTrackPlacement.anchorFaceNormalForAxis((Level)level, editAnchor, t))).scale((double)1.4f));
        double tRay = BezierHandleDragManager.raySphere(ro, rd, tip, BezierHandleDragManager.pickRadiusWorld(ro, tip));
        if (tRay < 0.0) {
            return null;
        }
        return new LiftHover(editAnchor.immutable(), tip);
    }

    @Nullable
    public static TiltHover rayPickClosestTiltHandle(Minecraft mc) {
        Vec3 face;
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return null;
        }
        BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
        if (editAnchor == null) {
            return null;
        }
        if (!CoasterBezierHandleEdit.anchorEligibleForTiltEdit((Level)level, editAnchor)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(editAnchor);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 ro = mc.player.getEyePosition(pt);
        Vec3 rd = BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera());
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves((Level)level, be);
        if (incident.isEmpty()) {
            return null;
        }
        Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), editAnchor);
        if (t == null) {
            return null;
        }
        Vec3 center = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, editAnchor, CoasterAnchorpointBlockEntity.worldCenter((Level)level, editAnchor));
        Vec3 tip = center.add((face = CoasterAnchorClientSpace.toRenderDirection((Level)level, editAnchor, CoasterTrackPlacement.anchorFaceNormalForAxis((Level)level, editAnchor, t))).scale(0.75));
        double tRay = BezierHandleDragManager.raySphere(ro, rd, tip, BezierHandleDragManager.pickRadiusWorld(ro, tip));
        if (tRay < 0.0) {
            return null;
        }
        return new TiltHover(editAnchor.immutable(), tip);
    }

    private static HandlePick rayPickClosestTangentHandle(Minecraft mc) {
        Camera cam = mc.gameRenderer.getMainCamera();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return null;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 ro = mc.player.getEyePosition(pt);
        Vec3 rd = BezierHandleDragManager.cameraLookDir(cam);
        double bestT = Double.MAX_VALUE;
        BlockPos bestHost = null;
        BlockPos bestRemote = null;
        int bestEp = -1;
        Vec3 bestTip = null;
        BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
        if (editAnchor == null) {
            return null;
        }
        for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
            BlockPos here;
            if (anchor.getLevel() != level || anchor.isRemoved() || !editAnchor.equals((Object)(here = anchor.getBlockPos()))) continue;
            for (BezierConnection bcRaw : anchor.getAnchorPeerCurvesView().values()) {
                double tSecond;
                BezierConnection bc;
                if (bcRaw == null) continue;
                BezierConnection bezierConnection = bc = bcRaw.isPrimary() ? bcRaw : bcRaw.secondary();
                if (!bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) continue;
                Vec3 e1 = (Vec3)bc.starts.getFirst();
                Vec3 e2 = (Vec3)bc.starts.getSecond();
                Vec3 f1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
                Vec3 f2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
                BlockPos storageHost = (BlockPos)bc.bePositions.getFirst();
                BlockPos remote = bc.getKey();
                Vec3 f1Hit = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, here, f1);
                Vec3 f2Hit = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, here, f2);
                double tFirst = BezierHandleDragManager.raySphere(ro, rd, f1Hit, BezierHandleDragManager.pickRadiusWorld(ro, f1Hit));
                if (tFirst >= 0.0 && tFirst < bestT) {
                    bestT = tFirst;
                    bestHost = storageHost;
                    bestRemote = remote;
                    bestEp = 0;
                    bestTip = f1Hit;
                }
                if (!((tSecond = BezierHandleDragManager.raySphere(ro, rd, f2Hit, BezierHandleDragManager.pickRadiusWorld(ro, f2Hit))) >= 0.0) || !(tSecond < bestT)) continue;
                bestT = tSecond;
                bestHost = storageHost;
                bestRemote = remote;
                bestEp = 1;
                bestTip = f2Hit;
            }
        }
        if (bestHost == null || bestEp < 0 || bestTip == null) {
            return null;
        }
        return new HandlePick(bestHost.immutable(), bestRemote.immutable(), bestEp, bestTip);
    }

    @Nullable
    private static HandlePick maybeSwitchEditAnchorForNeighborTangentHandleThenRefreshPick(Minecraft mc, Level level, @Nullable HandlePick hp) {
        BlockPos ep;
        if (hp == null || !BezierHandleEditMode.isActive()) {
            return hp;
        }
        BlockPos active = BezierHandleEditMode.getActiveAnchor();
        if (active == null) {
            return hp;
        }
        BezierConnection bc = BezierHandleDragManager.loadPrimary(level, hp.primaryHost(), hp.remoteEnd());
        if (bc == null) {
            return hp;
        }
        int epi = hp.endpointIndex();
        BlockPos blockPos = ep = epi == 0 ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
        if (active.equals((Object)ep)) {
            return hp;
        }
        BezierHandleEditMode.switchActiveAnchorTo(ep, level);
        HandlePick hp2 = BezierHandleDragManager.rayPickClosestTangentHandle(mc);
        return hp2 != null ? hp2 : hp;
    }

    private static void tryBeginDrag(Minecraft mc, double cx, double cy) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 ro = mc.player.getEyePosition(pt);
        Vec3 rd = BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera());
        HandlePick hp = BezierHandleDragManager.rayPickClosestTangentHandle(mc);
        hp = BezierHandleDragManager.maybeSwitchEditAnchorForNeighborTangentHandleThenRefreshPick(mc, (Level)level, hp);
        TiltHover th = BezierHandleDragManager.rayPickClosestTiltHandle(mc);
        LiftHover lh = BezierHandleDragManager.rayPickClosestLiftHandle(mc);
        OpenEndpointHover oe = BezierHandleDragManager.rayPickOpenEndpointHandle(mc);
        Double tHp = hp != null ? Double.valueOf(BezierHandleDragManager.raySphere(ro, rd, hp.handleTipWorld(), BezierHandleDragManager.pickRadiusWorld(ro, hp.handleTipWorld()))) : null;
        Double tTh = th != null ? Double.valueOf(BezierHandleDragManager.raySphere(ro, rd, th.tipWorld(), BezierHandleDragManager.pickRadiusWorld(ro, th.tipWorld()))) : null;
        Double tLh = lh != null ? Double.valueOf(BezierHandleDragManager.raySphere(ro, rd, lh.tipWorld(), BezierHandleDragManager.pickRadiusWorld(ro, lh.tipWorld()))) : null;
        Double tOe = oe != null ? Double.valueOf(BezierHandleDragManager.raySphere(ro, rd, oe.tipWorld(), BezierHandleDragManager.pickRadiusWorld(ro, oe.tipWorld()))) : null;
        double best = Double.POSITIVE_INFINITY;
        int winner = -1;
        if (tHp != null && tHp >= 0.0 && tHp < best) {
            best = tHp;
            winner = 0;
        }
        if (tTh != null && tTh >= 0.0 && tTh < best) {
            best = tTh;
            winner = 1;
        }
        if (tLh != null && tLh >= 0.0 && tLh < best) {
            best = tLh;
            winner = 2;
        }
        if (tOe != null && tOe >= 0.0 && tOe < best) {
            best = tOe;
            winner = 3;
        }
        if (winner == 1 && th != null) {
            BezierHandleDragManager.tryBeginTiltDrag(mc, th);
            return;
        }
        if (winner == 2 && lh != null) {
            BezierHandleDragManager.tryBeginLiftDrag(mc, lh);
            return;
        }
        if (winner == 3 && oe != null) {
            BezierHandleDragManager.tryBeginOpenEndpointDrag(mc, oe);
            return;
        }
        if (hp == null) {
            return;
        }
        BlockPos bestHost = hp.primaryHost();
        BlockPos bestRemote = hp.remoteEnd();
        int bestEp = hp.endpointIndex();
        BezierConnection bc = BezierHandleDragManager.loadPrimary((Level)level, bestHost, bestRemote);
        if (bc == null) {
            return;
        }
        BlockPos ep = bestEp == 0 ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
        Vec3 fwd = mc.player.getViewVector(pt);
        fwd = fwd.lengthSqr() < 1.0E-12 ? BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera()) : fwd.normalize();
        Vec3 handleTip = hp.handleTipWorld();
        double along = handleTip.subtract(ro).dot(fwd);
        if (along < 0.3) {
            along = 3.0;
        }
        dragViewForwardDistance = Mth.clamp((double)along, (double)0.3, (double)24.0);
        dragging = true;
        hoverPick = null;
        hoverTilt = null;
        dragPrimaryHost = bestHost.immutable();
        dragRemoteEnd = bestRemote.immutable();
        dragEndpointIndex = bestEp;
        dragEndpointPos = ep.immutable();
        dragActiveEdgeKey = BezierHandleDragManager.edgeKey(bc);
        BezierHandleDragManager.resetHandleDragSoundSample();
        BezierHandleDragManager.updatePreviewFromVirtualTarget(mc);
        lastCursorX = cx;
        lastCursorY = cy;
    }

    private static void tryBeginTiltDrag(Minecraft mc, TiltHover th) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 ro = mc.player.getEyePosition(pt);
        Vec3 fwd = mc.player.getViewVector(pt);
        fwd = fwd.lengthSqr() < 1.0E-12 ? BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera()) : fwd.normalize();
        double along = th.tipWorld().subtract(ro).dot(fwd);
        if (along < 0.3) {
            along = 3.0;
        }
        dragViewForwardDistance = Mth.clamp((double)along, (double)0.3, (double)24.0);
        draggingTilt = true;
        hoverPick = null;
        hoverTilt = null;
        hoverLift = null;
        dragTiltAnchorPos = th.anchorPos().immutable();
        BezierHandleDragManager.resetHandleDragSoundSample();
        BezierHandleDragManager.updateTiltPreviewFromVirtualTarget(mc);
    }

    private static void tryBeginLiftDrag(Minecraft mc, LiftHover lh) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 ro = mc.player.getEyePosition(pt);
        Vec3 fwd = mc.player.getViewVector(pt);
        fwd = fwd.lengthSqr() < 1.0E-12 ? BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera()) : fwd.normalize();
        double along = lh.tipWorld().subtract(ro).dot(fwd);
        if (along < 0.3) {
            along = 3.0;
        }
        dragViewForwardDistance = Mth.clamp((double)along, (double)0.3, (double)24.0);
        draggingLift = true;
        hoverPick = null;
        hoverTilt = null;
        hoverLift = null;
        hoverOpenEndpoint = null;
        dragLiftAnchorPos = lh.anchorPos().immutable();
        BezierHandleDragManager.resetHandleDragSoundSample();
        BezierHandleDragManager.updateLiftPreviewFromVirtualTarget(mc);
    }

    private static void tryBeginOpenEndpointDrag(Minecraft mc, OpenEndpointHover oe) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 ro = mc.player.getEyePosition(pt);
        Vec3 fwd = mc.player.getViewVector(pt);
        fwd = fwd.lengthSqr() < 1.0E-12 ? BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera()) : fwd.normalize();
        double along = oe.tipWorld().subtract(ro).dot(fwd);
        if (along < 0.3) {
            along = 3.0;
        }
        dragViewForwardDistance = Mth.clamp((double)along, (double)0.3, (double)24.0);
        draggingOpenEndpoint = true;
        hoverPick = null;
        hoverTilt = null;
        hoverLift = null;
        hoverOpenEndpoint = null;
        dragOpenEndpointAnchorPos = oe.anchorPos().immutable();
        openEndpointPreviewExtensionBlocks = Float.NaN;
        BezierHandleDragManager.resetHandleDragSoundSample();
        BezierHandleDragManager.updateOpenEndpointPreviewFromVirtualTarget(mc);
    }

    @Nullable
    private static OpenEndpointHover rayPickOpenEndpointHandle(Minecraft mc) {
        float pt;
        BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
        ClientLevel level = mc.level;
        if (editAnchor == null || level == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(editAnchor);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        if (be.legCount() != 1) {
            return null;
        }
        BezierConnection bc = CoasterOpenEndExtension.primaryCoasterCurveAt((Level)level, editAnchor);
        if (bc == null) {
            return null;
        }
        String ek = BezierHandleDragManager.edgeKey(bc);
        BezierConnection draw = BezierHandleDragManager.previewCurveOrWorld(ek, bc);
        Vec3 handle = BezierHandleDragManager.openEndpointHandleRenderWorld((Level)level, editAnchor, draw, pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level));
        if (handle == null) {
            return null;
        }
        return new OpenEndpointHover(editAnchor.immutable(), handle);
    }

    @Nullable
    static Vec3 openEndpointHandleRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc, float partialTick) {
        return BezierHandleDragManager.openEndpointTipRenderWorld(level, anchorPos, bc, partialTick);
    }

    @Nullable
    static Vec3 openEndpointHandleRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc) {
        return BezierHandleDragManager.openEndpointHandleRenderWorld(level, anchorPos, bc, AnimationTickHolder.getPartialTicks((LevelAccessor)level));
    }

    @Nullable
    static Vec3 openEndpointJunctionRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc, float partialTick) {
        Vec3 junctionPlot = CoasterOpenEndExtension.junctionPlotLocal(bc, anchorPos);
        return CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, anchorPos, junctionPlot, partialTick);
    }

    @Nullable
    static Vec3 openEndpointJunctionRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc) {
        return BezierHandleDragManager.openEndpointJunctionRenderWorld(level, anchorPos, bc, AnimationTickHolder.getPartialTicks((LevelAccessor)level));
    }

    @Nullable
    static Vec3 openEndpointTipRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc, float partialTick) {
        float ext = BezierHandleDragManager.openEndExtensionPreviewBlocks(anchorPos) != null ? BezierHandleDragManager.openEndExtensionPreviewBlocks(anchorPos).floatValue() : CoasterOpenEndExtension.extensionBlocks(level, anchorPos);
        Vec3 tipPlot = CoasterOpenEndExtension.tipPlotLocal(bc, anchorPos, ext);
        return CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, anchorPos, tipPlot, partialTick);
    }

    @Nullable
    static Vec3 openEndpointTipRenderWorld(Level level, BlockPos anchorPos, BezierConnection bc) {
        return BezierHandleDragManager.openEndpointTipRenderWorld(level, anchorPos, bc, AnimationTickHolder.getPartialTicks((LevelAccessor)level));
    }

    @Nullable
    public static Float openEndExtensionPreviewBlocks(BlockPos anchorPos) {
        if (!draggingOpenEndpoint || dragOpenEndpointAnchorPos == null || !dragOpenEndpointAnchorPos.equals((Object)anchorPos)) {
            return null;
        }
        return Float.isNaN(openEndpointPreviewExtensionBlocks) ? null : Float.valueOf(openEndpointPreviewExtensionBlocks);
    }

    @Nullable
    public static BlockPos openEndpointDragAnchor() {
        return draggingOpenEndpoint ? dragOpenEndpointAnchorPos : null;
    }

    @Nullable
    static Vec3 curveStartAtAnchor(BezierConnection bc, BlockPos anchorPos) {
        if (((BlockPos)bc.bePositions.getFirst()).equals((Object)anchorPos)) {
            return (Vec3)bc.starts.getFirst();
        }
        if (((BlockPos)bc.bePositions.getSecond()).equals((Object)anchorPos)) {
            return (Vec3)bc.starts.getSecond();
        }
        return null;
    }

    private static BezierConnection loadPrimary(Level level, BlockPos host, BlockPos remote) {
        BlockEntity be = level.getBlockEntity(host);
        if (be instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity ape = (CoasterAnchorpointBlockEntity)be;
            BezierConnection c = ape.getAnchorPeerCurvesView().get(remote);
            return c != null && c.isPrimary() ? c : null;
        }
        return null;
    }

    private static void updatePreviewFromVirtualTarget(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || dragEndpointPos == null || mc.player == null) {
            return;
        }
        Vec3 raw = BezierHandleDragManager.computeTangentDragRawEyeTarget(mc);
        if (raw == null) {
            return;
        }
        CoasterBezierHandleEdit.TangentGuideSnapResult snap = BezierHandleDragManager.computeTangentGuideSnap(mc, raw);
        Vec3 virtualTarget = snap.snappedVirtualTarget();
        if (mc.player != null) {
            BezierHandleDragManager.showTangentGuideAlignmentActionBar(mc, snap.guideAlignmentAxes());
        }
        Vec3 handleRoot = CoasterAnchorpointBlockEntity.worldCenter((Level)level, dragEndpointPos);
        Vec3 handleOffset = virtualTarget.subtract(handleRoot);
        preview = CoasterBezierHandleEdit.computePreview((Level)level, dragPrimaryHost, dragRemoteEnd, dragEndpointIndex, handleOffset);
        BezierHandleDragManager.tickHandleDragSoundFromWorldPos(mc, BezierHandleDragManager.tangentDragHandleTipRenderWorld(mc));
    }

    @Nullable
    private static Vec3 tangentDragHandleTipRenderWorld(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || preview == null || dragEndpointPos == null || dragActiveEdgeKey == null) {
            return null;
        }
        for (BezierConnection c : preview.allUpdated()) {
            if (!BezierHandleDragManager.edgeKey(c).equals(dragActiveEdgeKey)) continue;
            BezierConnection u = c.isPrimary() ? c : c.secondary();
            Vec3 start = dragEndpointIndex == 0 ? (Vec3)u.starts.getFirst() : (Vec3)u.starts.getSecond();
            Vec3 axis = dragEndpointIndex == 0 ? (Vec3)u.axes.getFirst() : (Vec3)u.axes.getSecond();
            double len = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(u, dragEndpointIndex);
            Vec3 tipPlot = axis.lengthSqr() < 1.0E-12 ? start : start.add(axis.normalize().scale(len));
            float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
            return CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, dragEndpointPos, tipPlot, pt);
        }
        return null;
    }

    @Nullable
    private static Vec3 computeTangentDragRawEyeTarget(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return null;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 eye = mc.player.getEyePosition(pt);
        Vec3 fwd = mc.player.getViewVector(pt);
        fwd = fwd.lengthSqr() < 1.0E-12 ? BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera()) : fwd.normalize();
        return eye.add(fwd.scale(dragViewForwardDistance));
    }

    private static CoasterBezierHandleEdit.TangentGuideSnapResult computeTangentGuideSnap(Minecraft mc, Vec3 rawEyeTarget) {
        ClientLevel level = mc.level;
        if (level == null || dragPrimaryHost == null || dragRemoteEnd == null || dragEndpointPos == null) {
            return new CoasterBezierHandleEdit.TangentGuideSnapResult(rawEyeTarget, 0);
        }
        Vec3 plotTarget = CoasterAnchorClientSpace.toPlotLocal((Level)level, dragEndpointPos, rawEyeTarget);
        return CoasterBezierHandleEdit.snapBezierTangentVirtualTargetWithGuidePlanes((Level)level, dragPrimaryHost, dragRemoteEnd, dragEndpointIndex, plotTarget, !BezierHandleDragManager.isCreateAltFineDown(), 0.3);
    }

    private static void updateTiltPreviewFromVirtualTarget(Minecraft mc) {
        Vec3 virtualTarget = BezierHandleDragManager.tiltDragEyeVirtualTarget(mc);
        if (virtualTarget == null) {
            return;
        }
        ClientLevel level = mc.level;
        tiltPreview = CoasterBezierHandleEdit.computeTiltPreview((Level)level, dragTiltAnchorPos, virtualTarget);
        Double theta = CoasterBezierHandleEdit.computeJointBankRadiansFromVirtualTarget((Level)level, dragTiltAnchorPos, virtualTarget);
        if (theta != null) {
            BezierHandleDragManager.tickHandleDragSoundFromScalar(mc, theta);
            if (mc.player != null && tiltPreview != null && tiltPreview.valid()) {
                BezierHandleDragManager.showLiveTiltAngleReadout(mc, theta);
            }
        }
    }

    private static void showLiveLiftReadout(Minecraft mc, float liftBlocksMeters) {
        if (mc.player == null) {
            return;
        }
        mc.player.displayClientMessage((Component)CoasterBezierHandleEdit.liftMetersComponent(liftBlocksMeters).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)0xFFFFFF))), true);
    }

    private static void showLiveOpenEndExtensionReadout(Minecraft mc, float extensionBlocks) {
        if (mc.player == null) {
            return;
        }
        mc.player.displayClientMessage((Component)CoasterBezierHandleEdit.openEndExtensionBlocksComponent(extensionBlocks).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)0xFFFFFF))), true);
    }

    private static void showLiveTiltAngleReadout(Minecraft mc, double jointBankRadians) {
        if (mc.player == null) {
            return;
        }
        mc.player.displayClientMessage((Component)CoasterBezierHandleEdit.jointBankDegreesComponent(jointBankRadians).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)0xFFFFFF))), true);
    }

    private static void showTangentGuideAlignmentActionBar(Minecraft mc, int guideAlignmentAxes) {
        int rgb;
        String key;
        if (mc.player == null) {
            return;
        }
        int blueRgb = 11781103;
        int whiteRgb = 0xFFFFFF;
        if (guideAlignmentAxes >= 2) {
            key = "simulatedcoasters.track.bezier_edit_tangent_handle_aligned_2_axis";
            rgb = whiteRgb;
        } else if (guideAlignmentAxes == 1) {
            key = "simulatedcoasters.track.bezier_edit_tangent_handle_aligned_1_axis";
            rgb = whiteRgb;
        } else {
            key = "simulatedcoasters.track.bezier_edit_tangent_handle_unaligned";
            rgb = blueRgb;
        }
        mc.player.displayClientMessage((Component)Component.translatable((String)key).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)rgb))), true);
    }

    public static boolean shouldShowBezierSnapControlHint() {
        return draggingLift && liftPreview != null && liftPreview.valid() || draggingTilt && tiltPreview != null && tiltPreview.valid() || dragging && preview != null && preview.valid() || draggingOpenEndpoint && dragOpenEndpointAnchorPos != null && !Float.isNaN(openEndpointPreviewExtensionBlocks);
    }

    public static boolean shouldShowBezierEditAnchorStatusHud(Minecraft mc) {
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return false;
        }
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return false;
        }
        if (!BezierHandleEditMode.isActive()) {
            return false;
        }
        if (!AllItems.WRENCH.isIn(mc.player.getMainHandItem()) && !AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
            return false;
        }
        if (BezierHandleDragManager.shouldShowBezierSnapControlHint()) {
            return false;
        }
        if (dragging || draggingTilt || draggingLift) {
            return false;
        }
        BlockPos ap = BezierHandleEditMode.getActiveAnchor();
        return ap != null && mc.level != null && mc.level.getBlockEntity(ap) instanceof CoasterAnchorpointBlockEntity;
    }

    public static void renderBezierEditAnchorStatusHud(Minecraft mc, GuiGraphics guiGraphics) {
        BlockEntity blockEntity;
        if (!BezierHandleDragManager.shouldShowBezierEditAnchorStatusHud(mc)) {
            return;
        }
        BlockPos ap = BezierHandleEditMode.getActiveAnchor();
        if (ap == null || mc.level == null || !((blockEntity = mc.level.getBlockEntity(ap)) instanceof CoasterAnchorpointBlockEntity)) {
            return;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        int whiteRgb = 0xFFFFFF;
        int yellowRgb = 0xFFDD55;
        Style styleWhite = Style.EMPTY.withColor(TextColor.fromRgb((int)whiteRgb));
        Style styleCurvature = Style.EMPTY.withColor(TextColor.fromRgb((int)(hoverPick != null ? yellowRgb : whiteRgb)));
        Style styleTilt = Style.EMPTY.withColor(TextColor.fromRgb((int)(hoverTilt != null ? yellowRgb : whiteRgb)));
        Style styleLift = Style.EMPTY.withColor(TextColor.fromRgb((int)(hoverLift != null ? yellowRgb : whiteRgb)));
        String tiltStr = CoasterBezierHandleEdit.formatJointBankDegreesReadout(be.getJointBankRadians());
        String liftStr = CoasterBezierHandleEdit.formatLiftMetersReadout(be.getLiftBlocks());
        MutableComponent line = Component.empty().append((Component)Component.translatable((String)"simulatedcoasters.track.bezier_edit_anchor_status_curvature").withStyle(styleCurvature)).append((Component)Component.translatable((String)"simulatedcoasters.track.bezier_edit_anchor_status_sep").withStyle(styleWhite)).append((Component)Component.translatable((String)"simulatedcoasters.track.bezier_edit_anchor_status_tilt", (Object[])new Object[]{tiltStr}).withStyle(styleTilt)).append((Component)Component.translatable((String)"simulatedcoasters.track.bezier_edit_anchor_status_sep").withStyle(styleWhite)).append((Component)Component.translatable((String)"simulatedcoasters.track.bezier_edit_anchor_status_lift", (Object[])new Object[]{liftStr}).withStyle(styleLift));
        Font font = mc.font;
        int w = font.width((FormattedText)line);
        int x = (guiGraphics.guiWidth() - w) / 2;
        int y = BezierHandleDragManager.bezierEditHudRowAboveHotbarY(mc, guiGraphics);
        int backdropTint = FastColor.ARGB32.color((int)255, (int)whiteRgb);
        guiGraphics.drawStringWithBackdrop(font, (Component)line, x, y, w, backdropTint);
    }

    public static int editHudRowAboveHotbarY(Minecraft mc, GuiGraphics guiGraphics) {
        return BezierHandleDragManager.bezierEditHudRowAboveHotbarY(mc, guiGraphics);
    }

    private static int bezierEditHudRowAboveHotbarY(Minecraft mc, GuiGraphics guiGraphics) {
        int yShift = Math.max(mc.gui.leftHeight, mc.gui.rightHeight);
        int y = guiGraphics.guiHeight() - Math.max(yShift, 59);
        Objects.requireNonNull(mc.font);
        return (y += 14) - 9 - 2;
    }

    public static void renderBezierSnapControlHintHud(Minecraft mc, GuiGraphics guiGraphics) {
        if (!BezierHandleDragManager.shouldShowBezierSnapControlHint() || mc.player == null || mc.options.hideGui) {
            return;
        }
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        int y = BezierHandleDragManager.bezierEditHudRowAboveHotbarY(mc, guiGraphics);
        int blueRgb = 11781103;
        int keyRgb = BezierHandleDragManager.isCreateAltFineDown() ? 0xFFFFFF : blueRgb;
        Style keyStyle = Style.EMPTY.withColor(TextColor.fromRgb((int)keyRgb));
        MutableComponent bracketedKey = Component.literal((String)"[").withStyle(keyStyle).append((Component)Component.literal((String)AllKeys.ALT_MODIFIER.getBoundKey()).withStyle(keyStyle)).append((Component)Component.literal((String)"]").withStyle(keyStyle));
        MutableComponent hint = Component.translatable((String)"simulatedcoasters.track.bezier_edit_lift_snap_hint", (Object[])new Object[]{bracketedKey}).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)blueRgb)));
        Font font = mc.font;
        int w = font.width((FormattedText)hint);
        int x = (guiGraphics.guiWidth() - w) / 2;
        int color = FastColor.ARGB32.color((int)255, (int)11781103);
        guiGraphics.drawStringWithBackdrop(font, (Component)hint, x, y, w, color);
    }

    private static void updateLiftPreviewFromVirtualTarget(Minecraft mc) {
        Vec3 virtualTarget = BezierHandleDragManager.liftDragEyeVirtualTarget(mc);
        if (virtualTarget == null) {
            return;
        }
        ClientLevel level = mc.level;
        liftPreview = CoasterBezierHandleEdit.computeLiftPreview((Level)level, dragLiftAnchorPos, virtualTarget);
        Float w = CoasterBezierHandleEdit.computeLiftBlocksFromVirtualTarget((Level)level, dragLiftAnchorPos, virtualTarget);
        if (w != null) {
            BezierHandleDragManager.tickHandleDragSoundFromScalar(mc, w.floatValue());
            if (mc.player != null && liftPreview != null && liftPreview.valid()) {
                BezierHandleDragManager.showLiveLiftReadout(mc, w.floatValue());
            }
        }
    }

    private static void updateOpenEndpointPreviewFromVirtualTarget(Minecraft mc) {
        Vec3 virtualTarget = BezierHandleDragManager.openEndpointDragVirtualTarget(mc);
        if (virtualTarget == null) {
            return;
        }
        ClientLevel level = mc.level;
        if (level == null || dragOpenEndpointAnchorPos == null) {
            return;
        }
        CoasterBezierHandleEdit.OpenEndExtensionPreviewResult pr = CoasterBezierHandleEdit.computeOpenEndExtensionPreview((Level)level, dragOpenEndpointAnchorPos, virtualTarget, false);
        if (!pr.valid()) {
            return;
        }
        openEndpointPreviewExtensionBlocks = pr.extensionBlocks();
        if (mc.player != null) {
            BezierHandleDragManager.showLiveOpenEndExtensionReadout(mc, pr.extensionBlocks());
        }
        BezierHandleDragManager.tickHandleDragSoundFromScalar(mc, pr.extensionBlocks());
    }

    @Nullable
    private static Vec3 openEndpointDragVirtualTarget(Minecraft mc) {
        Vec3 eyeRayTarget = BezierHandleDragManager.openEndpointDragEyeVirtualTargetPlotLocal(mc);
        if (eyeRayTarget == null || mc.level == null || dragOpenEndpointAnchorPos == null) {
            return null;
        }
        return BezierHandleDragManager.openEndDragVirtualTarget((Level)mc.level, dragOpenEndpointAnchorPos, eyeRayTarget);
    }

    private static Vec3 openEndDragVirtualTarget(Level level, BlockPos anchorPos, Vec3 eyeRayTargetPlotLocal) {
        if (BezierHandleDragManager.isCreateAltFineDown()) {
            return eyeRayTargetPlotLocal;
        }
        BezierConnection bc = CoasterOpenEndExtension.primaryCoasterCurveAt(level, anchorPos);
        if (bc == null) {
            return eyeRayTargetPlotLocal;
        }
        Float raw = CoasterOpenEndExtension.extensionBlocksFromVirtualTarget(level, anchorPos, bc, eyeRayTargetPlotLocal, false);
        if (raw == null) {
            return eyeRayTargetPlotLocal;
        }
        float snapped = CoasterOpenEndExtension.snapExtensionBlocksToCenter(raw.floatValue());
        return CoasterOpenEndExtension.virtualTargetPlotLocalForExtensionBlocks(bc, anchorPos, eyeRayTargetPlotLocal, snapped);
    }

    @Nullable
    private static Vec3 openEndpointDragEyeVirtualTargetPlotLocal(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || dragOpenEndpointAnchorPos == null || mc.player == null) {
            return null;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 eye = mc.player.getEyePosition(pt);
        Vec3 fwd = mc.player.getViewVector(pt);
        fwd = fwd.lengthSqr() < 1.0E-12 ? BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera()) : fwd.normalize();
        Vec3 eyeRayTargetRender = eye.add(fwd.scale(dragViewForwardDistance));
        return CoasterAnchorClientSpace.toPlotLocal((Level)level, dragOpenEndpointAnchorPos, eyeRayTargetRender);
    }

    @Nullable
    private static Vec3 liftDragEyeVirtualTarget(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || dragLiftAnchorPos == null || mc.player == null) {
            return null;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 eye = mc.player.getEyePosition(pt);
        Vec3 fwd = mc.player.getViewVector(pt);
        fwd = fwd.lengthSqr() < 1.0E-12 ? BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera()) : fwd.normalize();
        Vec3 eyeRayTarget = eye.add(fwd.scale(dragViewForwardDistance));
        return BezierHandleDragManager.liftDragVirtualTarget(mc, (Level)level, eyeRayTarget);
    }

    private static void resetHandleDragSoundSample() {
        lastHandleDragSoundWorldPos = null;
        lastHandleDragSoundScalar = Double.NaN;
        handleDragTickSoundAccum = 0.0;
    }

    private static void tickHandleDragSoundFromWorldPos(Minecraft mc, @Nullable Vec3 currentWorld) {
        if (mc.player == null || mc.level == null || currentWorld == null) {
            return;
        }
        if (lastHandleDragSoundWorldPos == null) {
            lastHandleDragSoundWorldPos = currentWorld;
            return;
        }
        BezierHandleDragManager.accumulateHandleDragSoundFromMovement(mc, lastHandleDragSoundWorldPos.distanceTo(currentWorld));
        lastHandleDragSoundWorldPos = currentWorld;
    }

    private static void tickHandleDragSoundFromScalar(Minecraft mc, double current) {
        if (mc.player == null || mc.level == null || Double.isNaN(current)) {
            return;
        }
        if (Double.isNaN(lastHandleDragSoundScalar)) {
            lastHandleDragSoundScalar = current;
            return;
        }
        BezierHandleDragManager.accumulateHandleDragSoundFromMovement(mc, Math.abs(current - lastHandleDragSoundScalar));
        lastHandleDragSoundScalar = current;
    }

    private static void accumulateHandleDragSoundFromMovement(Minecraft mc, double deltaBlocks) {
        if (deltaBlocks < 0.002) {
            return;
        }
        handleDragTickSoundAccum += deltaBlocks;
        while (handleDragTickSoundAccum >= 0.08) {
            handleDragTickSoundAccum -= 0.08;
            float vol = Mth.clamp((float)((float)(deltaBlocks * (double)0.85f)), (float)0.035f, (float)0.1f);
            float pitch = 0.84f + mc.level.random.nextFloat() * 0.26f;
            BezierHandleClientSounds.playCreateUi(AllSoundEvents.SCROLL_VALUE.getMainEvent(), vol, pitch);
        }
    }

    private static Vec3 liftDragVirtualTarget(Minecraft mc, Level level, Vec3 eyeRayTargetRender) {
        if (dragLiftAnchorPos == null) {
            return eyeRayTargetRender;
        }
        Vec3 eyeRayTarget = CoasterAnchorClientSpace.toPlotLocal(level, dragLiftAnchorPos, eyeRayTargetRender);
        if (BezierHandleDragManager.isCreateAltFineDown()) {
            return eyeRayTarget;
        }
        Float rawLift = CoasterBezierHandleEdit.computeLiftBlocksFromVirtualTarget(level, dragLiftAnchorPos, eyeRayTarget);
        if (rawLift == null) {
            return eyeRayTarget;
        }
        float snapped = CoasterBezierHandleEdit.snapLiftBlocksToNearestHalf(rawLift.floatValue());
        Vec3 adj = CoasterBezierHandleEdit.liftVirtualTargetWorldForStoredLift(level, dragLiftAnchorPos, eyeRayTarget, snapped);
        return adj != null ? adj : eyeRayTarget;
    }

    private static boolean isCreateAltFineDown() {
        return AllKeys.altDown();
    }

    private static Vec3 tiltDragVirtualTarget(Minecraft mc, Level level, Vec3 eyeRayTargetRender) {
        if (dragTiltAnchorPos == null) {
            return eyeRayTargetRender;
        }
        Vec3 eyeRayTarget = CoasterAnchorClientSpace.toPlotLocal(level, dragTiltAnchorPos, eyeRayTargetRender);
        if (BezierHandleDragManager.isCreateAltFineDown()) {
            return eyeRayTarget;
        }
        Double rawTheta = CoasterBezierHandleEdit.computeJointBankRadiansFromVirtualTarget(level, dragTiltAnchorPos, eyeRayTarget);
        if (rawTheta == null) {
            return eyeRayTarget;
        }
        double snapped = CoasterBezierHandleEdit.snapJointBankRadiansToNearestTiltSnapStep(level, dragTiltAnchorPos, rawTheta);
        Vec3 adj = CoasterBezierHandleEdit.tiltVirtualTargetWorldForJointBankRadians(level, dragTiltAnchorPos, eyeRayTarget, snapped);
        return adj != null ? adj : eyeRayTarget;
    }

    @Nullable
    private static Vec3 tiltDragEyeVirtualTarget(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || dragTiltAnchorPos == null || mc.player == null) {
            return null;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 eye = mc.player.getEyePosition(pt);
        Vec3 fwd = mc.player.getViewVector(pt);
        fwd = fwd.lengthSqr() < 1.0E-12 ? BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera()) : fwd.normalize();
        Vec3 eyeRayTarget = eye.add(fwd.scale(dragViewForwardDistance));
        return BezierHandleDragManager.tiltDragVirtualTarget(mc, (Level)level, eyeRayTarget);
    }

    private static void releaseCommit(Minecraft mc) {
        Vec3 raw;
        if (draggingTilt) {
            BezierHandleDragManager.updateTiltPreviewFromVirtualTarget(mc);
            if (tiltPreview != null && tiltPreview.valid() && dragTiltAnchorPos != null && mc.level != null && mc.player != null) {
                Vec3 virtualTarget = BezierHandleDragManager.tiltDragEyeVirtualTarget(mc);
                if (virtualTarget != null) {
                    PacketDistributor.sendToServer((CustomPacketPayload)new BezierTiltEditPayload(dragTiltAnchorPos, virtualTarget), (CustomPacketPayload[])new CustomPacketPayload[0]);
                    BezierHandleClientSounds.playCommitSuccessSound(mc);
                    BezierHandleDragManager.cancelInner(mc, false);
                } else {
                    BezierHandleDragManager.cancelInner(mc, true);
                }
            } else {
                BezierHandleDragManager.cancelInner(mc, true);
            }
            useKeyWasDown = false;
            return;
        }
        if (draggingLift) {
            BezierHandleDragManager.updateLiftPreviewFromVirtualTarget(mc);
            if (liftPreview != null && liftPreview.valid() && dragLiftAnchorPos != null && mc.level != null && mc.player != null) {
                float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
                Vec3 eye = mc.player.getEyePosition(pt);
                Vec3 fwd = mc.player.getViewVector(pt);
                fwd = fwd.lengthSqr() < 1.0E-12 ? BezierHandleDragManager.cameraLookDir(mc.gameRenderer.getMainCamera()) : fwd.normalize();
                Vec3 eyeRayTarget = eye.add(fwd.scale(dragViewForwardDistance));
                Vec3 virtualTarget = BezierHandleDragManager.liftDragVirtualTarget(mc, (Level)mc.level, eyeRayTarget);
                PacketDistributor.sendToServer((CustomPacketPayload)new BezierLiftEditPayload(dragLiftAnchorPos, virtualTarget), (CustomPacketPayload[])new CustomPacketPayload[0]);
                BezierHandleClientSounds.playCommitSuccessSound(mc);
                BezierHandleDragManager.cancelInner(mc, false);
            } else {
                BezierHandleDragManager.cancelInner(mc, true);
            }
            useKeyWasDown = false;
            return;
        }
        if (draggingOpenEndpoint) {
            BezierHandleDragManager.updateOpenEndpointPreviewFromVirtualTarget(mc);
            if (dragOpenEndpointAnchorPos != null && mc.level != null && mc.player != null && !Float.isNaN(openEndpointPreviewExtensionBlocks)) {
                Vec3 virtualTarget = BezierHandleDragManager.openEndpointDragVirtualTarget(mc);
                if (virtualTarget != null) {
                    PacketDistributor.sendToServer((CustomPacketPayload)new BezierOpenEndExtensionEditPayload(dragOpenEndpointAnchorPos, virtualTarget), (CustomPacketPayload[])new CustomPacketPayload[0]);
                    BezierHandleClientSounds.playCommitSuccessSound(mc);
                    BezierHandleDragManager.cancelInner(mc, false);
                } else {
                    BezierHandleDragManager.cancelInner(mc, true);
                }
            } else {
                BezierHandleDragManager.cancelInner(mc, true);
            }
            useKeyWasDown = false;
            return;
        }
        BezierHandleDragManager.updatePreviewFromVirtualTarget(mc);
        if (preview != null && preview.valid() && mc.level != null && mc.player != null && dragEndpointPos != null && (raw = BezierHandleDragManager.computeTangentDragRawEyeTarget(mc)) != null) {
            Vec3 virtualTarget = BezierHandleDragManager.computeTangentGuideSnap(mc, raw).snappedVirtualTarget();
            Vec3 handleRoot = CoasterAnchorpointBlockEntity.worldCenter((Level)mc.level, dragEndpointPos);
            Vec3 handleOffset = virtualTarget.subtract(handleRoot);
            PacketDistributor.sendToServer((CustomPacketPayload)new BezierHandleEditPayload(dragPrimaryHost, dragRemoteEnd, dragEndpointIndex, handleOffset), (CustomPacketPayload[])new CustomPacketPayload[0]);
            BezierHandleClientSounds.playCommitSuccessSound(mc);
        }
        BezierHandleDragManager.cancelInner(mc, true);
        useKeyWasDown = false;
    }

    private static void cancelInner() {
        BezierHandleDragManager.cancelInner(Minecraft.getInstance(), true);
    }

    private static void cancelInner(Minecraft mc, boolean clearHandleHud) {
        boolean hadLiftDrag = draggingLift;
        boolean hadTiltDrag = draggingTilt;
        boolean hadTangentDrag = dragging;
        boolean hadOpenEndpointDrag = draggingOpenEndpoint;
        suppressHandleGrabUntilUseRelease = false;
        BezierHandleDragManager.resetHandleDragSoundSample();
        dragging = false;
        draggingTilt = false;
        draggingLift = false;
        draggingOpenEndpoint = false;
        hoverPick = null;
        hoverTilt = null;
        hoverLift = null;
        hoverOpenEndpoint = null;
        dragPrimaryHost = null;
        dragRemoteEnd = null;
        dragEndpointPos = null;
        dragTiltAnchorPos = null;
        dragLiftAnchorPos = null;
        dragOpenEndpointAnchorPos = null;
        openEndpointPreviewExtensionBlocks = Float.NaN;
        dragActiveEdgeKey = null;
        dragViewForwardDistance = 0.0;
        preview = null;
        tiltPreview = null;
        liftPreview = null;
        if (clearHandleHud && (hadLiftDrag || hadTiltDrag || hadTangentDrag || hadOpenEndpointDrag) && mc != null && mc.player != null) {
            mc.player.displayClientMessage((Component)Component.empty(), true);
        }
    }

    public static HandlePick getHoverPick() {
        return hoverPick;
    }

    public static boolean isDraggingHandle() {
        return dragging || draggingTilt || draggingLift || draggingOpenEndpoint;
    }

    public static boolean isDraggingTangentHandle() {
        return dragging;
    }

    public static boolean isDraggingTangentOnAnchor(BlockPos anchorBlock) {
        return dragging && dragEndpointPos != null && dragEndpointPos.equals((Object)anchorBlock);
    }

    public static boolean isDraggingOpenEndpointHandle() {
        return draggingOpenEndpoint;
    }

    public static boolean isHoveredOpenEndpointAnchor(BlockPos anchorBlock) {
        return !dragging && !draggingTilt && !draggingLift && !draggingOpenEndpoint && hoverOpenEndpoint != null && hoverOpenEndpoint.anchorPos().equals((Object)anchorBlock);
    }

    public static boolean isDraggingTiltHandle() {
        return draggingTilt;
    }

    public static boolean isDraggingLiftHandle() {
        return draggingLift;
    }

    public static boolean isHoveringOrDraggingAnyHandle(Minecraft mc) {
        if (BezierHandleDragManager.isDraggingHandle()) {
            return true;
        }
        if (!BezierHandleEditMode.isActive() || mc.player == null || mc.level == null) {
            return false;
        }
        BezierHandleDragManager.resolveHover(mc);
        return hoverPick != null || hoverTilt != null || hoverLift != null || hoverOpenEndpoint != null;
    }

    public static boolean shouldSuppressVanillaUse(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return false;
        }
        return BezierHandleDragManager.isHoveringOrDraggingAnyHandle(mc);
    }

    @Nullable
    public static CoasterBezierHandleEdit.PreviewResult currentDragPreview() {
        if (draggingTilt) {
            return tiltPreview;
        }
        if (draggingLift) {
            return liftPreview;
        }
        return preview;
    }

    @Nullable
    public static AnchorFrameOverride livePreviewedAnchorFrame(BlockPos anchor) {
        CoasterBezierHandleEdit.PreviewResult pr = BezierHandleDragManager.currentDragPreview();
        if (pr == null || pr.allUpdated() == null) {
            return null;
        }
        for (BezierConnection c : pr.allUpdated()) {
            Vec3 tIn;
            if (((BlockPos)c.bePositions.getFirst()).equals((Object)anchor) && (tIn = CoasterBezierHandleEdit.tangentIntoFromAnchor(c, anchor)) != null) {
                return new AnchorFrameOverride((Vec3)c.starts.getFirst(), (Vec3)c.normals.getFirst(), tIn);
            }
            if (!((BlockPos)c.bePositions.getSecond()).equals((Object)anchor) || (tIn = CoasterBezierHandleEdit.tangentIntoFromAnchor(c, anchor)) == null) continue;
            return new AnchorFrameOverride((Vec3)c.starts.getSecond(), (Vec3)c.normals.getSecond(), tIn);
        }
        return null;
    }

    public static boolean isHoveredEndpoint(Minecraft mc, String edgeKey, BlockPos endpointBlock) {
        if (dragging || draggingTilt || draggingLift || mc.level == null) {
            return false;
        }
        HandlePick h = hoverPick;
        if (h == null) {
            return false;
        }
        BezierConnection bc = BezierHandleDragManager.loadPrimary((Level)mc.level, h.primaryHost(), h.remoteEnd());
        if (bc == null || !BezierHandleDragManager.edgeKey(bc).equals(edgeKey)) {
            return false;
        }
        BlockPos ep = h.endpointIndex() == 0 ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
        return ep.equals((Object)endpointBlock);
    }

    public static boolean isDraggingEndpoint(String edgeKey, BlockPos endpointBlock) {
        return dragging && dragActiveEdgeKey != null && dragEndpointPos != null && dragActiveEdgeKey.equals(edgeKey) && dragEndpointPos.equals((Object)endpointBlock);
    }

    public static boolean isDraggingTiltOnAnchor(BlockPos anchorBlock) {
        return draggingTilt && dragTiltAnchorPos != null && dragTiltAnchorPos.equals((Object)anchorBlock);
    }

    public static boolean isHoveredTiltAnchor(BlockPos anchorBlock) {
        return !dragging && !draggingTilt && !draggingLift && hoverTilt != null && hoverTilt.anchorPos().equals((Object)anchorBlock);
    }

    public static boolean isDraggingLiftOnAnchor(BlockPos anchorBlock) {
        return draggingLift && dragLiftAnchorPos != null && dragLiftAnchorPos.equals((Object)anchorBlock);
    }

    public static boolean isHoveredLiftAnchor(BlockPos anchorBlock) {
        return !dragging && !draggingTilt && !draggingLift && hoverLift != null && hoverLift.anchorPos().equals((Object)anchorBlock);
    }

    private static double raySphere(Vec3 ro, Vec3 rd, Vec3 center, double radius) {
        double c;
        Vec3 oc = ro.subtract(center);
        double b = oc.dot(rd);
        double disc = b * b - (c = oc.dot(oc) - radius * radius);
        if (disc < 0.0) {
            return -1.0;
        }
        double sqrt = Math.sqrt(disc);
        double t0 = -b - sqrt;
        double t1 = -b + sqrt;
        double t = t0 >= 1.0E-4 ? t0 : (t1 >= 1.0E-4 ? t1 : -1.0);
        return t;
    }

    public static String edgeKey(BezierConnection bc) {
        long b;
        long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
        return a <= (b = ((BlockPos)bc.bePositions.getSecond()).asLong()) ? a + ":" + b : b + ":" + a;
    }

    public static BezierConnection previewCurveOrWorld(String edgeKey, BezierConnection worldBc) {
        if (draggingTilt && tiltPreview != null) {
            for (BezierConnection c : tiltPreview.allUpdated()) {
                if (!BezierHandleDragManager.edgeKey(c).equals(edgeKey)) continue;
                return c;
            }
            return worldBc;
        }
        if (draggingLift && liftPreview != null) {
            for (BezierConnection c : liftPreview.allUpdated()) {
                if (!BezierHandleDragManager.edgeKey(c).equals(edgeKey)) continue;
                return c;
            }
            return worldBc;
        }
        if (!dragging || preview == null) {
            return worldBc;
        }
        for (BezierConnection c : preview.allUpdated()) {
            if (!BezierHandleDragManager.edgeKey(c).equals(edgeKey)) continue;
            return c;
        }
        return worldBc;
    }

    public static boolean previewValid() {
        return draggingTilt && tiltPreview != null && tiltPreview.valid() || draggingLift && liftPreview != null && liftPreview.valid() || dragging && preview != null && preview.valid();
    }

    public static boolean previewInvalidDrag() {
        return draggingTilt && tiltPreview != null && !tiltPreview.valid() || draggingLift && liftPreview != null && !liftPreview.valid() || dragging && preview != null && !preview.valid();
    }

    public static boolean invalidTint(String edgeKey) {
        return BezierHandleDragManager.previewInvalidDrag() && BezierHandleDragManager.previewTouchesEdge(edgeKey);
    }

    public static boolean validDragTint(String edgeKey) {
        return BezierHandleDragManager.previewValid() && BezierHandleDragManager.previewTouchesEdge(edgeKey);
    }

    private static boolean previewTouchesEdge(String ek) {
        if (draggingTilt && tiltPreview != null) {
            for (BezierConnection c : tiltPreview.allUpdated()) {
                if (!BezierHandleDragManager.edgeKey(c).equals(ek)) continue;
                return true;
            }
        }
        if (draggingLift && liftPreview != null) {
            for (BezierConnection c : liftPreview.allUpdated()) {
                if (!BezierHandleDragManager.edgeKey(c).equals(ek)) continue;
                return true;
            }
        }
        if (dragging && preview != null) {
            for (BezierConnection c : preview.allUpdated()) {
                if (!BezierHandleDragManager.edgeKey(c).equals(ek)) continue;
                return true;
            }
        }
        return false;
    }

    static {
        lastHandleDragSoundScalar = Double.NaN;
        openEndpointPreviewExtensionBlocks = Float.NaN;
    }

    public record HandlePick(BlockPos primaryHost, BlockPos remoteEnd, int endpointIndex, Vec3 handleTipWorld) {
    }

    public record TiltHover(BlockPos anchorPos, Vec3 tipWorld) {
    }

    public record LiftHover(BlockPos anchorPos, Vec3 tipWorld) {
    }

    public record OpenEndpointHover(BlockPos anchorPos, Vec3 tipWorld) {
    }

    public record AnchorFrameOverride(Vec3 center, Vec3 faceNormal, Vec3 tangentInto) {
    }
}

