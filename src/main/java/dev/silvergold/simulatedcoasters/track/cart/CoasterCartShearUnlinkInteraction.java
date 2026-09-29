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
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CoasterCartShearUnlinkInteraction {
    private CoasterCartShearUnlinkInteraction() {
    }

    public static boolean holdingShears(ItemStack stack) {
        return stack.is(Items.SHEARS);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!CoasterCartShearUnlinkInteraction.holdingShears(stack)) {
            return;
        }
        Level level = player.level();
        SubLevel clickedCart = CoasterCartResolve.findCartSubLevelAt(level, event.getPos());
        if (clickedCart == null) {
            return;
        }
        event.setCanceled(true);
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
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
        if (!CoasterCartTrainLinkConstraint.clearAllLinks(sl, serverCart)) {
            return;
        }
        if (!serverPlayer.getAbilities().instabuild) {
            stack.hurtAndBreak(1, (LivingEntity)serverPlayer, EquipmentSlot.MAINHAND);
        }
        serverPlayer.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.cart.links_cut").withStyle(ChatFormatting.GRAY), true);
        SimulatedCoasters.LOGGER.info("[cart-link] shears cleared all links on cart {}", (Object)serverCart.getUniqueId());
    }
}

