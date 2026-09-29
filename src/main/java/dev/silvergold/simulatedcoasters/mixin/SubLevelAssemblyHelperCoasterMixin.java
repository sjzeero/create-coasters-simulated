/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  dev.ryanhcode.sable.companion.math.BoundingBox3ic
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonAssembly;
import dev.silvergold.simulatedcoasters.rivet.RivetAssembly;
import dev.silvergold.simulatedcoasters.rivet.RivetAssemblyMoveContext;
import dev.silvergold.simulatedcoasters.rivet.RivetDisassemblyEnforcer;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.assembly.CoasterBezierAssemblyRotationFix;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.glue.CoasterGlueAssemblyRailSplitter;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={SubLevelAssemblyHelper.class})
public class SubLevelAssemblyHelperCoasterMixin {
    private static final ThreadLocal<UUID> RIVET_MOVE_SOURCE_SUBLEVEL = new ThreadLocal();

    @Inject(method={"assembleBlocks"}, at={@At(value="INVOKE", target="Ldev/ryanhcode/sable/api/SubLevelAssemblyHelper;moveOtherStuff(Lnet/minecraft/server/level/ServerLevel;Ldev/ryanhcode/sable/api/SubLevelAssemblyHelper$AssemblyTransform;Ljava/lang/Iterable;Ldev/ryanhcode/sable/companion/math/BoundingBox3ic;)V", shift=At.Shift.BEFORE)})
    private static void simulatedcoasters$splitCoasterRailsCrossingGlueBeforeMove(ServerLevel level, BlockPos anchor, Iterable<BlockPos> blocks, BoundingBox3ic bounds, CallbackInfoReturnable<ServerSubLevel> cir) {
        CoasterGlueAssemblyRailSplitter.splitRailsSpanningGlueBoundary(level, blocks);
    }

    @Inject(method={"moveBlocks"}, at={@At(value="HEAD")})
    private static void simulatedcoasters$beginRivetAssemblyMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        RivetAssemblyMoveContext.begin();
        UUID sourceSubLevelId = null;
        for (BlockPos src : blocks) {
            SubLevel sub;
            if (src == null || (sub = Sable.HELPER.getContaining((Level)level, (Vec3i)src)) == null) continue;
            sourceSubLevelId = sub.getUniqueId();
            break;
        }
        RIVET_MOVE_SOURCE_SUBLEVEL.set(sourceSubLevelId);
    }

    @Inject(method={"moveBlocks"}, at={@At(value="HEAD")})
    private static void simulatedcoasters$detachChainLiftKineticsBeforeMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        for (BlockPos src : blocks) {
            BlockEntity blockEntity;
            if (src == null || !((blockEntity = level.getBlockEntity(src)) instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
            anchor.prepareKineticForAssemblyMove();
        }
    }

    @Inject(method={"moveBlocks"}, at={@At(value="TAIL")})
    private static void simulatedcoasters$breakWorldGridCartBlocksAfterMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        CoasterCartResolve.breakCartBlocksAfterAssemblyMove(level, transform, blocks);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Inject(method={"moveBlocks"}, at={@At(value="TAIL")})
    private static void simulatedcoasters$rebindRivetsAfterMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        try {
            RivetAssembly.onHostBlocksMoved(level, transform, blocks, RIVET_MOVE_SOURCE_SUBLEVEL.get());
            BalloonAssembly.onHostBlocksMoved(level, transform, blocks, RIVET_MOVE_SOURCE_SUBLEVEL.get());
            RivetDisassemblyEnforcer.cleanupRivetBlocksAfterMove(level, transform, blocks);
        }
        finally {
            RIVET_MOVE_SOURCE_SUBLEVEL.remove();
            RivetAssemblyMoveContext.end();
        }
    }

    @Inject(method={"moveBlocks"}, at={@At(value="TAIL")})
    private static void simulatedcoasters$fixCoasterBeziersAfterMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        CoasterBezierAssemblyRotationFix.afterMoveBlocks(level, transform, blocks);
    }
}

