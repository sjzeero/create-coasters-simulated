/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import dev.silvergold.simulatedcoasters.rivet.RivetPoseAlignment;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class RivetDrop {
    private static final double OUTWARD_MARGIN_BLOCKS = 0.125;

    private RivetDrop() {
    }

    public static void spawn(ServerLevel level, BlockPos rivetCell, BlockState state, @Nullable BlockEntity blockEntity, @Nullable Player player, ItemStack tool) {
        List drops = Block.getDrops((BlockState)state, (ServerLevel)level, (BlockPos)rivetCell, (BlockEntity)blockEntity, (Entity)player, (ItemStack)tool);
        if (drops.isEmpty()) {
            return;
        }
        Vec3 dropPos = RivetDrop.dropPosition(level, rivetCell, blockEntity);
        RandomSource random = level.getRandom();
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            double vx = random.nextGaussian() * 0.05;
            double vy = random.nextGaussian() * 0.05 + 0.2;
            double vz = random.nextGaussian() * 0.05;
            ItemEntity entity = new ItemEntity((Level)level, dropPos.x, dropPos.y, dropPos.z, stack);
            entity.setDeltaMovement(vx, vy, vz);
            level.addFreshEntity((Entity)entity);
        }
    }

    private static Vec3 dropPosition(ServerLevel level, BlockPos rivetCell, @Nullable BlockEntity blockEntity) {
        ServerSubLevel ssl;
        if (blockEntity instanceof RivetBlockEntity) {
            RivetBlockEntity be = (RivetBlockEntity)blockEntity;
            RivetPlacement.Resolved resolved = RivetPoseAlignment.resolvedFrom(be);
            Vec3 outward = RivetPlacement.hostFaceOutwardWorld((Level)level, resolved);
            Vec3 faceAnchor = RivetPlacement.faceAnchorWorld((Level)level, resolved);
            double offset = 0.1875;
            return faceAnchor.add(outward.scale(offset));
        }
        SubLevel sub = Sable.HELPER.getContaining((Level)level, (Vec3i)rivetCell);
        if (sub instanceof ServerSubLevel && !(ssl = (ServerSubLevel)sub).isRemoved()) {
            Vec3 cellCenter = Vec3.atCenterOf((Vec3i)ssl.getPlot().getCenterBlock());
            Vector3d outward = new Vector3d(0.0, 1.0, 0.0);
            ssl.logicalPose().orientation().transform(outward);
            double offset = 0.625;
            return cellCenter.add(outward.x * offset, outward.y * offset, outward.z * offset);
        }
        return Vec3.atCenterOf((Vec3i)rivetCell);
    }
}

