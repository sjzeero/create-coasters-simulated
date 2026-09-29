/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackBlockEntity
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.AABB
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurvePickContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={BezierConnection.class})
public class BezierConnectionGetBoundsClientCoasterMixin {
    @Inject(method={"getBounds"}, at={@At(value="HEAD")})
    private void simulatedcoasters$noteTrackEntityDuringCurvePick(CallbackInfoReturnable<AABB> cir) {
        if (!CoasterTrackCurvePickContext.inPickCurves()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return;
        }
        BlockPos p = (BlockPos)((BezierConnection)this).bePositions.getFirst();
        BlockEntity be = mc.level.getBlockEntity(p);
        if (be instanceof TrackBlockEntity) {
            TrackBlockEntity tbe = (TrackBlockEntity)be;
            CoasterTrackCurvePickContext.setTrackEntity(tbe);
        }
    }
}

