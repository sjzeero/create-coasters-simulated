/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.UUIDUtil
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track.cart;

import com.simibubi.create.AllItems;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterCartLinkDistanceEditPayload(UUID cartId, boolean shorten) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CoasterCartLinkDistanceEditPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"cart_link_distance_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoasterCartLinkDistanceEditPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)UUIDUtil.STREAM_CODEC, CoasterCartLinkDistanceEditPayload::cartId, (StreamCodec)ByteBufCodecs.BOOL, CoasterCartLinkDistanceEditPayload::shorten, CoasterCartLinkDistanceEditPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(CoasterCartLinkDistanceEditPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
                return;
            }
            ServerLevel level = player.serverLevel();
            ServerSubLevel cart = CoasterCartResolve.findCartById(level, payload.cartId());
            if (cart == null || cart.isRemoved()) {
                return;
            }
            Vec3 center = CoasterCartLinkPlacement.partnerBearingCenterWorld((SubLevel)cart, null);
            if (center == null) {
                return;
            }
            double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
            if (player.getEyePosition().distanceToSqr(center) > reach * reach) {
                return;
            }
            CoasterCartTrainLinkConstraint.adjustEditModeLinkDistances(level, cart, payload.shorten());
        });
    }
}

