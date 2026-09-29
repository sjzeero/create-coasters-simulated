/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackHotbarSelectionSync;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClearCoasterTrackHotbarSelectionPayload(int hotbarSlot) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ClearCoasterTrackHotbarSelectionPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"clear_coaster_track_hotbar_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearCoasterTrackHotbarSelectionPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.VAR_INT, ClearCoasterTrackHotbarSelectionPayload::hotbarSlot, ClearCoasterTrackHotbarSelectionPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(ClearCoasterTrackHotbarSelectionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            int slot = payload.hotbarSlot();
            if (slot < 0 || slot > 8) {
                return;
            }
            ItemStack stack = player.getInventory().getItem(slot);
            if (CoasterTrackItem.hasPendingConnection(stack)) {
                CoasterTrackItem.clearPendingConnection(stack);
            }
            if (CoasterCartItem.hasLinkSelection(stack)) {
                CoasterCartItem.clearLinkSelection(stack);
            }
            if (stack.is(Items.CHAIN) && CoasterCartLinkSelection.hasLinkTarget(stack)) {
                CoasterCartLinkSelection.clearLinkTarget(stack);
            }
            player.getInventory().setItem(slot, stack);
            if (slot == player.getInventory().selected && stack.getItem() instanceof CoasterTrackItem) {
                CoasterTrackHotbarSelectionSync.syncMainHand(player, InteractionHand.MAIN_HAND, null);
            }
        });
    }
}

