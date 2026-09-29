/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.rivet;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class RivetSubLevelLightSample {
    private RivetSubLevelLightSample() {
    }

    public static BlockPos probeCell(ClientLevel level, ClientSubLevel rivetSub, BlockState rivetState, @Nullable RivetBlockEntity be) {
        if (be != null) {
            Pose3dc hostPose = RivetSubLevelLightSample.hostRenderPose(level, be);
            Vec3 airPlot = Vec3.atCenterOf((Vec3i)be.hostPos().relative(be.face()));
            Vec3 world = hostPose == null ? airPlot : hostPose.transformPosition(airPlot);
            return BlockPos.containing((Position)world);
        }
        Pose3dc rivetPose = rivetSub.renderPose();
        Vec3 anchorPlot = RivetPlacement.rivetOutwardAnchorPlot(BlockPos.ZERO);
        double plateOut = 0.14250000000000002;
        Vec3 world = rivetPose.transformPosition(new Vec3(anchorPlot.x, anchorPlot.y + plateOut, anchorPlot.z));
        return BlockPos.containing((Position)world);
    }

    @Nullable
    private static Pose3dc hostRenderPose(ClientLevel level, RivetBlockEntity be) {
        UUID hostSubLevelId = be.hostKey().hostSubLevelId();
        if (hostSubLevelId == null) {
            return null;
        }
        ClientSubLevelContainer raw = SubLevelContainer.getContainer((ClientLevel)level);
        if (raw == null) {
            return null;
        }
        SubLevel hostSub = raw.getSubLevel(hostSubLevelId);
        if (hostSub instanceof ClientSubLevel) {
            ClientSubLevel clientHost = (ClientSubLevel)hostSub;
            return clientHost.renderPose();
        }
        if (hostSub != null) {
            return hostSub.logicalPose();
        }
        return null;
    }
}

