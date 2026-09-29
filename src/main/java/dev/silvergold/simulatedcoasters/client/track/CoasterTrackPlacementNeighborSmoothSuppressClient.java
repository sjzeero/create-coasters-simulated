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
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllKeys;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacementNeighborSmoothSuppressPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterTrackPlacementNeighborSmoothSuppressClient {
    private static boolean lastSentSuppress;

    private CoasterTrackPlacementNeighborSmoothSuppressClient() {
    }

    public static boolean isSuppressNeighborSmoothing() {
        return AllKeys.altDown();
    }

    public static void clientTick(Minecraft mc) {
        if (mc.player == null) {
            CoasterTrackPlacementNeighborSmoothSuppressClient.syncSuppress(false);
            return;
        }
        if (!CoasterTrackPlacementNeighborSmoothSuppressClient.tracksPlacementContext(mc)) {
            CoasterTrackPlacementNeighborSmoothSuppressClient.syncSuppress(false);
            return;
        }
        CoasterTrackPlacementNeighborSmoothSuppressClient.syncSuppress(CoasterTrackPlacementNeighborSmoothSuppressClient.isSuppressNeighborSmoothing());
    }

    private static boolean tracksPlacementContext(Minecraft mc) {
        ItemStack main = mc.player.getMainHandItem();
        if (main.getItem() instanceof CoasterTrackItem) {
            return true;
        }
        ItemStack off = mc.player.getOffhandItem();
        return off.getItem() instanceof CoasterTrackItem;
    }

    private static void syncSuppress(boolean suppress) {
        if (suppress == lastSentSuppress) {
            return;
        }
        lastSentSuppress = suppress;
        PacketDistributor.sendToServer((CustomPacketPayload)new CoasterTrackPlacementNeighborSmoothSuppressPayload(suppress), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

