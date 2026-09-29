package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClearCoasterTrackHotbarSelectionPayload(int hotbarSlot) implements CustomPacketPayload {
   public static final Type<ClearCoasterTrackHotbarSelectionPayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "clear_coaster_track_hotbar_selection")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ClearCoasterTrackHotbarSelectionPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, ClearCoasterTrackHotbarSelectionPayload::hotbarSlot, ClearCoasterTrackHotbarSelectionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnServer(ClearCoasterTrackHotbarSelectionPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof ServerPlayer player) {
            int slot = payload.hotbarSlot();
            if (slot >= 0 && slot <= 8) {
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
            }
         }
      });
   }
}
