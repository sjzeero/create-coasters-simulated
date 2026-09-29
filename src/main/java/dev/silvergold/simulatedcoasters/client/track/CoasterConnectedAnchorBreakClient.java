package dev.silvergold.simulatedcoasters.client.track;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterConnectedAnchorBreakClient {
   private CoasterConnectedAnchorBreakClient() {
   }

   public static boolean requiresCreativePacedBreak(@Nullable Level level, BlockPos pos) {
      if (level == null) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.gameMode != null && mc.gameMode.getPlayerMode().isCreative()) {
            BlockState st = level.getBlockState(pos);
            if (!(st.getBlock() instanceof CoasterAnchorpointBlock)) {
               return false;
            } else {
               return level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be ? be.legCount() > 0 : false;
            }
         } else {
            return false;
         }
      }
   }
}
