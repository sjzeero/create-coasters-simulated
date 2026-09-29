/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftEndRef;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftPlacementSounds;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftShaftAxis;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpan;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanOverlap;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record ChainLiftCommitPayload(BlockPos startAnchor, ChainLiftEndRef endRef, Vec3 shaftAxis) implements CustomPacketPayload
{
    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.DOUBLE, v -> v.x, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.y, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new);
    public static final CustomPacketPayload.Type<ChainLiftCommitPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"chain_lift_commit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainLiftCommitPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, ChainLiftCommitPayload::startAnchor, ChainLiftEndRef.STREAM_CODEC, ChainLiftCommitPayload::endRef, VEC3_CODEC, ChainLiftCommitPayload::shaftAxis, ChainLiftCommitPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(ChainLiftCommitPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Direction facing;
            BlockPos start;
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            if (!player.getMainHandItem().is(Items.CHAIN) && !player.getOffhandItem().is(Items.CHAIN)) {
                return;
            }
            Level level = player.level();
            if (!(level.getBlockState(start = payload.startAnchor().immutable()).getBlock() instanceof CoasterAnchorpointBlock)) {
                return;
            }
            BlockEntity patt1$temp = level.getBlockEntity(start);
            if (!(patt1$temp instanceof CoasterAnchorpointBlockEntity)) {
                return;
            }
            CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)patt1$temp;
            if (be.hasChainLiftShaft()) {
                return;
            }
            Vec3 shaftAxis = payload.shaftAxis();
            if (!ChainLiftShaftAxis.isValidForFacing(shaftAxis, facing = (Direction)level.getBlockState(start).getValue((Property)CoasterAnchorpointBlock.FACING))) {
                return;
            }
            @Nullable CoasterPathGraphSpans.SpanPath spanPath = ChainLiftSpan.resolveIdealPath(level, CoasterPathGraphLookup.get(level), start, payload.endRef(), 0.0);
            if (spanPath == null || !CoasterPathGraphSpans.isWithinChainLiftSpan(spanPath.totalLength())) {
                return;
            }
            if (ChainLiftSpanOverlap.overlapsAnyExisting(level, spanPath, start, 0.0)) {
                return;
            }
            be.commitChainLift(ChainLiftShaftAxis.toDominantAxis(shaftAxis), payload.endRef(), spanPath);
            ChainLiftPlacementSounds.playChainCommit(level, ChainLiftPlacementSounds.spanEndWorldPos(level, spanPath));
        });
    }
}

