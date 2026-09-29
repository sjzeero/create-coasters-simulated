package dev.silvergold.simulatedcoasters.rivet;

import dev.silvergold.simulatedcoasters.client.rivet.RivetPlacementAnimationClient;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RivetPlaceAnimPayload(UUID rivetSubLevelId, Vec3 seatWorld, Vec3 faceOutwardWorld, BlockState hostBlockState) implements CustomPacketPayload {
   public static final Type<RivetPlaceAnimPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "rivet_place_anim"));
   private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite(
      ByteBufCodecs.DOUBLE, Vec3::x, ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new
   );
   private static final StreamCodec<RegistryFriendlyByteBuf, BlockState> BLOCK_STATE_CODEC = StreamCodec.of(
      (buf, state) -> buf.writeVarInt(Block.getId(state)), buf -> Block.stateById(buf.readVarInt())
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, RivetPlaceAnimPayload> STREAM_CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC,
      RivetPlaceAnimPayload::rivetSubLevelId,
      VEC3_CODEC,
      RivetPlaceAnimPayload::seatWorld,
      VEC3_CODEC,
      RivetPlaceAnimPayload::faceOutwardWorld,
      BLOCK_STATE_CODEC,
      RivetPlaceAnimPayload::hostBlockState,
      RivetPlaceAnimPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void handleOnClient(RivetPlaceAnimPayload payload, IPayloadContext ctx) {
      ctx.enqueueWork(() -> RivetPlacementAnimationClient.begin(payload));
   }

   public static void notifyPlacement(ServerLevel level, UUID rivetSubLevelId, RivetPlacement.Resolved placement) {
      BlockState hostState = level.getBlockState(placement.host());
      PacketDistributor.sendToPlayersInDimension(
         level,
         new RivetPlaceAnimPayload(
            rivetSubLevelId, RivetPlacement.faceAnchorWorld(level, placement), RivetPlacement.hostFaceOutwardWorld(level, placement), hostState
         ),
         new CustomPacketPayload[0]
      );
   }
}
