package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
      List<ItemStack> drops = Block.getDrops(state, level, rivetCell, blockEntity, player, tool);
      if (!drops.isEmpty()) {
         Vec3 dropPos = dropPosition(level, rivetCell, blockEntity);
         RandomSource random = level.getRandom();

         for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
               double vx = random.nextGaussian() * 0.05;
               double vy = random.nextGaussian() * 0.05 + 0.2;
               double vz = random.nextGaussian() * 0.05;
               ItemEntity entity = new ItemEntity(level, dropPos.x, dropPos.y, dropPos.z, stack);
               entity.setDeltaMovement(vx, vy, vz);
               level.addFreshEntity(entity);
            }
         }
      }
   }

   private static Vec3 dropPosition(ServerLevel level, BlockPos rivetCell, @Nullable BlockEntity blockEntity) {
      if (blockEntity instanceof RivetBlockEntity be) {
         RivetPlacement.Resolved resolved = RivetPoseAlignment.resolvedFrom(be);
         Vec3 outward = RivetPlacement.hostFaceOutwardWorld(level, resolved);
         Vec3 faceAnchor = RivetPlacement.faceAnchorWorld(level, resolved);
         double offset = 0.1875;
         return faceAnchor.add(outward.scale(offset));
      } else {
         if (Sable.HELPER.getContaining(level, rivetCell) instanceof ServerSubLevel ssl && !ssl.isRemoved()) {
            Vec3 cellCenter = Vec3.atCenterOf(ssl.getPlot().getCenterBlock());
            Vector3d outward = new Vector3d(0.0, 1.0, 0.0);
            ssl.logicalPose().orientation().transform(outward);
            double offset = 0.625;
            return cellCenter.add(outward.x * offset, outward.y * offset, outward.z * offset);
         }

         return Vec3.atCenterOf(rivetCell);
      }
   }
}
