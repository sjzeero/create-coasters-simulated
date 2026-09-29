/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.fml.loading.FMLEnvironment
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartChainLinkOutlineClient;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSnap;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public final class CoasterCartChainLinkInteraction {
    private CoasterCartChainLinkInteraction() {
    }

    public static boolean holdingChain(ItemStack stack) {
        return stack.is(Items.CHAIN);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!CoasterCartChainLinkInteraction.holdingChain(stack)) {
            return;
        }
        Level level = player.level();
        if (player.isShiftKeyDown() && CoasterCartLinkSelection.hasLinkTarget(stack)) {
            CoasterCartLinkSelection.clearLinkTarget(stack);
            if (!level.isClientSide()) {
                player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.cart.link_cleared").withStyle(ChatFormatting.GRAY), true);
            }
            event.setCanceled(true);
            return;
        }
        SubLevel clickedCart = CoasterCartResolve.findCartSubLevelAt(level, event.getPos());
        if (clickedCart == null) {
            return;
        }
        event.setCanceled(true);
        if (level.isClientSide()) {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientCoupleFeedback.playDenyIfBlocked(stack, clickedCart);
            }
            return;
        }
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)level;
        if (!(clickedCart instanceof ServerSubLevel)) {
            return;
        }
        ServerSubLevel serverCart = (ServerSubLevel)clickedCart;
        CoasterCartChainLinkInteraction.handleLinkClick(sl, serverPlayer, stack, serverCart);
    }

    private static void handleLinkClick(ServerLevel level, ServerPlayer player, ItemStack stack, ServerSubLevel clickedCart) {
        @Nullable UUID selectedId = CoasterCartLinkSelection.readLinkTarget(stack);
        if (selectedId == null) {
            CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling(level, clickedCart);
            CoasterCartLinkSelection.applyLinkTarget((Level)level, stack, (SubLevel)clickedCart);
            player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.cart.link_selected_chain").withStyle(ChatFormatting.GRAY), true);
            SimulatedCoasters.LOGGER.info("[cart-link] chain selected cart {} for coupling", (Object)clickedCart.getUniqueId());
            return;
        }
        if (selectedId.equals(clickedCart.getUniqueId())) {
            CoasterCartLinkSelection.applyLinkTarget((Level)level, stack, (SubLevel)clickedCart);
            player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.cart.link_selected_chain").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        ServerSubLevel partner = CoasterCartResolve.findCartById(level, selectedId);
        if (partner == null || partner.isRemoved() || partner == clickedCart) {
            CoasterCartLinkSelection.clearLinkTarget(stack);
            SimulatedCoasters.LOGGER.info("[cart-link] chain partner {} missing; selection cleared", (Object)selectedId);
            player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.cart.link_cleared").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        double rawDistance = CoasterCartLinkPlacement.bearingCenterDistance((SubLevel)partner, (SubLevel)clickedCart, null);
        if (Double.isNaN(rawDistance) || CoasterCartLinkPlacement.rejectsLinkDistance(rawDistance)) {
            return;
        }
        if (CoasterCartTrainLinkConstraint.coupleRejectLangKey((SubLevel)partner, (SubLevel)clickedCart) != null) {
            return;
        }
        double distance = CoasterCartLinkPlacement.resolveChainLinkDistance((Level)level, (SubLevel)partner, (SubLevel)clickedCart, null, CoasterCartLinkSnap.isSuppressLinkSnap(player.getUUID()));
        CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling(level, partner);
        CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling(level, clickedCart);
        if (!CoasterCartTrainLinkConstraint.linkCarts(level, partner, clickedCart, distance)) {
            return;
        }
        CoasterCartLinkSelection.clearLinkTarget(stack);
        player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.cart.link_coupled").withStyle(ChatFormatting.GRAY), true);
    }

    @Nullable
    public static SubLevel resolveHoveredCart(Level level, HitResult hitResult) {
        BlockHitResult bhr;
        if (!(hitResult instanceof BlockHitResult) || (bhr = (BlockHitResult)hitResult).getType() != HitResult.Type.BLOCK) {
            return null;
        }
        SubLevel cart = CoasterCartResolve.findCartSubLevelAt(level, bhr.getBlockPos());
        if (cart == null || cart.isRemoved()) {
            return null;
        }
        return cart;
    }

    private static final class ClientCoupleFeedback {
        private ClientCoupleFeedback() {
        }

        static void playDenyIfBlocked(ItemStack chain, SubLevel clickedCart) {
            CoasterCartChainLinkOutlineClient.playDenyIfCoupleBlocked(chain, clickedCart);
        }
    }
}

