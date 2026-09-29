/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.silvergold.simulatedcoasters.mixin.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets={"dev.simulated_team.simulated.content.blocks.merging_glue.MergingGlueBlockEntity"}, remap=false)
public abstract class MergingGlueBlockEntityRivetMixin {
    private static final int MERGE_DURATION = 10;
    private static final int MERGE_WAIT = 2;
    @Shadow
    private Rotation endRotation;
    @Shadow
    private BlockPos partnerPosition;
    @Shadow
    private boolean isController;
    @Shadow
    private int ageTicks;

    @Shadow
    private void breakGlue() {
    }

    @Shadow
    private void disassembleToPartner(Rotation rotation) {
    }

    @Inject(method={"tick"}, at={@At(value="INVOKE", target="Ldev/ryanhcode/sable/sublevel/ServerSubLevel;getMassTracker()Ldev/ryanhcode/sable/api/physics/mass/MassData;", ordinal=0)}, cancellable=true, require=0)
    private void simulatedcoasters$mergeForeignSubLevelIntoRivet(CallbackInfo ci) {
        boolean partnerIsRivet;
        BlockEntity self = (BlockEntity)this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!this.isController || this.ageTicks <= 12 || this.partnerPosition == null) {
            return;
        }
        SubLevel selfSub = Sable.HELPER.getContaining(level, (Vec3i)self.getBlockPos());
        SubLevel partnerSub = Sable.HELPER.getContaining(level, (Vec3i)this.partnerPosition);
        boolean selfIsRivet = RivetHostKey.isRivetSubLevel(selfSub);
        if (selfIsRivet == (partnerIsRivet = RivetHostKey.isRivetSubLevel(partnerSub))) {
            return;
        }
        BlockEntity partnerBe = level.getBlockEntity(this.partnerPosition);
        if (partnerBe == null || partnerBe == self || partnerBe.isRemoved()) {
            return;
        }
        this.breakGlue();
        if (partnerIsRivet) {
            this.disassembleToPartner(this.endRotation);
        } else {
            ((MergingGlueBlockEntityRivetMixin)partnerBe).disassembleToPartner(this.endRotation);
        }
        ci.cancel();
    }
}

