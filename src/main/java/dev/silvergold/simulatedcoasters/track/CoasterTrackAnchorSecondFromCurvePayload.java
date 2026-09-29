/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterTrackAnchorSecondFromCurvePayload(BlockPos secondAnchor, BlockPos firstAnchor) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CoasterTrackAnchorSecondFromCurvePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track_anchor_second_from_curve"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoasterTrackAnchorSecondFromCurvePayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, CoasterTrackAnchorSecondFromCurvePayload::secondAnchor, (StreamCodec)BlockPos.STREAM_CODEC, CoasterTrackAnchorSecondFromCurvePayload::firstAnchor, CoasterTrackAnchorSecondFromCurvePayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(CoasterTrackAnchorSecondFromCurvePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof CoasterTrackItem)) {
                return;
            }
            BlockPos first = payload.firstAnchor();
            if (first == null || first.equals((Object)BlockPos.ZERO)) {
                return;
            }
            CoasterTrackPlacement.commitSecondAnchorOnServer(player, InteractionHand.MAIN_HAND, first, payload.secondAnchor());
        });
    }
}

