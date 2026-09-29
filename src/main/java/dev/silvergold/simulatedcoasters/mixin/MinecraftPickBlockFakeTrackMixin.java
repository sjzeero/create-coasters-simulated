/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackBlockOutline
 *  com.simibubi.create.content.trains.track.TrackBlockOutline$BezierPointSelection
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.HitResult
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.client.track.TrackOutlineBezierAccess;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Minecraft.class})
public class MinecraftPickBlockFakeTrackMixin {
    @WrapOperation(method={"pickBlock"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;")})
    private ItemStack simulatedcoasters$managedFakeTrackPickCoasterTrack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player, Operation<ItemStack> original) {
        BezierConnection bc;
        BezierConnection bc2;
        if (AnchorPeerCurveHit.isActive() && (bc2 = AnchorPeerCurveHit.curve()) != null && bc2.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
        }
        TrackBlockOutline.BezierPointSelection trackSel = TrackBlockOutline.result;
        if (trackSel != null && (bc = TrackOutlineBezierAccess.primaryBezierForSelection(trackSel)) != null && bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
        }
        ItemStack stack = (ItemStack)original.call(new Object[]{state, target, level, pos, player});
        if (!(level instanceof Level)) {
            return stack;
        }
        Level world = (Level)level;
        if (!AnchorPeerFakeTracks.isManagedFakeTrack(world, pos)) {
            return stack;
        }
        return new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
    }

    @Inject(method={"addCustomNbtData"}, at={@At(value="HEAD")}, cancellable=true)
    private void simulatedcoasters$skipFakeTrackCtrlPickComponents(ItemStack stack, BlockEntity blockEntity, RegistryAccess registryAccess, CallbackInfo ci) {
        Level beLevel = blockEntity.getLevel();
        if (beLevel != null && AnchorPeerFakeTracks.isManagedFakeTrack(beLevel, blockEntity.getBlockPos())) {
            ci.cancel();
        }
    }
}

