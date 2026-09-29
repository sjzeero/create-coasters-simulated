package dev.silvergold.simulatedcoasters.track;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterTrackHotbarSelectionSyncPayload(int hotbarSlot, long anchorBlockPosLong) implements CustomPacketPayload {
   public static final Type<CoasterTrackHotbarSelectionSyncPayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_track_hotbar_selection_sync")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, CoasterTrackHotbarSelectionSyncPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      CoasterTrackHotbarSelectionSyncPayload::hotbarSlot,
      ByteBufCodecs.VAR_LONG,
      CoasterTrackHotbarSelectionSyncPayload::anchorBlockPosLong,
      CoasterTrackHotbarSelectionSyncPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnClient(CoasterTrackHotbarSelectionSyncPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         if (ctx.player() instanceof LocalPlayer player) {
            int slot = payload.hotbarSlot();
            if (slot >= 0 && slot <= 8) {
               ItemStack stack = player.getInventory().getItem(slot);
               if (stack.getItem() instanceof CoasterTrackItem) {
                  long anchorLong = payload.anchorBlockPosLong();
                  if (anchorLong == 0L) {
                     CoasterTrackItem.clearPendingConnection(stack);
                  } else {
                     CoasterTrackPlacement.setAnchorFirstSelection(stack, BlockPos.of(anchorLong));
                  }

                  player.getInventory().setItem(slot, stack);
               }
            }
         }
      });
   }

   public static void broadcast(ServerPlayer player, int hotbarSlot, long anchorBlockPosLong) {
      if (hotbarSlot >= 0 && hotbarSlot <= 8) {
         PacketDistributor.sendToPlayer(player, new CoasterTrackHotbarSelectionSyncPayload(hotbarSlot, anchorBlockPosLong), new CustomPacketPayload[0]);
      }
   }
}
