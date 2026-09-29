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
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoasterTrackAnchorFirstFromCurvePayload(BlockPos anchor) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<CoasterTrackAnchorFirstFromCurvePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track_anchor_first_from_curve"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CoasterTrackAnchorFirstFromCurvePayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, CoasterTrackAnchorFirstFromCurvePayload::anchor, CoasterTrackAnchorFirstFromCurvePayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(CoasterTrackAnchorFirstFromCurvePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            Level level = player.level();
            BlockPos anchor = payload.anchor();
            if (!(level.getBlockState(anchor).getBlock() instanceof CoasterAnchorpointBlock)) {
                return;
            }
            BlockEntity patt1$temp = level.getBlockEntity(anchor);
            if (!(patt1$temp instanceof CoasterAnchorpointBlockEntity)) {
                return;
            }
            CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)patt1$temp;
            if (be.legCount() >= 2) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof CoasterTrackItem)) {
                return;
            }
            BlockPos existing = CoasterTrackPlacement.readAnchorFirstSelection(stack);
            if (existing != null && existing.equals((Object)anchor)) {
                return;
            }
            CoasterTrackPlacement.applyAnchorFirstSelection(level, stack, anchor, (Player)player, InteractionHand.MAIN_HAND);
        });
    }
}

