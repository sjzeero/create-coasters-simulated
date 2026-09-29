package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.rivet.RivetPlaceAnimPayload;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacePayload;
import dev.silvergold.simulatedcoasters.track.BezierHandleEditPayload;
import dev.silvergold.simulatedcoasters.track.BezierLiftEditPayload;
import dev.silvergold.simulatedcoasters.track.BezierOpenEndExtensionEditPayload;
import dev.silvergold.simulatedcoasters.track.BezierTiltEditPayload;
import dev.silvergold.simulatedcoasters.track.ClearCoasterTrackHotbarSelectionPayload;
import dev.silvergold.simulatedcoasters.track.CoasterCurveDyeApplyPayload;
import dev.silvergold.simulatedcoasters.track.CoasterCurveUndyeApplyPayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackAnchorFirstFromCurvePayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackAnchorSecondFromCurvePayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackHotbarSelectionSyncPayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacementNeighborSmoothSuppress;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacementNeighborSmoothSuppressPayload;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveDestroyPayload;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkDistanceEditPayload;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSnap;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSnapSuppressPayload;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacePayload;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftCommitPayload;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftRemovePayload;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftShaftCyclePayload;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(
   modid = "simulatedcoasters"
)
public final class CoasterPathSync {
   private static final Set<ResourceKey<Level>> PENDING_BROADCAST = ConcurrentHashMap.newKeySet();

   private CoasterPathSync() {
   }

   public static void registerPayload(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar reg = event.registrar("simulatedcoasters");
      reg.playToClient(CoasterPathSync.CoasterGraphSyncPayload.TYPE, CoasterPathSync.CoasterGraphSyncPayload.STREAM_CODEC, CoasterPathSync::handleClient);
      reg.playToServer(AnchorPeerCurveDestroyPayload.TYPE, AnchorPeerCurveDestroyPayload.STREAM_CODEC, AnchorPeerCurveDestroyPayload::handleOnServer);
      reg.playToServer(BezierHandleEditPayload.TYPE, BezierHandleEditPayload.STREAM_CODEC, BezierHandleEditPayload::handleOnServer);
      reg.playToServer(BezierTiltEditPayload.TYPE, BezierTiltEditPayload.STREAM_CODEC, BezierTiltEditPayload::handleOnServer);
      reg.playToServer(BezierLiftEditPayload.TYPE, BezierLiftEditPayload.STREAM_CODEC, BezierLiftEditPayload::handleOnServer);
      reg.playToServer(
         BezierOpenEndExtensionEditPayload.TYPE, BezierOpenEndExtensionEditPayload.STREAM_CODEC, BezierOpenEndExtensionEditPayload::handleOnServer
      );
      reg.playToServer(
         ClearCoasterTrackHotbarSelectionPayload.TYPE,
         ClearCoasterTrackHotbarSelectionPayload.STREAM_CODEC,
         ClearCoasterTrackHotbarSelectionPayload::handleOnServer
      );
      reg.playToServer(
         CoasterCartLinkSnapSuppressPayload.TYPE, CoasterCartLinkSnapSuppressPayload.STREAM_CODEC, CoasterCartLinkSnapSuppressPayload::handleOnServer
      );
      reg.playToServer(
         CoasterTrackPlacementNeighborSmoothSuppressPayload.TYPE,
         CoasterTrackPlacementNeighborSmoothSuppressPayload.STREAM_CODEC,
         CoasterTrackPlacementNeighborSmoothSuppressPayload::handleOnServer
      );
      reg.playToServer(CoasterCartMidTrackPlacePayload.TYPE, CoasterCartMidTrackPlacePayload.STREAM_CODEC, CoasterCartMidTrackPlacePayload::handleOnServer);
      reg.playToServer(
         CoasterCartLinkDistanceEditPayload.TYPE, CoasterCartLinkDistanceEditPayload.STREAM_CODEC, CoasterCartLinkDistanceEditPayload::handleOnServer
      );
      reg.playToServer(CoasterCurveDyeApplyPayload.TYPE, CoasterCurveDyeApplyPayload.STREAM_CODEC, CoasterCurveDyeApplyPayload::handleOnServer);
      reg.playToServer(CoasterCurveUndyeApplyPayload.TYPE, CoasterCurveUndyeApplyPayload.STREAM_CODEC, CoasterCurveUndyeApplyPayload::handleOnServer);
      reg.playToServer(
         CoasterTrackAnchorFirstFromCurvePayload.TYPE,
         CoasterTrackAnchorFirstFromCurvePayload.STREAM_CODEC,
         CoasterTrackAnchorFirstFromCurvePayload::handleOnServer
      );
      reg.playToServer(
         CoasterTrackAnchorSecondFromCurvePayload.TYPE,
         CoasterTrackAnchorSecondFromCurvePayload.STREAM_CODEC,
         CoasterTrackAnchorSecondFromCurvePayload::handleOnServer
      );
      reg.playToClient(
         CoasterTrackHotbarSelectionSyncPayload.TYPE,
         CoasterTrackHotbarSelectionSyncPayload.STREAM_CODEC,
         CoasterTrackHotbarSelectionSyncPayload::handleOnClient
      );
      reg.playToServer(ChainLiftCommitPayload.TYPE, ChainLiftCommitPayload.STREAM_CODEC, ChainLiftCommitPayload::handleOnServer);
      reg.playToServer(ChainLiftShaftCyclePayload.TYPE, ChainLiftShaftCyclePayload.STREAM_CODEC, ChainLiftShaftCyclePayload::handleOnServer);
      reg.playToServer(ChainLiftRemovePayload.TYPE, ChainLiftRemovePayload.STREAM_CODEC, ChainLiftRemovePayload::handleOnServer);
      reg.playToServer(RivetPlacePayload.TYPE, RivetPlacePayload.STREAM_CODEC, RivetPlacePayload::handleOnServer);
      reg.playToClient(RivetPlaceAnimPayload.TYPE, RivetPlaceAnimPayload.STREAM_CODEC, RivetPlaceAnimPayload::handleOnClient);
   }

