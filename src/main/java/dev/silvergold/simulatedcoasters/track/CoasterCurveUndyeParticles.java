package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.BezierConnection.Segment;
import net.createmod.catnip.data.Iterate;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CoasterCurveUndyeParticles {
   private CoasterCurveUndyeParticles() {
   }

   public static void spawnAlongCurve(ServerLevel level, BezierConnection bc, int diffuseRgbPacked) {
      spawnAlongCurveInternal(level, bc, diffuseRgbPacked);
   }

   public static void spawnAlongCurve(PonderLevel level, BezierConnection bc, int diffuseRgbPacked) {
      spawnAlongCurveInternal(level, bc, diffuseRgbPacked);
   }

   private static void spawnAlongCurveInternal(Level level, BezierConnection bc, int diffuseRgbPacked) {
      int rgb = CoasterCurveTintColors.diffuseRgbWithVisualMuting(diffuseRgbPacked) & 16777215;
      if (rgb != 0) {
         BlockState concrete = concreteStateNearestToDiffuse(rgb);
         Vec3 base = Vec3.atLowerCornerOf((Vec3i)bc.bePositions.getFirst());
         RandomSource random = level.getRandom();

         for (Segment segment : bc) {
            for (int sign : Iterate.positiveAndNegative) {
               Vec3 pos = segment.position.add(segment.normal.scale((double)(0.875F * (float)sign))).add(base);
               if (level instanceof PonderLevel ponder) {
                  spawnPonderBlockDebris(ponder, concrete, pos, random);
               } else if (level instanceof ServerLevel sl) {
                  sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, concrete), pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
               }
            }
         }
      }
   }

   private static void spawnPonderBlockDebris(PonderLevel level, BlockState state, Vec3 pos, RandomSource random) {
      double vx = (random.nextDouble() - 0.5) * 0.12;
      double vy = random.nextDouble() * 0.08 + 0.02;
      double vz = (random.nextDouble() - 0.5) * 0.12;
      level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.x, pos.y, pos.z, vx, vy, vz);
   }

   private static BlockState concreteStateNearestToDiffuse(int rgb) {
      return concreteBlockFor(nearestDyeColorByRgb(rgb)).defaultBlockState();
   }

   private static DyeColor nearestDyeColorByRgb(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      DyeColor best = DyeColor.WHITE;
      int bestScore = Integer.MAX_VALUE;

      for (DyeColor d : DyeColor.values()) {
         int diffuse = d.getTextureDiffuseColor() & 16777215;
         int dr = (diffuse >> 16 & 0xFF) - r;
         int dg = (diffuse >> 8 & 0xFF) - g;
         int db = (diffuse & 0xFF) - b;
         int score = dr * dr + dg * dg + db * db;
         if (score < bestScore) {
            bestScore = score;
            best = d;
         }
      }

      return best;
   }

   private static Block concreteBlockFor(DyeColor color) {
      return switch (color) {
         case WHITE -> Blocks.WHITE_CONCRETE;
         case ORANGE -> Blocks.ORANGE_CONCRETE;
         case MAGENTA -> Blocks.MAGENTA_CONCRETE;
         case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE;
         case YELLOW -> Blocks.YELLOW_CONCRETE;
         case LIME -> Blocks.LIME_CONCRETE;
         case PINK -> Blocks.PINK_CONCRETE;
         case GRAY -> Blocks.GRAY_CONCRETE;
         case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE;
         case CYAN -> Blocks.CYAN_CONCRETE;
         case PURPLE -> Blocks.PURPLE_CONCRETE;
         case BLUE -> Blocks.BLUE_CONCRETE;
         case BROWN -> Blocks.BROWN_CONCRETE;
         case GREEN -> Blocks.GREEN_CONCRETE;
         case RED -> Blocks.RED_CONCRETE;
         case BLACK -> Blocks.BLACK_CONCRETE;
         default -> throw new MatchException(null, null);
      };
   }
}
