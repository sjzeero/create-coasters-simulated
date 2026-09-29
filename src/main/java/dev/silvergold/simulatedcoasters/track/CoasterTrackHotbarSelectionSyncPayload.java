/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.neoforged.neoforge.network.PacketDistributor
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterTrackHotbarSelectionSyncPayload(int hotbarSlot, long anchorBlockPosLong) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CoasterTrackHotbarSelectionSyncPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track_hotbar_selection_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoasterTrackHotbarSelectionSyncPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.VAR_INT, CoasterTrackHotbarSelectionSyncPayload::hotbarSlot, (StreamCodec)ByteBufCodecs.VAR_LONG, CoasterTrackHotbarSelectionSyncPayload::anchorBlockPosLong, CoasterTrackHotbarSelectionSyncPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(CoasterTrackHotbarSelectionSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof LocalPlayer)) {
                return;
            }
            LocalPlayer player = (LocalPlayer)patt0$temp;
            int slot = payload.hotbarSlot();
            if (slot < 0 || slot > 8) {
                return;
            }
            ItemStack stack = player.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof CoasterTrackItem)) {
                return;
            }
            long anchorLong = payload.anchorBlockPosLong();
            if (anchorLong == 0L) {
                CoasterTrackItem.clearPendingConnection(stack);
            } else {
                CoasterTrackPlacement.setAnchorFirstSelection(stack, BlockPos.of((long)anchorLong));
            }
            player.getInventory().setItem(slot, stack);
        });
    }

    public static void broadcast(ServerPlayer player, int hotbarSlot, long anchorBlockPosLong) {
        if (hotbarSlot < 0 || hotbarSlot > 8) {
            return;
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new CoasterTrackHotbarSelectionSyncPayload(hotbarSlot, anchorBlockPosLong), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

