/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerChangedDimensionEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent
 *  net.neoforged.neoforge.network.PacketDistributor
 *  net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  net.neoforged.neoforge.network.registration.PayloadRegistrar
 */
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
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathSavedData;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid="simulatedcoasters")
public final class CoasterPathSync {
    private static final Set<ResourceKey<Level>> PENDING_BROADCAST = ConcurrentHashMap.newKeySet();

    private CoasterPathSync() {
    }

    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("simulatedcoasters");
        reg.playToClient(CoasterGraphSyncPayload.TYPE, CoasterGraphSyncPayload.STREAM_CODEC, CoasterPathSync::handleClient);
        reg.playToServer(AnchorPeerCurveDestroyPayload.TYPE, AnchorPeerCurveDestroyPayload.STREAM_CODEC, AnchorPeerCurveDestroyPayload::handleOnServer);
        reg.playToServer(BezierHandleEditPayload.TYPE, BezierHandleEditPayload.STREAM_CODEC, BezierHandleEditPayload::handleOnServer);
        reg.playToServer(BezierTiltEditPayload.TYPE, BezierTiltEditPayload.STREAM_CODEC, BezierTiltEditPayload::handleOnServer);
        reg.playToServer(BezierLiftEditPayload.TYPE, BezierLiftEditPayload.STREAM_CODEC, BezierLiftEditPayload::handleOnServer);
        reg.playToServer(BezierOpenEndExtensionEditPayload.TYPE, BezierOpenEndExtensionEditPayload.STREAM_CODEC, BezierOpenEndExtensionEditPayload::handleOnServer);
        reg.playToServer(ClearCoasterTrackHotbarSelectionPayload.TYPE, ClearCoasterTrackHotbarSelectionPayload.STREAM_CODEC, ClearCoasterTrackHotbarSelectionPayload::handleOnServer);
        reg.playToServer(CoasterCartLinkSnapSuppressPayload.TYPE, CoasterCartLinkSnapSuppressPayload.STREAM_CODEC, CoasterCartLinkSnapSuppressPayload::handleOnServer);
        reg.playToServer(CoasterTrackPlacementNeighborSmoothSuppressPayload.TYPE, CoasterTrackPlacementNeighborSmoothSuppressPayload.STREAM_CODEC, CoasterTrackPlacementNeighborSmoothSuppressPayload::handleOnServer);
        reg.playToServer(CoasterCartMidTrackPlacePayload.TYPE, CoasterCartMidTrackPlacePayload.STREAM_CODEC, CoasterCartMidTrackPlacePayload::handleOnServer);
        reg.playToServer(CoasterCartLinkDistanceEditPayload.TYPE, CoasterCartLinkDistanceEditPayload.STREAM_CODEC, CoasterCartLinkDistanceEditPayload::handleOnServer);
        reg.playToServer(CoasterCurveDyeApplyPayload.TYPE, CoasterCurveDyeApplyPayload.STREAM_CODEC, CoasterCurveDyeApplyPayload::handleOnServer);
        reg.playToServer(CoasterCurveUndyeApplyPayload.TYPE, CoasterCurveUndyeApplyPayload.STREAM_CODEC, CoasterCurveUndyeApplyPayload::handleOnServer);
        reg.playToServer(CoasterTrackAnchorFirstFromCurvePayload.TYPE, CoasterTrackAnchorFirstFromCurvePayload.STREAM_CODEC, CoasterTrackAnchorFirstFromCurvePayload::handleOnServer);
        reg.playToServer(CoasterTrackAnchorSecondFromCurvePayload.TYPE, CoasterTrackAnchorSecondFromCurvePayload.STREAM_CODEC, CoasterTrackAnchorSecondFromCurvePayload::handleOnServer);
        reg.playToClient(CoasterTrackHotbarSelectionSyncPayload.TYPE, CoasterTrackHotbarSelectionSyncPayload.STREAM_CODEC, CoasterTrackHotbarSelectionSyncPayload::handleOnClient);
        reg.playToServer(ChainLiftCommitPayload.TYPE, ChainLiftCommitPayload.STREAM_CODEC, ChainLiftCommitPayload::handleOnServer);
        reg.playToServer(ChainLiftShaftCyclePayload.TYPE, ChainLiftShaftCyclePayload.STREAM_CODEC, ChainLiftShaftCyclePayload::handleOnServer);
        reg.playToServer(ChainLiftRemovePayload.TYPE, ChainLiftRemovePayload.STREAM_CODEC, ChainLiftRemovePayload::handleOnServer);
        reg.playToServer(RivetPlacePayload.TYPE, RivetPlacePayload.STREAM_CODEC, RivetPlacePayload::handleOnServer);
        reg.playToClient(RivetPlaceAnimPayload.TYPE, RivetPlaceAnimPayload.STREAM_CODEC, RivetPlaceAnimPayload::handleOnClient);
    }

    private static void handleClient(CoasterGraphSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            CoasterPathGraphManagerClient.applyClientSync(payload.root());
            ChainLiftSpanPathCache.invalidateAll();
        });
    }

    public static void broadcastDimension(MinecraftServer server, ResourceKey<Level> dimension) {
        CompoundTag root = CoasterPathSync.writeDimensionsTag((HolderLookup.Provider)server.registryAccess(), CoasterPathSavedData.get(server), dimension);
        PacketDistributor.sendToAllPlayers((CustomPacketPayload)new CoasterGraphSyncPayload(root), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void scheduleBroadcastDimension(MinecraftServer server, ResourceKey<Level> dimension) {
        PENDING_BROADCAST.add(dimension);
    }

    public static void flushPendingBroadcasts(MinecraftServer server) {
        if (PENDING_BROADCAST.isEmpty()) {
            return;
        }
        Set<ResourceKey<Level>> dims = Set.copyOf(PENDING_BROADCAST);
        PENDING_BROADCAST.clear();
        for (ResourceKey<Level> dim : dims) {
            CoasterPathSync.broadcastDimension(server, dim);
        }
    }

    public static void sendFullSyncTo(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        CompoundTag root = CoasterPathSync.writeDimensionsTag((HolderLookup.Provider)server.registryAccess(), CoasterPathSavedData.get(server), null);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new CoasterGraphSyncPayload(root), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private static CompoundTag writeDimensionsTag(HolderLookup.Provider registries, CoasterPathSavedData data, ResourceKey<Level> onlyDimension) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<ResourceKey<Level>, CoasterPathGraph> e : data.allGraphs().entrySet()) {
            if (onlyDimension != null && !e.getKey().equals(onlyDimension)) continue;
            CompoundTag dim = new CompoundTag();
            dim.putString("Dimension", e.getKey().location().toString());
            dim.put("Graph", (Tag)e.getValue().write(registries));
            list.add((Object)dim);
        }
        root.put("Dimensions", (Tag)list);
        return root;
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        CoasterCartLinkSnap.clearPlayer(event.getEntity().getUUID());
        CoasterTrackPlacementNeighborSmoothSuppress.clearPlayer(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer sp;
        Player player = event.getEntity();
        if (player instanceof ServerPlayer && !(sp = (ServerPlayer)player).level().isClientSide()) {
            CoasterPathSync.sendFullSyncTo(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ServerPlayer sp;
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer) || (sp = (ServerPlayer)player).level().isClientSide()) {
            return;
        }
        MinecraftServer server = sp.getServer();
        if (server == null) {
            return;
        }
        CompoundTag root = CoasterPathSync.writeDimensionsTag((HolderLookup.Provider)server.registryAccess(), CoasterPathSavedData.get(server), (ResourceKey<Level>)event.getTo());
        PacketDistributor.sendToPlayer((ServerPlayer)sp, (CustomPacketPayload)new CoasterGraphSyncPayload(root), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public record CoasterGraphSyncPayload(CompoundTag root) implements CustomPacketPayload
    {
        public static final CustomPacketPayload.Type<CoasterGraphSyncPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_paths_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CoasterGraphSyncPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.COMPOUND_TAG, CoasterGraphSyncPayload::root, CoasterGraphSyncPayload::new);

        public CustomPacketPayload.Type<CoasterGraphSyncPayload> type() {
            return TYPE;
        }
    }
}

