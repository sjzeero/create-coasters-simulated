/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.AxeItem
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveOutlineSupport;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveDyeOutlineClient;
import dev.silvergold.simulatedcoasters.track.CoasterCurveUndyeApplyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCurveUndyeApplyClient {
    private CoasterCurveUndyeApplyClient() {
    }

    public static boolean onUseItemKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!CoasterCurveUndyeApplyClient.trySendClear(mc)) {
            return false;
        }
        event.setCanceled(true);
        event.setSwingHand(true);
        return true;
    }

    private static boolean trySendClear(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return false;
        }
        if (CoasterCurveDyeOutlineClient.heldDyeItem((Player)mc.player) != null) {
            return false;
        }
        InteractionHand hand = CoasterCurveUndyeApplyClient.handWithAxe((Player)mc.player);
        if (hand == null) {
            return false;
        }
        BezierConnection bc = CoasterCurveDyeOutlineClient.resolvePreviewHostedPrimary(mc);
        if (bc == null || !AnchorPeerCurveOutlineSupport.hasSaneOutlineBounds(bc)) {
            return false;
        }
        Vec3 soundPos = CoasterCurveDyeOutlineClient.resolvePreviewWorldHit(mc);
        if (soundPos == null) {
            return false;
        }
        BlockPos owner = (BlockPos)bc.bePositions.getFirst();
        BlockPos remote = (BlockPos)bc.bePositions.getSecond();
        PacketDistributor.sendToServer((CustomPacketPayload)new CoasterCurveUndyeApplyPayload(owner, remote, soundPos, hand), (CustomPacketPayload[])new CustomPacketPayload[0]);
        return true;
    }

    private static InteractionHand handWithAxe(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof AxeItem) {
            return InteractionHand.MAIN_HAND;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof AxeItem) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}

