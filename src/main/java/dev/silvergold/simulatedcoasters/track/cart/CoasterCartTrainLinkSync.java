/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.UUIDUtil
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerChangedDimensionEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent
 *  net.neoforged.neoforge.network.PacketDistributor
 *  net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  net.neoforged.neoforge.network.registration.PayloadRegistrar
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainLinkClientState;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkData;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid="simulatedcoasters")
public final class CoasterCartTrainLinkSync {
    private CoasterCartTrainLinkSync() {
    }

    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("simulatedcoasters");
        reg.playToClient(CartTrainLinkSyncPayload.TYPE, CartTrainLinkSyncPayload.STREAM_CODEC, CoasterCartTrainLinkSync::handleClient);
    }

    private static void handleClient(CartTrainLinkSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CoasterCartTrainLinkClientState.applySync(payload.dimension(), payload.cartId(), payload.links()));
    }

    public static void broadcastCart(ServerLevel level, UUID cartId, @Nullable ServerSubLevel cart) {
        List<LinkEntry> links = cart != null && CoasterCartResolve.isCartSubLevel((SubLevel)cart) ? CoasterCartTrainLinkData.readAll(cart).stream().map(LinkEntry::from).toList() : List.of();
        PacketDistributor.sendToPlayersInDimension((ServerLevel)level, (CustomPacketPayload)new CartTrainLinkSyncPayload((ResourceKey<Level>)level.dimension(), cartId, links), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void broadcastCart(ServerSubLevel cart) {
        CoasterCartTrainLinkSync.broadcastCart(cart.getLevel(), cart.getUniqueId(), cart);
    }

    public static void broadcastAllCartsInDimension(ServerLevel level) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        for (ServerSubLevel sub : container.getAllSubLevels()) {
            if (!CoasterCartResolve.isCartSubLevel((SubLevel)sub)) continue;
            CoasterCartTrainLinkSync.broadcastCart(sub);
        }
    }

    public static void sendCartToPlayer(ServerPlayer player, ServerSubLevel cart) {
        if (!CoasterCartResolve.isCartSubLevel((SubLevel)cart)) {
            return;
        }
        ServerLevel level = cart.getLevel();
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new CartTrainLinkSyncPayload((ResourceKey<Level>)level.dimension(), cart.getUniqueId(), CoasterCartTrainLinkData.readAll(cart).stream().map(LinkEntry::from).toList()), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void sendFullSyncTo(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
            if (!(raw instanceof ServerSubLevelContainer)) continue;
            ServerSubLevelContainer container = raw;
            for (ServerSubLevel sub : container.getAllSubLevels()) {
                if (!CoasterCartResolve.isCartSubLevel((SubLevel)sub)) continue;
                CoasterCartTrainLinkSync.sendCartToPlayer(player, sub);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer sp;
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer) || (sp = (ServerPlayer)player).level().isClientSide()) {
            return;
        }
        sp.getServer().execute(() -> CoasterCartTrainLinkSync.sendFullSyncTo(sp));
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ServerPlayer sp;
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer) || (sp = (ServerPlayer)player).level().isClientSide()) {
            return;
        }
        sp.getServer().execute(() -> CoasterCartTrainLinkSync.sendFullSyncTo(sp));
    }

    public record CartTrainLinkSyncPayload(ResourceKey<Level> dimension, UUID cartId, List<LinkEntry> links) implements CustomPacketPayload
    {
        public static final CustomPacketPayload.Type<CartTrainLinkSyncPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"cart_train_link_sync"));
        private static final StreamCodec<RegistryFriendlyByteBuf, LinkEntry> LINK_CODEC = StreamCodec.composite((StreamCodec)UUIDUtil.STREAM_CODEC, LinkEntry::partnerId, (StreamCodec)ByteBufCodecs.DOUBLE, LinkEntry::centerDistance, LinkEntry::new);
        public static final StreamCodec<RegistryFriendlyByteBuf, CartTrainLinkSyncPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)ResourceKey.streamCodec((ResourceKey)Registries.DIMENSION), CartTrainLinkSyncPayload::dimension, (StreamCodec)UUIDUtil.STREAM_CODEC, CartTrainLinkSyncPayload::cartId, (StreamCodec)LINK_CODEC.apply(ByteBufCodecs.list()), CartTrainLinkSyncPayload::links, CartTrainLinkSyncPayload::new);

        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record LinkEntry(UUID partnerId, double centerDistance) {
        static LinkEntry from(CoasterCartTrainLinkData.LinkRecord record) {
            return new LinkEntry(record.partnerId(), record.centerDistance());
        }
    }
}

