/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Transformation
 *  net.createmod.catnip.render.CachedBuffers
 *  net.createmod.catnip.render.SuperByteBuffer
 *  net.createmod.catnip.render.SuperByteBufferCache
 *  net.createmod.catnip.render.SuperByteBufferCache$Compartment
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.resources.model.BlockModelRotation
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.joml.Vector3f
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterAnchorpointShaftModel {
    public static final SuperByteBufferCache.Compartment<BlockState> SHAFT_BODY = new SuperByteBufferCache.Compartment();

    private CoasterAnchorpointShaftModel() {
    }

    public static void registerClientInit() {
        SuperByteBufferCache.getInstance().registerCompartment(SHAFT_BODY);
    }

    public static void renderBody(PoseStack ms, MultiBufferSource buffer, BlockState anchorState, Direction facing, Direction.Axis shaftAxis, int light) {
        SuperByteBuffer model = CachedBuffers.block(SHAFT_BODY, (BlockState)anchorState);
        CoasterAnchorpointShaftModel.applyShaftAxisAlignment(model, facing, shaftAxis);
        model.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
    }

    private static void applyShaftAxisAlignment(SuperByteBuffer buf, Direction facing, Direction.Axis shaftAxis) {
        Vec3 modelShaftAxis = CoasterAnchorpointShaftModel.modelShaftAxisAfterFacing(facing);
        Vec3 shaftDir = CoasterAnchorpointShaftModel.axisUnit(shaftAxis);
        Vec3 faceNormal = Vec3.atLowerCornerOf((Vec3i)facing.getNormal());
        float degrees = (float)Math.toDegrees(CoasterAnchorpointShaftModel.signedAngleRadians(faceNormal, modelShaftAxis, shaftDir));
        if (Math.abs(degrees) < 0.001f) {
            return;
        }
        CoasterAnchorpointShaftModel.rotateCenteredDegrees(buf, degrees, facing);
    }

    private static Vec3 modelShaftAxisAfterFacing(Direction facing) {
        int[] rotations = CoasterAnchorpointShaftModel.blockstateRotations(facing);
        BlockModelRotation rotation = BlockModelRotation.by((int)rotations[0], (int)rotations[1]);
        Vector3f bore = new Vector3f(0.0f, 0.0f, 1.0f);
        Transformation transform = rotation.getRotation();
        transform.transformNormal(bore);
        return new Vec3((double)bore.x(), (double)bore.y(), (double)bore.z());
    }

    private static int[] blockstateRotations(Direction facing) {
        int[] nArray;
        switch (facing) {
            default: {
                throw new MatchException(null, null);
            }
            case DOWN: {
                int[] nArray2 = new int[2];
                nArray2[0] = 180;
                nArray = nArray2;
                nArray2[1] = 0;
                break;
            }
            case NORTH: {
                int[] nArray3 = new int[2];
                nArray3[0] = 90;
                nArray = nArray3;
                nArray3[1] = 0;
                break;
            }
            case SOUTH: {
                int[] nArray4 = new int[2];
                nArray4[0] = 270;
                nArray = nArray4;
                nArray4[1] = 0;
                break;
            }
            case WEST: {
                int[] nArray5 = new int[2];
                nArray5[0] = 90;
                nArray = nArray5;
                nArray5[1] = 270;
                break;
            }
            case EAST: {
                int[] nArray6 = new int[2];
                nArray6[0] = 90;
                nArray = nArray6;
                nArray6[1] = 90;
                break;
            }
            case UP: {
                int[] nArray7 = new int[2];
                nArray7[0] = 0;
                nArray = nArray7;
                nArray7[1] = 0;
            }
        }
        return nArray;
    }

    private static void rotateCenteredDegrees(SuperByteBuffer buf, float degrees, Direction axis) {
        if (Math.abs(degrees) < 0.001f) {
            return;
        }
        buf.rotateCentered((float)Math.toRadians(degrees), axis);
    }

    private static Vec3 axisUnit(Direction.Axis axis) {
        return switch (axis) {
            default -> throw new MatchException(null, null);
            case Direction.Axis.X -> new Vec3(1.0, 0.0, 0.0);
            case Direction.Axis.Y -> new Vec3(0.0, 1.0, 0.0);
            case Direction.Axis.Z -> new Vec3(0.0, 0.0, 1.0);
        };
    }

    private static double signedAngleRadians(Vec3 axis, Vec3 from, Vec3 to) {
        Vec3 a = CoasterAnchorpointShaftModel.projectOntoPlane(from, axis);
        Vec3 b = CoasterAnchorpointShaftModel.projectOntoPlane(to, axis);
        if (a.lengthSqr() < 1.0E-12 || b.lengthSqr() < 1.0E-12) {
            return 0.0;
        }
        a = a.normalize();
        b = b.normalize();
        Vec3 n = axis.normalize();
        double sin = n.dot(a.cross(b));
        double cos = a.dot(b);
        return Math.atan2(sin, cos);
    }

    private static Vec3 projectOntoPlane(Vec3 v, Vec3 planeNormal) {
        Vec3 n = planeNormal.normalize();
        return v.subtract(n.scale(v.dot(n)));
    }
}

