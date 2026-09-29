/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  com.simibubi.create.AllSoundEvents
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.cart;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleEditMode;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkDistanceEditPayload;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.UUID;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartEditMode {
    @Nullable
    private static UUID activeCartId;

    private CoasterCartEditMode() {
    }

    public static boolean isActive() {
        return activeCartId != null;
    }

    @Nullable
    public static UUID getActiveCartId() {
        return activeCartId;
    }

    public static void clear() {
        activeCartId = null;
    }

    public static boolean tryActivateFromInteract(Level level, Player player, BlockPos clickedPos) {
        if (!level.isClientSide()) {
            return false;
        }
        if (!CoasterCartEditMode.isHoldingWrench(player)) {
            return false;
        }
        SubLevel cart = CoasterCartResolve.findCartSubLevelAt(level, clickedPos);
        if (cart == null || cart.isRemoved()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != player) {
            return false;
        }
        if (!CoasterCartEditMode.isWithinReach(player, level, cart)) {
            return false;
        }
        if (activeCartId != null && activeCartId.equals(cart.getUniqueId())) {
            return CoasterCartEditMode.tryAdjustLinksFromInteract(player, cart);
        }
        BezierHandleEditMode.clear();
        activeCartId = cart.getUniqueId();
        CoasterCartEditMode.playActivateFeedback(player);
        return true;
    }

    private static boolean tryAdjustLinksFromInteract(Player player, SubLevel cart) {
        PacketDistributor.sendToServer((CustomPacketPayload)new CoasterCartLinkDistanceEditPayload(cart.getUniqueId(), player.isShiftKeyDown()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        CoasterCartEditMode.playActivateFeedback(player);
        return true;
    }

    public static void clientTick(Minecraft mc) {
        if (activeCartId == null) {
            return;
        }
        if (mc.player == null || mc.level == null || mc.screen != null) {
            CoasterCartEditMode.clear();
            return;
        }
        if (!CoasterCartEditMode.isHoldingWrench((Player)mc.player)) {
            CoasterCartEditMode.clear();
            return;
        }
        SubLevel cart = CoasterCartResolve.findCartById((Level)mc.level, activeCartId);
        if (cart == null || cart.isRemoved()) {
            CoasterCartEditMode.clear();
            return;
        }
        if (!CoasterCartEditMode.isWithinReach((Player)mc.player, (Level)mc.level, cart)) {
            CoasterCartEditMode.clear();
        }
    }

    private static boolean isHoldingWrench(Player player) {
        return AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem());
    }

    private static boolean isWithinReach(Player player, Level level, SubLevel cart) {
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 eye = player.getEyePosition(pt);
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Vec3 center = CoasterCartLinkPlacement.partnerBearingCenterWorld(cart, Double.valueOf(pt));
        if (center == null) {
            return false;
        }
        return eye.distanceToSqr(center) <= reach * reach;
    }

    private static void playActivateFeedback(Player player) {
        InteractionHand hand = AllItems.WRENCH.isIn(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        player.swing(hand);
        float pitch = 1.0f + player.level().random.nextFloat() * 0.08f;
        BezierHandleClientSounds.playCreateUi(AllSoundEvents.WRENCH_ROTATE.getMainEvent(), 0.9f, pitch);
    }
}

