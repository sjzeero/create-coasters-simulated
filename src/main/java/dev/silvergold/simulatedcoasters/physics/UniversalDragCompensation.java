/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.PhysicsPipeline
 *  dev.ryanhcode.sable.api.physics.PhysicsPipelineBody
 *  dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.server.level.ServerLevel
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.physics;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class UniversalDragCompensation {
    private UniversalDragCompensation() {
    }

    public static void applyPartial(ServerLevel level, SubLevelPhysicsSystem physics, ServerSubLevel sub, double dt, double fraction) {
        if (dt <= 0.0 || fraction <= 0.0) {
            return;
        }
        double c = DimensionPhysicsData.getUniversalDrag((ServerLevel)level);
        if (c <= 0.0) {
            return;
        }
        double factor = fraction * c * dt;
        if (factor <= 0.0) {
            return;
        }
        PhysicsPipeline pipeline = physics.getPipeline();
        Vector3d v = pipeline.getLinearVelocity((PhysicsPipelineBody)sub, new Vector3d());
        Vector3d w = pipeline.getAngularVelocity((PhysicsPipelineBody)sub, new Vector3d());
        Vector3d dv = new Vector3d((Vector3dc)v).mul(factor);
        Vector3d dw = new Vector3d((Vector3dc)w).mul(factor);
        if (dv.lengthSquared() < 1.0E-22 && dw.lengthSquared() < 1.0E-22) {
            return;
        }
        pipeline.addLinearAndAngularVelocity((PhysicsPipelineBody)sub, (Vector3dc)dv, (Vector3dc)dw);
    }
}

