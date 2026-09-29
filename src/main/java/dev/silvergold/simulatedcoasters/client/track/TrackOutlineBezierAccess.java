/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackBlockEntity
 *  com.simibubi.create.content.trains.track.TrackBlockOutline$BezierPointSelection
 *  net.minecraft.core.BlockPos
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class TrackOutlineBezierAccess {
    private TrackOutlineBezierAccess() {
    }

    @Nullable
    public static BezierConnection primaryBezierForSelection(TrackBlockOutline.BezierPointSelection sel) {
        TrackBlockEntity tbe = sel.blockEntity();
        BlockPos key = sel.loc().curveTarget();
        for (BezierConnection c : tbe.getConnections().values()) {
            if (!c.isPrimary() || !c.getKey().equals((Object)key)) continue;
            return c;
        }
        return null;
    }
}

