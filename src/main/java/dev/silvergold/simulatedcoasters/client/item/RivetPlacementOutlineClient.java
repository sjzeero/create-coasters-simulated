/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllBlocks
 *  com.simibubi.create.AllKeys
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.item;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllKeys;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.SimulatedCoastersSoundEvents;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.client.track.OrientedBoxOutlineRenderer;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacePayload;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class RivetPlacementOutlineClient {
    private static final String OUTLINER_KEY = "rivet_placement_preview";
    private static final int OUTLINE_COLOR = 10411635;
    private static final int UNSUPPORTED_OUTLINE_COLOR = 0xFF7171;
    private static final float RIVET_ROTATE_SOUND_VOLUME = 0.85f;
    private static final float RIVET_PLACE_SOUND_VOLUME = 0.5f;
    private static final float RIVET_SOUND_PITCH_SPREAD = 0.1f;
    private static final float FINE_TUNE_ENTER_PLACE_PITCH = 0.85f;
    private static final long ROTATE_HOLD_FINE_MS = 500L;
    private static final double FINE_ROTATION_YAW_TO_DEGREES = 0.15;
    private static long lastInsufficientSupportTick = -1L;
    private static int previewYRotationDegrees = 0;
    private static double previewYRotationDegreesExact = 0.0;
    private static long rotatePressStartedMs = -1L;
    private static boolean rotateHoldPromotedToFine;
    private static boolean rotateWasDown;
    private static boolean fineRotationActive;

    private RivetPlacementOutlineClient() {
    }

    static int tabPreviewYRotationDegrees() {
        return previewYRotationDegrees;
    }

    static int displayPreviewYRotationDegrees(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return previewYRotationDegrees;
        }
        @Nullable BlockHitResult bhr = RivetPlacement.findPlacementHit((Level)mc.level, (Entity)mc.player);
        if (bhr == null) {
            return previewYRotationDegrees;
        }
        boolean freePosition = AllKeys.ctrlDown();
        Vec3 look = mc.player.getViewVector(1.0f);
        return RivetPlacement.effectiveYRotationDegrees((Level)mc.level, bhr, freePosition, previewYRotationDegrees, look);
    }

    static boolean isFineRotationActive() {
        return fineRotationActive;
    }

    public static boolean onFineRotationMouseMove(double yawDelta, double pitchDelta) {
        if (!fineRotationActive) {
            return false;
        }
        if (Math.abs(yawDelta) > 1.0E-12) {
            previewYRotationDegrees = RivetPlacement.wrapYRotationDegrees((int)Math.round(previewYRotationDegreesExact += yawDelta * 0.15));
        }
        return true;
    }

    public static void clientTick(Minecraft mc) {
        Outliner outliner = Outliner.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !RivetPlacementOutlineClient.holdingRivet(mc)) {
            RivetPlacementOutlineClient.resetRotateState(mc.player);
            RivetPlacementOutlineClient.clearPreview(outliner);
            previewYRotationDegrees = 0;
            previewYRotationDegreesExact = 0.0;
            return;
        }
        if (mc.screen != null) {
            RivetPlacementOutlineClient.resetRotateState(mc.player);
        } else {
            RivetPlacementOutlineClient.tickRotateHoldAndFineRotation(mc);
        }
        @Nullable BlockHitResult bhr = RivetPlacement.findPlacementHit((Level)mc.level, (Entity)mc.player);
        if (bhr == null) {
            RivetPlacementOutlineClient.clearPreview(outliner);
            return;
        }
        if (RivetPlacementOutlineClient.isItemHatch((Level)mc.level, bhr.getBlockPos())) {
            RivetPlacementOutlineClient.clearPreview(outliner);
            return;
        }
        if (RivetPlacement.looksAtExistingRivet((Level)mc.level, (Entity)mc.player)) {
            RivetPlacementOutlineClient.clearPreview(outliner);
            return;
        }
        boolean freePosition = AllKeys.ctrlDown();
        Vec3 look = mc.player.getViewVector(1.0f);
        int yRotation = RivetPlacement.effectiveYRotationDegrees((Level)mc.level, bhr, freePosition, previewYRotationDegrees, look);
        if (RivetPlacement.crosshairTargetsExistingRivet((Level)mc.level, bhr, freePosition, previewYRotationDegrees, look)) {
            RivetPlacementOutlineClient.clearPreview(outliner);
            return;
        }
        RivetPlacement.Resolved placement = RivetPlacement.resolve(bhr, freePosition).withYRotation(yRotation);
        boolean supported = RivetPlacement.isValidPlacement((Level)mc.level, placement);
        int outlineColor = supported ? 10411635 : 0xFF7171;
        RivetPlacement.PreviewOutline outline = RivetPlacement.previewOutline(placement);
        OrientedBoxOutlineRenderer.show(outliner, OUTLINER_KEY, outline.center(), outline.axisRight(), outline.axisUp(), outline.axisForward(), outline.halfRight(), outline.halfUp(), outline.halfForward(), outlineColor);
    }

    public static boolean onUseItemKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        if (!mc.player.getItemInHand(event.getHand()).is((Item)SimulatedCoasters.RIVET.get())) {
            return false;
        }
        if (RivetPlacement.looksAtExistingRivet((Level)mc.level, (Entity)mc.player)) {
            return false;
        }
        @Nullable BlockHitResult bhr = RivetPlacement.findPlacementHit((Level)mc.level, (Entity)mc.player);
        if (bhr != null && RivetPlacementOutlineClient.isItemHatch((Level)mc.level, bhr.getBlockPos())) {
            return false;
        }
        @Nullable RivetPlacePayload payload = RivetPlacementOutlineClient.buildPayload(mc, event.getHand());
        if (payload == null) {
            return false;
        }
        if (payload == RivetPlacePayload.INVALID) {
            RivetPlacementOutlineClient.rejectInvalid(mc);
            event.setCanceled(true);
            event.setSwingHand(false);
            return true;
        }
        PacketDistributor.sendToServer((CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
        RivetPlacementOutlineClient.playPlaceSound(mc, payload);
        event.setCanceled(true);
        event.setSwingHand(true);
        return true;
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (!event.getItemStack().is((Item)SimulatedCoasters.RIVET.get())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (RivetPlacement.looksAtExistingRivet((Level)mc.level, (Entity)mc.player)) {
            return;
        }
        if (RivetPlacementOutlineClient.isItemHatch(event.getLevel(), event.getPos())) {
            return;
        }
        @Nullable RivetPlacePayload payload = RivetPlacementOutlineClient.buildPayload(mc, event.getHand());
        if (payload == null) {
            return;
        }
        event.setCanceled(true);
        if (payload == RivetPlacePayload.INVALID) {
            RivetPlacementOutlineClient.rejectInvalid(mc);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        PacketDistributor.sendToServer((CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
        RivetPlacementOutlineClient.playPlaceSound(mc, payload);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @Nullable
    private static RivetPlacePayload buildPayload(Minecraft mc, InteractionHand hand) {
        if (RivetPlacement.looksAtExistingRivet((Level)mc.level, (Entity)mc.player)) {
            return null;
        }
        @Nullable BlockHitResult bhr = RivetPlacement.findPlacementHit((Level)mc.level, (Entity)mc.player);
        if (bhr == null) {
            return null;
        }
        boolean freePosition = AllKeys.ctrlDown();
        Vec3 look = mc.player.getViewVector(1.0f);
        int yRotation = RivetPlacement.effectiveYRotationDegrees((Level)mc.level, bhr, freePosition, previewYRotationDegrees, look);
        RivetPlacement.Resolved placement = RivetPlacement.resolve(bhr, freePosition).withYRotation(yRotation);
        if (!RivetPlacement.isValidPlacement((Level)mc.level, placement)) {
            return RivetPlacePayload.INVALID;
        }
        return new RivetPlacePayload(placement.host(), placement.face(), bhr.getLocation(), freePosition, placement.yRotationDegrees(), hand);
    }

    private static void tickRotateHoldAndFineRotation(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        boolean rotateDown = AllKeys.altDown();
        long now = System.currentTimeMillis();
        if (rotateDown && !rotateWasDown) {
            rotatePressStartedMs = now;
            rotateHoldPromotedToFine = false;
        }
        if (!rotateDown && rotateWasDown) {
            if (!rotateHoldPromotedToFine && rotatePressStartedMs >= 0L && now - rotatePressStartedMs < 500L) {
                previewYRotationDegrees = RivetPlacement.cycleYRotationDegrees(previewYRotationDegrees);
                previewYRotationDegreesExact = previewYRotationDegrees;
                RivetPlacementOutlineClient.playRotateSound(mc);
            }
            RivetPlacementOutlineClient.resetRotateState(player);
        }
        rotateWasDown = rotateDown;
        if (!rotateDown) {
            return;
        }
        if (!fineRotationActive && rotatePressStartedMs >= 0L && now - rotatePressStartedMs >= 500L) {
            RivetPlacementOutlineClient.enterFineRotation(mc);
            rotateHoldPromotedToFine = true;
        }
    }

    private static void enterFineRotation(Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        fineRotationActive = true;
        previewYRotationDegreesExact = previewYRotationDegrees;
        RivetPlacementOutlineClient.playFineTuneEnterSound(mc);
    }

    private static void exitFineRotation() {
        if (fineRotationActive) {
            previewYRotationDegrees = RivetPlacement.wrapYRotationDegrees((int)Math.round(previewYRotationDegreesExact));
        }
        fineRotationActive = false;
    }

    private static void resetRotateState(@Nullable LocalPlayer player) {
        rotatePressStartedMs = -1L;
        rotateHoldPromotedToFine = false;
        rotateWasDown = false;
        RivetPlacementOutlineClient.exitFineRotation();
    }

    private static void clearPreview(Outliner outliner) {
        OrientedBoxOutlineRenderer.clear(outliner, OUTLINER_KEY);
    }

    private static void playRotateSound(Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        RandomSource random = mc.level.getRandom();
        float pitch = 1.0f + (random.nextFloat() - random.nextFloat()) * 0.1f;
        BezierHandleClientSounds.playCreateUi((SoundEvent)SimulatedCoastersSoundEvents.RIVET_ROTATE.get(), 0.85f, pitch);
    }

    private static void playFineTuneEnterSound(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return;
        }
        RandomSource random = mc.level.getRandom();
        float pitch = 0.85f + (random.nextFloat() - random.nextFloat()) * 0.1f;
        Vec3 pos = mc.player.getEyePosition();
        mc.getSoundManager().play((SoundInstance)new SimpleSoundInstance((SoundEvent)SimulatedCoastersSoundEvents.RIVET_PLACE.get(), SoundSource.BLOCKS, 0.5f, pitch, random, pos.x, pos.y, pos.z));
    }

    private static void playPlaceSound(Minecraft mc, RivetPlacePayload payload) {
        if (mc.level == null) {
            return;
        }
        BlockHitResult hit = new BlockHitResult(payload.hitLocation(), payload.face(), payload.host(), false);
        RivetPlacement.Resolved placement = RivetPlacement.resolve(hit, payload.freePosition()).withYRotation(payload.yRotationDegrees());
        Vec3 pos = RivetPlacement.faceAnchorWorld((Level)mc.level, placement);
        RandomSource random = mc.level.getRandom();
        float pitch = 1.0f + (random.nextFloat() - random.nextFloat()) * 0.1f;
        mc.getSoundManager().play((SoundInstance)new SimpleSoundInstance((SoundEvent)SimulatedCoastersSoundEvents.RIVET_PLACE.get(), SoundSource.BLOCKS, 0.5f, pitch, random, pos.x, pos.y, pos.z));
    }

    private static void rejectInvalid(Minecraft mc) {
        long tick = mc.level.getGameTime();
        if (tick != lastInsufficientSupportTick) {
            lastInsufficientSupportTick = tick;
            BezierHandleClientSounds.playCreateDeny();
            mc.player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.rivet.insufficient_support").withStyle(ChatFormatting.RED), true);
        }
    }

    private static boolean isItemHatch(Level level, BlockPos pos) {
        return AllBlocks.ITEM_HATCH.has(level.getBlockState(pos));
    }

    private static boolean holdingRivet(Minecraft mc) {
        Item rivet = (Item)SimulatedCoasters.RIVET.get();
        return mc.player.getMainHandItem().is(rivet) || mc.player.getOffhandItem().is(rivet);
    }
}

