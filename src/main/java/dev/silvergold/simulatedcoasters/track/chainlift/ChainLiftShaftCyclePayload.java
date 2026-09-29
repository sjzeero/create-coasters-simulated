/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import com.simibubi.create.AllItems;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChainLiftShaftCyclePayload(BlockPos anchorPos) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ChainLiftShaftCyclePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"chain_lift_shaft_cycle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainLiftShaftCyclePayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, ChainLiftShaftCyclePayload::anchorPos, ChainLiftShaftCyclePayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(ChainLiftShaftCyclePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
                return;
            }
            int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
            if (!player.canInteractWithBlock(payload.anchorPos(), (double)maxRange)) {
                return;
            }
            BlockEntity patt1$temp = player.level().getBlockEntity(payload.anchorPos());
            if (!(patt1$temp instanceof CoasterAnchorpointBlockEntity)) {
                return;
            }
            CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)patt1$temp;
            if (!be.hasChainLiftShaft()) {
                return;
            }
            be.cycleChainLiftShaftAxis();
        });
    }
}

