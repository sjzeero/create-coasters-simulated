/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftDriveDirection {
    private static final double SPAN_DIR_SAMPLE_BLOCKS = 0.25;
    private static final double PARALLEL_EPS = 1.0E-4;

    private ChainLiftDriveDirection() {
    }

    public static Vec3 beltMotionAtShaftTop(BlockState anchorState, Direction.Axis shaftAxis, float signedRpm) {
        Vec3 axisUnit = ChainLiftDriveDirection.positiveAxisUnit(shaftAxis);
        Vec3 crownRadial = ChainLiftDriveDirection.crownRadial(axisUnit, anchorState);
        Vec3 tangent = crownRadial.cross(axisUnit);
        if (tangent.lengthSqr() < 1.0E-12) {
            return Vec3.ZERO;
        }
        double sign = signedRpm == 0.0f ? 1.0 : (double)Math.signum(signedRpm);
        return tangent.normalize().scale(sign);
    }

    @Nullable
    public static Vec3 spanTraversalDirection(Level level, CoasterPathGraphSpans.SpanPath path, double partialTickForRender) {
        double total = path.totalLength();
        if (total <= 1.0E-6) {
            return null;
        }
        Vec3 start = CoasterPathGraphSpans.samplePositionAtArcLength(level, path, 0.0, partialTickForRender);
        double sample = Math.min(0.25, total * 0.5);
        Vec3 ahead = CoasterPathGraphSpans.samplePositionAtArcLength(level, path, sample, partialTickForRender);
        Vec3 delta = ahead.subtract(start);
        if (delta.lengthSqr() < 1.0E-12) {
            return null;
        }
        return delta.normalize();
    }

    public static int computeTravelSign(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path, double partialTickForRender) {
        Level level = anchor.getLevel();
        Direction.Axis shaftAxis = anchor.getChainLiftShaftAxis();
        if (level == null || shaftAxis == null) {
            return 1;
        }
        Vec3 belt = ChainLiftDriveDirection.beltMotionAtShaftTop(anchor.getBlockState(), shaftAxis, 1.0f);
        Vec3 spanDir = ChainLiftDriveDirection.spanTraversalDirection(level, path, partialTickForRender);
        if (belt.lengthSqr() < 1.0E-12 || spanDir == null) {
            return 1;
        }
        return belt.dot(spanDir) >= 0.0 ? 1 : -1;
    }

    public static double signedChainBlocksPerSecond(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path, double partialTickForRender, float maxShaftRpm, double blocksPerSecondAtMaxRpm) {
        float rpm = anchor.getSpeed();
        double magnitude = blocksPerSecondAtMaxRpm * (double)Math.abs(rpm) / (double)maxShaftRpm;
        if (magnitude <= 1.0E-9) {
            return 0.0;
        }
        int travelSign = anchor.getChainLiftTravelSign();
        if (travelSign != 0) {
            return magnitude * (double)Math.signum(rpm) * (double)travelSign;
        }
        Level level = anchor.getLevel();
        Direction.Axis shaftAxis = anchor.getChainLiftShaftAxis();
        if (level == null || shaftAxis == null) {
            return magnitude * (double)Math.signum(rpm);
        }
        Vec3 belt = ChainLiftDriveDirection.beltMotionAtShaftTop(anchor.getBlockState(), shaftAxis, rpm);
        Vec3 spanDir = ChainLiftDriveDirection.spanTraversalDirection(level, path, partialTickForRender);
        if (belt.lengthSqr() < 1.0E-12 || spanDir == null) {
            return magnitude * (double)Math.signum(rpm);
        }
        double alignment = belt.dot(spanDir);
        if (Math.abs(alignment) < 1.0E-4) {
            return magnitude * (double)Math.signum(rpm);
        }
        return magnitude * Math.signum(alignment);
    }

    private static Vec3 positiveAxisUnit(Direction.Axis axis) {
        return switch (axis) {
            default -> throw new MatchException(null, null);
            case Direction.Axis.X -> new Vec3(1.0, 0.0, 0.0);
            case Direction.Axis.Y -> new Vec3(0.0, 1.0, 0.0);
            case Direction.Axis.Z -> new Vec3(0.0, 0.0, 1.0);
        };
    }

    private static Vec3 crownRadial(Vec3 axisUnit, BlockState anchorState) {
        Vec3 radial;
        Vec3 upRef = new Vec3(0.0, 1.0, 0.0);
        if (Math.abs(axisUnit.dot(upRef)) > 0.9999) {
            Direction facing = (Direction)anchorState.getValue((Property)CoasterAnchorpointBlock.FACING);
            upRef = Vec3.atLowerCornerOf((Vec3i)facing.getNormal());
        }
        if ((radial = upRef.subtract(axisUnit.scale(upRef.dot(axisUnit)))).lengthSqr() < 1.0E-12) {
            Direction facing = (Direction)anchorState.getValue((Property)CoasterAnchorpointBlock.FACING);
            Vec3 face = Vec3.atLowerCornerOf((Vec3i)facing.getNormal());
            radial = face.subtract(axisUnit.scale(face.dot(axisUnit)));
        }
        if (radial.lengthSqr() < 1.0E-12) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return radial.normalize();
    }
}

