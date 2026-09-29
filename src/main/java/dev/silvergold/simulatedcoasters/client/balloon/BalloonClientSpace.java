/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 */
package dev.silvergold.simulatedcoasters.client.balloon;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

@OnlyIn(value=Dist.CLIENT)
public final class BalloonClientSpace {
    private BalloonClientSpace() {
    }

    @Nullable
    public static SubLevel resolveHostSubLevel(Level level, BalloonBlockEntity be) {
        SubLevelContainer container = SubLevelContainer.getContainer((Level)level);
        if (container == null) {
            return null;
        }
        SubLevel byId = container.getSubLevel(be.hostSubLevelId());
        if (byId != null && !byId.isRemoved()) {
            return byId;
        }
        return null;
    }

    public static Vec3 plotPointRenderWorld(SubLevel sub, Vector3d plotPoint, float partialTick) {
        return BalloonClientSpace.plotPointRenderWorld(sub, new Vec3(plotPoint.x, plotPoint.y, plotPoint.z), partialTick);
    }

    public static Vec3 plotPointRenderWorld(SubLevel sub, Vec3 plotPoint, float partialTick) {
        Pose3dc pose = BalloonClientSpace.renderPose(sub, partialTick);
        return pose == null ? plotPoint : pose.transformPosition(plotPoint);
    }

    private static Pose3dc renderPose(SubLevel sub, float partialTick) {
        if (sub instanceof ClientSubLevel) {
            ClientSubLevel clientSub = (ClientSubLevel)sub;
            return clientSub.renderPose(partialTick);
        }
        return sub.logicalPose();
    }
}