   private static void handleClient(CoasterPathSync.CoasterGraphSyncPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> {
         CoasterPathGraphManagerClient.applyClientSync(payload.root());
         ChainLiftSpanPathCache.invalidateAll();
      });
   }

   public static void broadcastDimension(MinecraftServer server, ResourceKey<Level> dimension) {
      CompoundTag root = writeDimensionsTag(server.registryAccess(), CoasterPathSavedData.get(server), dimension);
      PacketDistributor.sendToAllPlayers(new CoasterPathSync.CoasterGraphSyncPayload(root), new CustomPacketPayload[0]);
   }

   public static void scheduleBroadcastDimension(MinecraftServer server, ResourceKey<Level> dimension) {
      PENDING_BROADCAST.add(dimension);
   }

   public static void flushPendingBroadcasts(MinecraftServer server) {
      if (!PENDING_BROADCAST.isEmpty()) {
         Set<ResourceKey<Level>> dims = Set.copyOf(PENDING_BROADCAST);
         PENDING_BROADCAST.clear();

         for (ResourceKey<Level> dim : dims) {
            broadcastDimension(server, dim);
         }
      }
   }

   public static void sendFullSyncTo(ServerPlayer player) {
      MinecraftServer server = player.getServer();
      if (server != null) {
         CompoundTag root = writeDimensionsTag(server.registryAccess(), CoasterPathSavedData.get(server), null);
         PacketDistributor.sendToPlayer(player, new CoasterPathSync.CoasterGraphSyncPayload(root), new CustomPacketPayload[0]);
      }
   }

   private static CompoundTag writeDimensionsTag(Provider registries, CoasterPathSavedData data, ResourceKey<Level> onlyDimension) {
      CompoundTag root = new CompoundTag();
      ListTag list = new ListTag();

      for (Entry<ResourceKey<Level>, CoasterPathGraph> e : data.allGraphs().entrySet()) {
         if (onlyDimension == null || e.getKey().equals(onlyDimension)) {
            CompoundTag dim = new CompoundTag();
            dim.putString("Dimension", e.getKey().location().toString());
            dim.put("Graph", e.getValue().write(registries));
            list.add(dim);
         }
      }

      root.put("Dimensions", list);
      return root;
   }

   @SubscribeEvent
   public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
      CoasterCartLinkSnap.clearPlayer(event.getEntity().getUUID());
      CoasterTrackPlacementNeighborSmoothSuppress.clearPlayer(event.getEntity().getUUID());
   }

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer sp && !sp.level().isClientSide()) {
         sendFullSyncTo(sp);
      }
   }

   @SubscribeEvent
   public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer sp && !sp.level().isClientSide()) {
         MinecraftServer server = sp.getServer();
         if (server == null) {
            return;
         }

         CompoundTag root = writeDimensionsTag(server.registryAccess(), CoasterPathSavedData.get(server), event.getTo());
         PacketDistributor.sendToPlayer(sp, new CoasterPathSync.CoasterGraphSyncPayload(root), new CustomPacketPayload[0]);
         return;
      }
   }

   public static record CoasterGraphSyncPayload(CompoundTag root) implements CustomPacketPayload {
      public static final Type<CoasterPathSync.CoasterGraphSyncPayload> TYPE = new Type(
         ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_paths_sync")
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, CoasterPathSync.CoasterGraphSyncPayload> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.COMPOUND_TAG, CoasterPathSync.CoasterGraphSyncPayload::root, CoasterPathSync.CoasterGraphSyncPayload::new
      );

      public Type<CoasterPathSync.CoasterGraphSyncPayload> type() {
         return TYPE;
      }
   }
}
