/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.rivet.RivetPlaceAnimPayload;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import dev.silvergold.simulatedcoasters.rivet.RivetSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RivetPlacePayload(BlockPos host, Direction face, Vec3 hitLocation, boolean freePosition, int yRotationDegrees, InteractionHand hand) implements CustomPacketPayload
{
    public static final RivetPlacePayload INVALID = new RivetPlacePayload(BlockPos.ZERO, Direction.UP, Vec3.ZERO, false, 0, InteractionHand.MAIN_HAND);
    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.DOUBLE, Vec3::x, (StreamCodec)ByteBufCodecs.DOUBLE, Vec3::y, (StreamCodec)ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);
    private static final StreamCodec<RegistryFriendlyByteBuf, InteractionHand> HAND_CODEC = StreamCodec.of((buf, hand) -> buf.writeVarInt(hand == InteractionHand.OFF_HAND ? 1 : 0), buf -> buf.readVarInt() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    public static final CustomPacketPayload.Type<RivetPlacePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"rivet_place"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RivetPlacePayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, RivetPlacePayload::host, (StreamCodec)Direction.STREAM_CODEC, RivetPlacePayload::face, VEC3_CODEC, RivetPlacePayload::hitLocation, (StreamCodec)ByteBufCodecs.BOOL, RivetPlacePayload::freePosition, (StreamCodec)ByteBufCodecs.VAR_INT, RivetPlacePayload::yRotationDegrees, HAND_CODEC, RivetPlacePayload::hand, RivetPlacePayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(RivetPlacePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            ServerLevel level = player.serverLevel();
            ItemStack stack = player.getItemInHand(payload.hand());
            if (!stack.is(SimulatedCoasters.RIVET)) {
                return;
            }
            if (!player.mayBuild()) {
                return;
            }
            BlockHitResult hit = RivetPlacement.findPlacementHit((Level)level, (Entity)player);
            if (hit == null) {
                return;
            }
            if (RivetPlacement.looksAtExistingRivet((Level)level, (Entity)player)) {
                return;
            }
            RivetPlacement.Resolved placement = RivetPlacement.resolve(hit, payload.freePosition()).withYRotation(payload.yRotationDegrees());
            if (!RivetPlacement.isValidPlacement((Level)level, placement)) {
                return;
            }
            ServerSubLevel placed = RivetSpawner.tryPlace(level, placement);
            if (placed == null) {
                return;
            }
            RivetPlaceAnimPayload.notifyPlacement(level, placed.getUniqueId(), placement);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.swing(payload.hand(), true);
        });
    }
}

