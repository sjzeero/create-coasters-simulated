/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.Config;
import dev.silvergold.simulatedcoasters.physics.UniversalDragCompensation;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class RivetCartDragCompensation {
    private RivetCartDragCompensation() {
    }

    public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        double fraction = Config.universalDragCompensationFraction();
        if (fraction <= 0.0) {
            return;
        }
        SubLevelPhysicsSystem physics = event.getPhysicsSystem();
        ServerLevel level = physics.getLevel();
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        double dt = event.getTimeStep();
        for (ServerSubLevel sub : container.getAllSubLevels()) {
            RivetBlockEntity be;
            if (sub.isRemoved() || !RivetHostKey.isRivetSubLevel((SubLevel)sub) || (be = RivetCartDragCompensation.rivetBlockEntity(sub)) == null || !be.compensatesCartHostUniversalDrag()) continue;
            UniversalDragCompensation.applyPartial(level, physics, sub, dt, fraction);
        }
    }

    @Nullable
    private static RivetBlockEntity rivetBlockEntity(ServerSubLevel rivetSub) {
        EmbeddedPlotLevelAccessor embedded = rivetSub.getPlot().getEmbeddedLevelAccessor();
        BlockEntity blockEntity = embedded.getBlockEntity(BlockPos.ZERO);
        if (blockEntity instanceof RivetBlockEntity) {
            RivetBlockEntity be = (RivetBlockEntity)blockEntity;
            return be;
        }
        return null;
    }
}

