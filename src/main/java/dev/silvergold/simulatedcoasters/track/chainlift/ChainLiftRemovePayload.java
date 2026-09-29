/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  com.simibubi.create.AllSoundEvents
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChainLiftRemovePayload(BlockPos anchorPos, Vec3 soundWorldPos) implements CustomPacketPayload
{
    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.DOUBLE, v -> v.x, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.y, (StreamCodec)ByteBufCodecs.DOUBLE, v -> v.z, Vec3::new);
    public static final CustomPacketPayload.Type<ChainLiftRemovePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"chain_lift_remove"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainLiftRemovePayload> STREAM_CODEC = StreamCodec.composite((StreamCodec)BlockPos.STREAM_CODEC, ChainLiftRemovePayload::anchorPos, VEC3_CODEC, ChainLiftRemovePayload::soundWorldPos, ChainLiftRemovePayload::new);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(ChainLiftRemovePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            boolean nearClick;
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            if (!player.isShiftKeyDown()) {
                return;
            }
            if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
                return;
            }
            int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
            Vec3 sound = payload.soundWorldPos();
            if (!(Double.isFinite(sound.x) && Double.isFinite(sound.y) && Double.isFinite(sound.z))) {
                return;
            }
            boolean nearHost = player.canInteractWithBlock(payload.anchorPos(), (double)maxRange);
            boolean bl = nearClick = player.position().distanceToSqr(sound) <= (double)maxRange * (double)maxRange;
            if (!nearHost && !nearClick) {
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
            be.clearChainLiftShaft();
            float pitch = player.serverLevel().random.nextFloat() * 0.5f + 0.5f;
            AllSoundEvents.WRENCH_REMOVE.play((Level)player.serverLevel(), null, sound, 1.0f, pitch);
        });
    }
}

