/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.contraptions.StructureTransform
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import com.simibubi.create.content.contraptions.StructureTransform;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public final class ChainLiftShaftAxis {
    private static final double AXIS_MATCH_EPS = 0.25;

    private ChainLiftShaftAxis() {
    }

    public static Vec3 resolveFromEye(Level level, BlockPos anchor, Vec3 eyePosition) {
        double alongSecond;
        Vec3 axis;
        BlockState state = level.getBlockState(anchor);
        Direction facing = state.hasProperty((Property)CoasterAnchorpointBlock.FACING) ? (Direction)state.getValue((Property)CoasterAnchorpointBlock.FACING) : Direction.UP;
        Vec3 faceNormal = Vec3.atLowerCornerOf((Vec3i)facing.getNormal()).normalize();
        Vec3[] inPlane = ChainLiftShaftAxis.inPlaneAxes(facing);
        Vec3 center = Vec3.atCenterOf((Vec3i)anchor);
        Vec3 toViewer = eyePosition.subtract(center);
        Vec3 planar = toViewer.subtract(faceNormal.scale(toViewer.dot(faceNormal)));
        if (planar.lengthSqr() < 1.0E-8) {
            return inPlane[0];
        }
        Vec3 planarUnit = planar.normalize();
        double alongFirst = Math.abs(planarUnit.dot(inPlane[0]));
        Vec3 vec3 = axis = alongFirst >= (alongSecond = Math.abs(planarUnit.dot(inPlane[1]))) ? inPlane[0] : inPlane[1];
        if (planarUnit.dot(axis) < 0.0) {
            axis = axis.scale(-1.0);
        }
        return axis;
    }

    public static Direction.Axis toDominantAxis(Vec3 axis) {
        Vec3 n = axis.normalize();
        double ax = Math.abs(n.x);
        double ay = Math.abs(n.y);
        double az = Math.abs(n.z);
        if (ax >= ay && ax >= az) {
            return Direction.Axis.X;
        }
        if (ay >= az) {
            return Direction.Axis.Y;
        }
        return Direction.Axis.Z;
    }

    public static Direction.Axis rotateStoredAxis(StructureTransform transform, Direction.Axis axis) {
        return ChainLiftShaftAxis.toDominantAxis(transform.applyWithoutOffsetUncentered(ChainLiftShaftAxis.unitVector(axis)));
    }

    private static Vec3 unitVector(Direction.Axis axis) {
        return switch (axis) {
            default -> throw new MatchException(null, null);
            case Direction.Axis.X -> new Vec3(1.0, 0.0, 0.0);
            case Direction.Axis.Y -> new Vec3(0.0, 1.0, 0.0);
            case Direction.Axis.Z -> new Vec3(0.0, 0.0, 1.0);
        };
    }

    public static boolean matchesSentAxis(Vec3 serverAxis, Vec3 clientAxis) {
        Vec3 b;
        if (clientAxis.lengthSqr() < 1.0E-12) {
            return false;
        }
        Vec3 a = serverAxis.normalize();
        return Math.abs(a.dot(b = clientAxis.normalize())) >= 0.75;
    }

    public static boolean isValidForFacing(Vec3 axis, Direction facing) {
        if (axis.lengthSqr() < 1.0E-12) {
            return false;
        }
        Vec3 normal = Vec3.atLowerCornerOf((Vec3i)facing.getNormal()).normalize();
        return Math.abs(axis.normalize().dot(normal)) < 0.01;
    }

    public static Direction.Axis alternateAxis(Direction.Axis current, Direction facing) {
        Vec3[] inPlane = ChainLiftShaftAxis.inPlaneAxes(facing);
        Direction.Axis first = ChainLiftShaftAxis.toDominantAxis(inPlane[0]);
        Direction.Axis second = ChainLiftShaftAxis.toDominantAxis(inPlane[1]);
        if (current == first) {
            return second;
        }
        if (current == second) {
            return first;
        }
        return second;
    }

    private static Vec3[] inPlaneAxes(Direction facing) {
        Vec3[] vec3Array;
        switch (facing.getAxis()) {
            default: {
                throw new MatchException(null, null);
            }
            case Y: {
                Vec3[] vec3Array2 = new Vec3[2];
                vec3Array2[0] = new Vec3(1.0, 0.0, 0.0);
                vec3Array = vec3Array2;
                vec3Array2[1] = new Vec3(0.0, 0.0, 1.0);
                break;
            }
            case X: {
                Vec3[] vec3Array3 = new Vec3[2];
                vec3Array3[0] = new Vec3(0.0, 1.0, 0.0);
                vec3Array = vec3Array3;
                vec3Array3[1] = new Vec3(0.0, 0.0, 1.0);
                break;
            }
            case Z: {
                Vec3[] vec3Array4 = new Vec3[2];
                vec3Array4[0] = new Vec3(1.0, 0.0, 0.0);
                vec3Array = vec3Array4;
                vec3Array4[1] = new Vec3(0.0, 1.0, 0.0);
            }
        }
        return vec3Array;
    }
}

