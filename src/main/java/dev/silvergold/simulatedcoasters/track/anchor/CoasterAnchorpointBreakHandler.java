/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.level.BlockEvent$BreakEvent
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurveDrops;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid="simulatedcoasters")
public final class CoasterAnchorpointBreakHandler {
    private CoasterAnchorpointBreakHandler() {
    }

    @SubscribeEvent
    public static void onAnchorpointBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) {
            return;
        }
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel)levelAccessor;
        if (!(event.getState().getBlock() instanceof CoasterAnchorpointBlock)) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || player.isCreative()) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return;
        }
        CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
        if (anchor.getAnchorPeerCurvesView().isEmpty()) {
            return;
        }
        for (BezierConnection bc : anchor.getAnchorPeerCurvesView().values()) {
            if (bc == null) continue;
            BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
            CoasterTrackCurveDrops.spawnItems((Level)level, primary);
            primary.spawnDestroyParticles((Level)level);
        }
    }
}

