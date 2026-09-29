/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorClientSpace;
import dev.silvergold.simulatedcoasters.client.track.CoasterPlotOutliner;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftShaftAxis;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class ChainLiftShaftPreviewRenderer {
    private static final int OUTLINE_COLOR = 0xFFFFFF;
    private static final double SHAFT_HALF_LENGTH_BLOCKS = 0.5625;
    private static final double SHAFT_HALF_CROSS_BLOCKS = 0.125;

    private ChainLiftShaftPreviewRenderer() {
    }

    public static Vec3 resolveShaftAxis(Minecraft mc, BlockPos anchor, float partialTick) {
        if (mc.level == null || mc.player == null) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        Vec3 eye = mc.player.getEyePosition(partialTick);
        if (CoasterPathGraphManager.anchorBlockIsOnSubLevel((Level)mc.level, anchor)) {
            eye = CoasterAnchorClientSpace.toPlotLocal((Level)mc.level, anchor, eye, partialTick);
        }
        return ChainLiftShaftAxis.resolveFromEye((Level)mc.level, anchor, eye);
    }

    public static void show(Outliner outliner, String key, BlockPos anchor, Vec3 shaftAxis) {
        Vec3 center = Vec3.atCenterOf((Vec3i)anchor);
        CoasterPlotOutliner.showShaftEndCaps(outliner, key, center, shaftAxis, 0.5625, 0.125, 0xFFFFFF);
    }

    public static void clear(Outliner outliner, String key) {
        CoasterPlotOutliner.clearShaftEndCaps(outliner, key);
    }
}

