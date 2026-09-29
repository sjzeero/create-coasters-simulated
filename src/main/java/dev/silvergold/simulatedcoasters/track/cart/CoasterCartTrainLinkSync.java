package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainLinkClientState;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "simulatedcoasters"
)
public final class CoasterCartTrainLinkSync {
   private CoasterCartTrainLinkSync() {
   }

   public static void registerPayload(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar reg = event.registrar("simulatedcoasters");
      reg.playToClient(
         CoasterCartTrainLinkSync.CartTrainLinkSyncPayload.TYPE,
         CoasterCartTrainLinkSync.CartTrainLinkSyncPayload.STREAM_CODEC,
         CoasterCartTrainLinkSync::handleClient
      );
   }

   private static void handleClient(CoasterCartTrainLinkSync.CartTrainLinkSyncPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> CoasterCartTrainLinkClientState.applySync(payload.dimension(), payload.cartId(), payload.links()));
   }

   public static void broadcastCart(ServerLevel level, UUID cartId, @Nullable ServerSubLevel cart) {
      List<CoasterCartTrainLinkSync.LinkEntry> links = cart != null && CoasterCartResolve.isCartSubLevel(cart)
         ? CoasterCartTrainLinkData.readAll(cart).stream().map(CoasterCartTrainLinkSync.LinkEntry::from).toList()
         : List.of();
      PacketDistributor.sendToPlayersInDimension(
         level, new CoasterCartTrainLinkSync.CartTrainLinkSyncPayload(level.dimension(), cartId, links), new CustomPacketPayload[0]
      );
   }

   public static void broadcastCart(ServerSubLevel cart) {
      broadcastCart(cart.getLevel(), cart.getUniqueId(), cart);
   }

   public static void broadcastAllCartsInDimension(ServerLevel level) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         for (ServerSubLevel sub : container.getAllSubLevels()) {
            if (CoasterCartResolve.isCartSubLevel(sub)) {
               broadcastCart(sub);
            }
         }
      }
   }

   public static void sendCartToPlayer(ServerPlayer player, ServerSubLevel cart) {
      if (CoasterCartResolve.isCartSubLevel(cart)) {
         ServerLevel level = cart.getLevel();
         PacketDistributor.sendToPlayer(
            player,
            new CoasterCartTrainLinkSync.CartTrainLinkSyncPayload(
               level.dimension(), cart.getUniqueId(), CoasterCartTrainLinkData.readAll(cart).stream().map(CoasterCartTrainLinkSync.LinkEntry::from).toList()
            ),
            new CustomPacketPayload[0]
         );
      }
   }

   public static void sendFullSyncTo(ServerPlayer player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         for (ServerLevel level : server.getAllLevels()) {
            SubLevelContainer raw = SubLevelContainer.getContainer(level);
            if (raw instanceof ServerSubLevelContainer) {
               ServerSubLevelContainer container = (ServerSubLevelContainer)raw;

               for (ServerSubLevel sub : container.getAllSubLevels()) {
                  if (CoasterCartResolve.isCartSubLevel(sub)) {
                     sendCartToPlayer(player, sub);
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer sp && !sp.level().isClientSide()) {
         sp.getServer().execute(() -> sendFullSyncTo(sp));
         return;
      }
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer sp && !sp.level().isClientSide()) {
         sp.getServer().execute(() -> sendFullSyncTo(sp));
         return;
      }
   }

   public static record CartTrainLinkSyncPayload(ResourceKey<Level> dimension, UUID cartId, List<CoasterCartTrainLinkSync.LinkEntry> links)
      implements CustomPacketPayload {
      public static final Type<CoasterCartTrainLinkSync.CartTrainLinkSyncPayload> TYPE = new Type(
         ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "cart_train_link_sync")
      );
      private static final StreamCodec<RegistryFriendlyByteBuf, CoasterCartTrainLinkSync.LinkEntry> LINK_CODEC = StreamCodec.composite(
         UUIDUtil.STREAM_CODEC,
         CoasterCartTrainLinkSync.LinkEntry::partnerId,
         ByteBufCodecs.DOUBLE,
         CoasterCartTrainLinkSync.LinkEntry::centerDistance,
         CoasterCartTrainLinkSync.LinkEntry::new
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, CoasterCartTrainLinkSync.CartTrainLinkSyncPayload> STREAM_CODEC = StreamCodec.composite(
         ResourceKey.streamCodec(Registries.DIMENSION),
         CoasterCartTrainLinkSync.CartTrainLinkSyncPayload::dimension,
         UUIDUtil.STREAM_CODEC,
         CoasterCartTrainLinkSync.CartTrainLinkSyncPayload::cartId,
         LINK_CODEC.apply(ByteBufCodecs.list()),
         CoasterCartTrainLinkSync.CartTrainLinkSyncPayload::links,
         CoasterCartTrainLinkSync.CartTrainLinkSyncPayload::new
      );

      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static record LinkEntry(UUID partnerId, double centerDistance) {
      static CoasterCartTrainLinkSync.LinkEntry from(CoasterCartTrainLinkData.LinkRecord record) {
         return new CoasterCartTrainLinkSync.LinkEntry(record.partnerId(), record.centerDistance());
      }
   }
}
