/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.engine_room.flywheel.api.instance.InstanceType
 *  dev.engine_room.flywheel.api.layout.FloatRepr
 *  dev.engine_room.flywheel.api.layout.IntegerRepr
 *  dev.engine_room.flywheel.api.layout.LayoutBuilder
 *  dev.engine_room.flywheel.api.layout.ValueRepr
 *  dev.engine_room.flywheel.lib.instance.SimpleInstanceType
 *  dev.engine_room.flywheel.lib.util.ExtraMemoryOps
 *  net.minecraft.resources.ResourceLocation
 *  org.joml.Vector3fc
 *  org.lwjgl.system.MemoryUtil
 */
package dev.silvergold.simulatedcoasters.client.track.bent;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.api.layout.ValueRepr;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import dev.silvergold.simulatedcoasters.client.track.bent.BentRailInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;

public final class BentRailInstanceType {
    public static final ResourceLocation VERTEX_SHADER = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"instance/bent_rail.vert");
    public static final ResourceLocation CULL_SHADER = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"instance/cull/bent_rail.glsl");
    public static final InstanceType<BentRailInstance> INSTANCE = SimpleInstanceType.builder(BentRailInstance::new).layout(LayoutBuilder.create().vector("color", (ValueRepr)FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4).vector("overlay", (ValueRepr)IntegerRepr.SHORT, 2).vector("light", (ValueRepr)FloatRepr.UNSIGNED_SHORT, 2).vector("prevSpine", (ValueRepr)FloatRepr.FLOAT, 3).vector("currSpine", (ValueRepr)FloatRepr.FLOAT, 3).vector("prevTangent", (ValueRepr)FloatRepr.FLOAT, 3).vector("currTangent", (ValueRepr)FloatRepr.FLOAT, 3).vector("prevLateral", (ValueRepr)FloatRepr.FLOAT, 3).vector("currLateral", (ValueRepr)FloatRepr.FLOAT, 3).scalar("modelAnchorY", (ValueRepr)FloatRepr.FLOAT).build()).writer((ptr, instance) -> {
        MemoryUtil.memPutByte((long)ptr, (byte)instance.red);
        MemoryUtil.memPutByte((long)(ptr + 1L), (byte)instance.green);
        MemoryUtil.memPutByte((long)(ptr + 2L), (byte)instance.blue);
        MemoryUtil.memPutByte((long)(ptr + 3L), (byte)instance.alpha);
        ExtraMemoryOps.put2x16((long)(ptr + 4L), (int)instance.overlay);
        ExtraMemoryOps.put2x16((long)(ptr + 8L), (int)instance.light);
        ExtraMemoryOps.putVector3f((long)(ptr + 12L), (Vector3fc)instance.prevSpine);
        ExtraMemoryOps.putVector3f((long)(ptr + 24L), (Vector3fc)instance.currSpine);
        ExtraMemoryOps.putVector3f((long)(ptr + 36L), (Vector3fc)instance.prevTangent);
        ExtraMemoryOps.putVector3f((long)(ptr + 48L), (Vector3fc)instance.currTangent);
        ExtraMemoryOps.putVector3f((long)(ptr + 60L), (Vector3fc)instance.prevLateral);
        ExtraMemoryOps.putVector3f((long)(ptr + 72L), (Vector3fc)instance.currLateral);
        MemoryUtil.memPutFloat((long)(ptr + 84L), (float)instance.modelAnchorY);
    }).vertexShader(VERTEX_SHADER).cullShader(CULL_SHADER).build();

    private BentRailInstanceType() {
    }
}

