package dev.silvergold.simulatedcoasters.track.graph;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartRailSnapEligibility;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartDebugRenderer {
   private static final String OUTLINER_BASE = "coaster_cart_dbg";
   private static final int LINE_COLOR = 16605168;
   private static final int FORWARD_COLOR = 15158332;
   private static final int RIGHT_COLOR = 3066993;
   private static final int UP_COLOR = 3447003;
   private static final double AXIS_LENGTH = 1.6;

   private CoasterCartDebugRenderer() {
   }

   public static void clear() {
      Outliner outliner = Outliner.getInstance();

      for (int i = 0; i <= 3; i++) {
         outliner.remove(Pair.of("coaster_cart_dbg", i));
      }
   }

   public static void render(Level level, CoasterPathGraph graph) {
      Minecraft mc = Minecraft.getInstance();
      Entity cameraEntity = mc.cameraEntity;
      if (cameraEntity != null && !graph.edges().isEmpty()) {
         SubLevel cart = resolveCrosshairCart(mc, level);
         if (cart == null || !renderCrosshairCartBearings(level, graph, cart)) {
            clear();
         }
      } else {
         clear();
      }
   }

   private static boolean renderCrosshairCartBearings(Level level, CoasterPathGraph graph, SubLevel cart) {
      Block bearingBlock = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
      LevelPlot plot = cart.getPlot();
      boolean[] drew = new boolean[]{false};
      CoasterCartPlotScan.forEachBearingWorldCell(plot, bearingBlock, bearingPlot -> {
         Vec3 bearingWorld = cart.logicalPose().transformPosition(Vec3.atCenterOf(bearingPlot));
         CoasterPathTrackFrame.GraphHit hit = CoasterPathTrackFrame.nearestGraphHit(level, graph, bearingWorld, 6.25);
         hit = CoasterCartRailSnapEligibility.filterHitForBearing(cart, level, hit);
         if (hit != null) {
            drew[0] = true;
            Outliner.getInstance().showLine(Pair.of("coaster_cart_dbg", 0), bearingWorld, hit.point()).colored(16605168).lineWidth(0.0625F);
            CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit);
            drawAxis(bearingWorld, basis.forward(), 15158332, 1);
            drawAxis(bearingWorld, basis.right(), 3066993, 2);
            drawAxis(bearingWorld, basis.up(), 3447003, 3);
         }
      });
      return drew[0];
   }

   @Nullable
   private static SubLevel resolveCrosshairCart(Minecraft mc, Level level) {
      HitResult hit = mc.hitResult;
      if (hit instanceof BlockHitResult bhr && hit.getType() == Type.BLOCK) {
         SubLevel cart = CoasterCartChainLinkInteraction.resolveHoveredCart(level, hit);
         if (cart != null) {
            return cart;
         }

         SubLevel sub = CoasterCartResolve.findSubLevelAt(level, bhr.getBlockPos());
         if (sub != null && !sub.isRemoved() && CoasterCartResolve.isCartSubLevel(sub)) {
            return sub;
         }
      }

      return null;
   }

   private static void drawAxis(Vec3 origin, Vec3 dirUnit, int color, int subKey) {
      if (!(dirUnit.lengthSqr() < 1.0E-12)) {
         Vec3 end = origin.add(dirUnit.normalize().scale(1.6));
         Outliner.getInstance().showLine(Pair.of("coaster_cart_dbg", subKey), origin, end).colored(color).lineWidth(0.05F);
      }
   }
}
