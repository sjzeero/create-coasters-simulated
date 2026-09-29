/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.UUIDUtil
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.PacketDistributor
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.silvergold.simulatedcoasters.client.rivet.RivetPlacementAnimationClient;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RivetPlaceAnimPayload(UUID rivetSubLevelId, Vec3 seatWorld, Vec3 faceOutwardWorld, BlockState hostBlockState) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<RivetPlaceAnimPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"rivet_place_anim"));
    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.DOUBLE, Vec3::x, (StreamCodec)ByteBufCodecs.DOUBLE, Vec3::y, (StreamCodec)ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, BlockState> BLOCK_STATE_CODEC = StreamCodec.of((buf, state) -> buf.writeVarInt(Block.getId((BlockState)state)), buf -> Block.stateById((int)buf.readVarInt()));
    public static final StreamCodec<RegistryFriendlyByteBuf, RivetPlaceAnimPayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)UUIDUtil.STREAM_CODEC, RivetPlaceAnimPayload::rivetSubLevelId, VEC3_CODEC, RivetPlaceAnimPayload::seatWorld, VEC3_CODEC, RivetPlaceAnimPayload::faceOutwardWorld, BLOCK_STATE_CODEC, RivetPlaceAnimPayload::hostBlockState, RivetPlaceAnimPayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(RivetPlaceAnimPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> RivetPlacementAnimationClient.begin(payload));
    }

    public static void notifyPlacement(ServerLevel level, UUID rivetSubLevelId, RivetPlacement.Resolved placement) {
        BlockState hostState = level.getBlockState(placement.host());
        PacketDistributor.sendToPlayersInDimension((ServerLevel)level, (CustomPacketPayload)new RivetPlaceAnimPayload(rivetSubLevelId, RivetPlacement.faceAnchorWorld((Level)level, placement), RivetPlacement.hostFaceOutwardWorld((Level)level, placement), hostState), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

