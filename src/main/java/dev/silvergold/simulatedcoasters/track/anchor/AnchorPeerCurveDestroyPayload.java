/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllSoundEvents
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurveDrops;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AnchorPeerCurveDestroyPayload(BlockPos minedAnchor, BlockPos peerAnchor, boolean wrench) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<AnchorPeerCurveDestroyPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"anchor_peer_curve_destroy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorPeerCurveDestroyPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, AnchorPeerCurveDestroyPayload::minedAnchor, (StreamCodec)BlockPos.STREAM_CODEC, AnchorPeerCurveDestroyPayload::peerAnchor, (StreamCodec)ByteBufCodecs.BOOL, AnchorPeerCurveDestroyPayload::wrench, AnchorPeerCurveDestroyPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(AnchorPeerCurveDestroyPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            ServerLevel level = player.serverLevel();
            int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
            if (!player.canInteractWithBlock(payload.minedAnchor(), (double)maxRange)) {
                return;
            }
            BlockEntity patt1$temp = level.getBlockEntity(payload.minedAnchor());
            if (!(patt1$temp instanceof CoasterAnchorpointBlockEntity)) {
                return;
            }
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)patt1$temp;
            Map<BlockPos, BezierConnection> curves = anchor.getAnchorPeerCurvesView();
            if (!curves.containsKey(payload.peerAnchor())) {
                return;
            }
            BezierConnection bc = curves.get(payload.peerAnchor());
            BlockState anchorState = anchor.getBlockState();
            SoundType sound = anchorState.getSoundType((LevelReader)level, payload.minedAnchor(), (Entity)player);
            if (!player.isCreative()) {
                if (payload.wrench()) {
                    CoasterTrackCurveDrops.addItemsToPlayer((Player)player, bc);
                } else {
                    CoasterTrackCurveDrops.spawnItems((Level)level, bc);
                }
            }
            bc.spawnDestroyParticles((Level)level);
            if (payload.wrench()) {
                AllSoundEvents.WRENCH_REMOVE.playOnServer((Level)level, (Vec3i)payload.minedAnchor(), 1.0f, level.random.nextFloat() * 0.5f + 0.5f);
            } else if (sound != null) {
                level.playSound(null, payload.minedAnchor(), sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
            }
            anchor.removeAnchorPeerCurve(level, payload.peerAnchor());
        });
    }
}

