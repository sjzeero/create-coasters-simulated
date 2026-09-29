/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.rivet;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.rivet.RivetSubLevelLightSample;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class RivetSubLevelRenderLight {
    private static final ThreadLocal<BlockPos> PROBE = new ThreadLocal();

    private RivetSubLevelRenderLight() {
    }

    public static void begin(ClientSubLevel sub, BlockState state) {
        RivetBlockEntity rivetBe;
        if (!state.is((Block)SimulatedCoastersBlocks.RIVET.get())) {
            return;
        }
        ClientLevel clientLevel = sub.getLevel();
        if (!(clientLevel instanceof ClientLevel)) {
            return;
        }
        ClientLevel level = clientLevel;
        BlockEntity blockEntity = sub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
        RivetBlockEntity be = blockEntity instanceof RivetBlockEntity ? (rivetBe = (RivetBlockEntity)blockEntity) : null;
        PROBE.set(RivetSubLevelLightSample.probeCell(level, sub, state, be));
    }

    public static void end() {
        PROBE.remove();
    }

    @Nullable
    public static BlockPos probePos() {
        return PROBE.get();
    }
}

