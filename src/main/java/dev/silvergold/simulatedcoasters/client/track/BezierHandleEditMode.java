/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  com.simibubi.create.AllSoundEvents
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartEditMode;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorClientSpace;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftRemovePayload;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftShaftCyclePayload;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanQuery;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class BezierHandleEditMode {
    @Nullable
    private static BlockPos activeAnchor;

    private BezierHandleEditMode() {
    }

    public static boolean isActive() {
        return activeAnchor != null;
    }

    @Nullable
    public static BlockPos getActiveAnchor() {
        return activeAnchor;
    }

    public static void switchActiveAnchorTo(BlockPos anchorPos, Level level) {
        if (!BezierHandleEditMode.isActive() || anchorPos == null) {
            return;
        }
        if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity)) {
            return;
        }
        if (activeAnchor != null && activeAnchor.equals((Object)anchorPos)) {
            return;
        }
        activeAnchor = anchorPos.immutable();
    }

    public static void clear() {
        activeAnchor = null;
    }

    public static boolean tryActivateFromInteract(Level level, Player player, BlockPos anchorPos) {
        if (player.isShiftKeyDown()) {
            return BezierHandleEditMode.tryRemoveChainLiftFromInteract(level, player, anchorPos);
        }
        if (BezierHandleEditMode.tryCycleChainLiftShaftFromInteract(level, player, anchorPos)) {
            return true;
        }
        BezierHandleEditMode.tryActivate(level, player, anchorPos, true);
        return false;
    }

    public static void tryActivateFromCurveInteract(Level level, Player player, BlockPos anchorPos) {
        BezierHandleEditMode.tryActivate(level, player, anchorPos, false);
    }

    public static boolean tryRemoveChainLiftFromCurveInteract(Level level, Player player, CoasterPathTrackFrame.GraphHit hit) {
        if (!level.isClientSide || hit == null) {
            return false;
        }
        if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) {
            return false;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        @Nullable ChainLiftSpanQuery.ChainLiftSpanHit lift = ChainLiftSpanQuery.findAtGraphHit(level, hit, pt);
        if (lift == null) {
            return false;
        }
        return BezierHandleEditMode.sendChainLiftRemove(player, lift.anchor().getBlockPos(), hit.point());
    }

    private static void tryActivate(Level level, Player player, BlockPos anchorPos, boolean enforceReach) {
        if (!level.isClientSide || !(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity)) {
            return;
        }
        if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) {
            return;
        }
        if (enforceReach) {
            float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
            Vec3 eye = player.getEyePosition(pt);
            double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 22.0;
            Vec3 ac = CoasterAnchorClientSpace.anchorCenterRenderWorld(level, anchorPos);
            if (eye.distanceToSqr(ac) > reach * reach) {
                return;
            }
        }
        if (activeAnchor != null && activeAnchor.equals((Object)anchorPos)) {
            return;
        }
        CoasterCartEditMode.clear();
        activeAnchor = anchorPos.immutable();
        BezierHandleDragManager.onEditModeActivated();
        BezierHandleEditMode.playWrenchActivateFeedback(player);
    }

    private static boolean tryCycleChainLiftShaftFromInteract(Level level, Player player, BlockPos anchorPos) {
        if (activeAnchor == null || !activeAnchor.equals((Object)anchorPos)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        if (!be.hasChainLiftShaft()) {
            return false;
        }
        if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) {
            return false;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 eye = player.getEyePosition(pt);
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 22.0;
        Vec3 ac = CoasterAnchorClientSpace.anchorCenterRenderWorld(level, anchorPos, pt);
        if (eye.distanceToSqr(ac) > reach * reach) {
            return false;
        }
        PacketDistributor.sendToServer((CustomPacketPayload)new ChainLiftShaftCyclePayload(anchorPos), (CustomPacketPayload[])new CustomPacketPayload[0]);
        BezierHandleDragManager.onEditModeActivated();
        BezierHandleEditMode.playWrenchActivateFeedback(player);
        return true;
    }

    private static boolean tryRemoveChainLiftFromInteract(Level level, Player player, BlockPos anchorPos) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) {
            return false;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 eye = player.getEyePosition(pt);
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 22.0;
        Vec3 ac = CoasterAnchorClientSpace.anchorCenterRenderWorld(level, anchorPos, pt);
        if (eye.distanceToSqr(ac) > reach * reach) {
            return false;
        }
        BlockPos hostPos = null;
        if (be.hasChainLiftShaft()) {
            hostPos = anchorPos;
        } else {
            @Nullable CoasterAnchorpointBlockEntity host = BezierHandleEditMode.findChainLiftHostCoveringAnchor(level, anchorPos, pt);
            if (host != null) {
                hostPos = host.getBlockPos();
            }
        }
        if (hostPos == null) {
            return false;
        }
        return BezierHandleEditMode.sendChainLiftRemove(player, hostPos, ac);
    }

    @Nullable
    private static CoasterAnchorpointBlockEntity findChainLiftHostCoveringAnchor(Level level, BlockPos anchorPos, float partialTick) {
        CoasterPathGraph graph = CoasterPathGraphManagerClient.get(level);
        for (CoasterPathTrackFrame.GraphHit junction : CoasterPathGraphSpans.anchorJunctionHits(level, graph, anchorPos, partialTick)) {
            @Nullable ChainLiftSpanQuery.ChainLiftSpanHit lift = ChainLiftSpanQuery.findAtGraphHit(level, junction, partialTick);
            if (lift == null) continue;
            return lift.anchor();
        }
        return null;
    }

    private static boolean sendChainLiftRemove(Player player, BlockPos hostPos, Vec3 soundWorldPos) {
        PacketDistributor.sendToServer((CustomPacketPayload)new ChainLiftRemovePayload(hostPos, soundWorldPos), (CustomPacketPayload[])new CustomPacketPayload[0]);
        if (BezierHandleEditMode.isActive()) {
            BezierHandleDragManager.onEditModeActivated();
        }
        BezierHandleEditMode.playWrenchSwing(player);
        return true;
    }

    private static void playWrenchSwing(Player player) {
        InteractionHand hand = AllItems.WRENCH.isIn(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        player.swing(hand);
    }

    private static void playWrenchActivateFeedback(Player player) {
        BezierHandleEditMode.playWrenchSwing(player);
        float pitch = 1.0f + player.level().random.nextFloat() * 0.08f;
        BezierHandleClientSounds.playCreateUi(AllSoundEvents.WRENCH_ROTATE.getMainEvent(), 0.9f, pitch);
    }

    public static void clientTick(Minecraft mc) {
        if (activeAnchor == null) {
            return;
        }
        if (mc.player == null || mc.level == null || mc.screen != null) {
            BezierHandleEditMode.clear();
            return;
        }
        if (!AllItems.WRENCH.isIn(mc.player.getMainHandItem()) && !AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
            BezierHandleEditMode.clear();
            return;
        }
        if (!(mc.level.getBlockEntity(activeAnchor) instanceof CoasterAnchorpointBlockEntity)) {
            BezierHandleEditMode.clear();
        }
    }
}

