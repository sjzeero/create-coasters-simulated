/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$Segment
 *  net.createmod.catnip.data.Iterate
 *  net.createmod.ponder.api.level.PonderLevel
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.particles.BlockParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterCurveTintColors;
import net.createmod.catnip.data.Iterate;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
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
        CoasterCurveUndyeParticles.spawnAlongCurveInternal((Level)level, bc, diffuseRgbPacked);
    }

    public static void spawnAlongCurve(PonderLevel level, BezierConnection bc, int diffuseRgbPacked) {
        CoasterCurveUndyeParticles.spawnAlongCurveInternal((Level)level, bc, diffuseRgbPacked);
    }

    private static void spawnAlongCurveInternal(Level level, BezierConnection bc, int diffuseRgbPacked) {
        int rgb = CoasterCurveTintColors.diffuseRgbWithVisualMuting(diffuseRgbPacked) & 0xFFFFFF;
        if (rgb == 0) {
            return;
        }
        BlockState concrete = CoasterCurveUndyeParticles.concreteStateNearestToDiffuse(rgb);
        Vec3 base = Vec3.atLowerCornerOf((Vec3i)((Vec3i)bc.bePositions.getFirst()));
        RandomSource random = level.getRandom();
        for (BezierConnection.Segment segment : bc) {
            for (int sign : Iterate.positiveAndNegative) {
                Vec3 pos = segment.position.add(segment.normal.scale((double)(0.875f * (float)sign))).add(base);
                if (level instanceof PonderLevel) {
                    PonderLevel ponder = (PonderLevel)level;
                    CoasterCurveUndyeParticles.spawnPonderBlockDebris(ponder, concrete, pos, random);
                    continue;
                }
                if (!(level instanceof ServerLevel)) continue;
                ServerLevel sl = (ServerLevel)level;
                sl.sendParticles((ParticleOptions)new BlockParticleOption(ParticleTypes.BLOCK, concrete), pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static void spawnPonderBlockDebris(PonderLevel level, BlockState state, Vec3 pos, RandomSource random) {
        double vx = (random.nextDouble() - 0.5) * 0.12;
        double vy = random.nextDouble() * 0.08 + 0.02;
        double vz = (random.nextDouble() - 0.5) * 0.12;
        level.addParticle((ParticleOptions)new BlockParticleOption(ParticleTypes.BLOCK, state), pos.x, pos.y, pos.z, vx, vy, vz);
    }

    private static BlockState concreteStateNearestToDiffuse(int rgb) {
        return CoasterCurveUndyeParticles.concreteBlockFor(CoasterCurveUndyeParticles.nearestDyeColorByRgb(rgb)).defaultBlockState();
    }

    private static DyeColor nearestDyeColorByRgb(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        DyeColor best = DyeColor.WHITE;
        int bestScore = Integer.MAX_VALUE;
        for (DyeColor d : DyeColor.values()) {
            int db;
            int dg;
            int diffuse = d.getTextureDiffuseColor() & 0xFFFFFF;
            int dr = (diffuse >> 16 & 0xFF) - r;
            int score = dr * dr + (dg = (diffuse >> 8 & 0xFF) - g) * dg + (db = (diffuse & 0xFF) - b) * db;
            if (score >= bestScore) continue;
            bestScore = score;
            best = d;
        }
        return best;
    }

    private static Block concreteBlockFor(DyeColor color) {
        return switch (color) {
            default -> throw new MatchException(null, null);
            case DyeColor.WHITE -> Blocks.WHITE_CONCRETE;
            case DyeColor.ORANGE -> Blocks.ORANGE_CONCRETE;
            case DyeColor.MAGENTA -> Blocks.MAGENTA_CONCRETE;
            case DyeColor.LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE;
            case DyeColor.YELLOW -> Blocks.YELLOW_CONCRETE;
            case DyeColor.LIME -> Blocks.LIME_CONCRETE;
            case DyeColor.PINK -> Blocks.PINK_CONCRETE;
            case DyeColor.GRAY -> Blocks.GRAY_CONCRETE;
            case DyeColor.LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE;
            case DyeColor.CYAN -> Blocks.CYAN_CONCRETE;
            case DyeColor.PURPLE -> Blocks.PURPLE_CONCRETE;
            case DyeColor.BLUE -> Blocks.BLUE_CONCRETE;
            case DyeColor.BROWN -> Blocks.BROWN_CONCRETE;
            case DyeColor.GREEN -> Blocks.GREEN_CONCRETE;
            case DyeColor.RED -> Blocks.RED_CONCRETE;
            case DyeColor.BLACK -> Blocks.BLACK_CONCRETE;
        };
    }
}

