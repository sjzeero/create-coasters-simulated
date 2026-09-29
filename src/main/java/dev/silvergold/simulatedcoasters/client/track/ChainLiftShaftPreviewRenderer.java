package dev.silvergold.simulatedcoasters.client.track;

import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftShaftAxis;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ChainLiftShaftPreviewRenderer {
   private static final int OUTLINE_COLOR = 16777215;
   private static final double SHAFT_HALF_LENGTH_BLOCKS = 0.5625;
   private static final double SHAFT_HALF_CROSS_BLOCKS = 0.125;

   private ChainLiftShaftPreviewRenderer() {
   }

   public static Vec3 resolveShaftAxis(Minecraft mc, BlockPos anchor, float partialTick) {
      if (mc.level != null && mc.player != null) {
         Vec3 eye = mc.player.getEyePosition(partialTick);
         if (CoasterPathGraphManager.anchorBlockIsOnSubLevel(mc.level, anchor)) {
            eye = CoasterAnchorClientSpace.toPlotLocal(mc.level, anchor, eye, partialTick);
         }

         return ChainLiftShaftAxis.resolveFromEye(mc.level, anchor, eye);
      } else {
         return new Vec3(1.0, 0.0, 0.0);
      }
   }

   public static void show(Outliner outliner, String key, BlockPos anchor, Vec3 shaftAxis) {
      Vec3 center = Vec3.atCenterOf(anchor);
      CoasterPlotOutliner.showShaftEndCaps(outliner, key, center, shaftAxis, 0.5625, 0.125, 16777215);
   }

   public static void clear(Outliner outliner, String key) {
      CoasterPlotOutliner.clearShaftEndCaps(outliner, key);
   }
}
