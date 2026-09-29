/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllKeys
 *  net.minecraft.client.Minecraft
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.item.ItemStack
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.silvergold.simulatedcoasters.client.cart;

import com.simibubi.create.AllKeys;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartMidTrackPlacementClient;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSnapSuppressPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartLinkSnapClient {
    private static boolean lastSentSuppress;

    private CoasterCartLinkSnapClient() {
    }

    public static boolean isSuppressLinkSnap() {
        return AllKeys.altDown();
    }

    public static void clientTick(Minecraft mc) {
        if (mc.player == null) {
            CoasterCartLinkSnapClient.syncSuppress(false);
            return;
        }
        if (!CoasterCartLinkSnapClient.tracksLinkSnapContext(mc)) {
            CoasterCartLinkSnapClient.syncSuppress(false);
            return;
        }
        CoasterCartLinkSnapClient.syncSuppress(CoasterCartLinkSnapClient.isSuppressLinkSnap());
    }

    private static boolean tracksLinkSnapContext(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        if (CoasterCartChainLinkInteraction.holdingChain(main)) {
            return true;
        }
        if (main.is(SimulatedCoasters.COASTER_CART)) {
            if (CoasterCartLinkSelection.hasLinkTarget(main)) {
                return true;
            }
            return CoasterCartMidTrackPlacementClient.holdingCoasterCart(mc) && CoasterCartMidTrackPlacementClient.placementPreviewActive();
        }
        return false;
    }

    private static void syncSuppress(boolean suppress) {
        if (suppress == lastSentSuppress) {
            return;
        }
        lastSentSuppress = suppress;
        PacketDistributor.sendToServer((CustomPacketPayload)new CoasterCartLinkSnapSuppressPayload(suppress), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

