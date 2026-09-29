/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.TrackPlacement
 *  com.simibubi.create.content.trains.track.TrackPlacement$PlacementInfo
 *  net.createmod.catnip.math.AngleHelper
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.trains.track.TrackPlacement;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={TrackPlacement.class})
public class TrackPlacementTryConnectCoasterMixin {
    @Shadow
    private static TrackPlacement.PlacementInfo cached;
    @Shadow
    private static BlockPos hoveringPos;
    @Shadow
    private static boolean hoveringMaxed;
    @Shadow
    private static int hoveringAngle;
    @Shadow
    private static ItemStack lastItem;

    @Inject(method={"tryConnect"}, at={@At(value="HEAD")}, cancellable=true)
    private static void simulatedcoasters$denyCreateTryConnectWhenHoldingCoasterItem(Level level, Player player, BlockPos pos2, BlockState state2, ItemStack stack, boolean girder, boolean maximiseTurn, CallbackInfoReturnable<TrackPlacement.PlacementInfo> cir) {
        if (!(stack.getItem() instanceof CoasterTrackItem)) {
            return;
        }
        Vec3 lookVec = player.getLookAngle();
        int lookAngle = (int)(22.5 + (double)(AngleHelper.deg((double)Mth.atan2((double)lookVec.z, (double)lookVec.x)) % 360.0f)) / 8;
        if (level.isClientSide() && cached != null && pos2.equals((Object)hoveringPos) && stack.equals(lastItem) && hoveringMaxed == maximiseTurn && lookAngle == hoveringAngle) {
            cir.setReturnValue((Object)cached);
            return;
        }
        TrackPlacement.PlacementInfo deny = TrackPlacementTryConnectCoasterMixin.blocked(stack);
        TrackPlacementTryConnectCoasterMixin.assignClientCache(level, pos2, stack, maximiseTurn, lookAngle, deny);
        cir.setReturnValue((Object)deny);
    }

    private static void assignClientCache(Level level, BlockPos pos2, ItemStack stack, boolean maximiseTurn, int lookAngle, TrackPlacement.PlacementInfo deny) {
        if (!level.isClientSide()) {
            return;
        }
        cached = deny;
        hoveringMaxed = maximiseTurn;
        hoveringAngle = lookAngle;
        hoveringPos = pos2;
        lastItem = stack;
    }

    private static TrackPlacement.PlacementInfo blocked(ItemStack stack) {
        return new TrackPlacement.PlacementInfo(CoasterTrackMaterials.COASTER).tooJumbly();
    }
}

