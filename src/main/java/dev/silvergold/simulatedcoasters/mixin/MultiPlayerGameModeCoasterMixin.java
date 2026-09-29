/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackBlockOutline
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.multiplayer.MultiPlayerGameMode
 *  net.minecraft.client.multiplayer.prediction.PredictiveAction
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
 *  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket$Action
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.neoforge.common.CommonHooks
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.gen.Invoker
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.silvergold.simulatedcoasters.PickInteractionGates;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.track.AnchorCurveMiningFilter;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveFlywheelPos;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveMineTarget;
import dev.silvergold.simulatedcoasters.client.track.CoasterConnectedAnchorBreakClient;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.CoasterTrackMaterialBlock;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveDestroyPayload;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurvePendingRemoval;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={MultiPlayerGameMode.class})
public abstract class MultiPlayerGameModeCoasterMixin {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private boolean isDestroying;
    @Shadow
    private int destroyDelay;
    private boolean simulatedcoasters$miningCurve;
    private static final float CREATIVE_CURVED_TRACK_DIG_PER_TICK = 0.125f;
    private BlockPos simulatedcoasters$curveMiningPos;
    private AnchorPeerCurveMineTarget simulatedcoasters$pendingCurveDestroy;
    private float simulatedcoasters$curveMineProgress;
    private float simulatedcoasters$curveMineTicks;
    private ItemStack simulatedcoasters$curveMineItem = ItemStack.EMPTY;
    private boolean simulatedcoasters$miningConnectedAnchor;
    private BlockPos simulatedcoasters$connectedAnchorMiningPos;
    private float simulatedcoasters$connectedAnchorProgress;
    private float simulatedcoasters$connectedAnchorTicks;
    private ItemStack simulatedcoasters$connectedAnchorItem = ItemStack.EMPTY;

    @Invoker(value="ensureHasSentCarriedItem")
    protected abstract void simulatedcoasters$ensureHasSentCarriedItem();

    @Invoker(value="startPrediction")
    protected abstract void simulatedcoasters$invokeStartPrediction(ClientLevel var1, PredictiveAction var2);

    @Inject(method={"stopDestroyBlock"}, at={@At(value="HEAD")})
    private void simulatedcoasters$releaseCurveMine(CallbackInfo ci) {
        this.simulatedcoasters$abortCurveMiningClient();
        this.simulatedcoasters$abortConnectedAnchorMiningClient();
    }

    @Inject(method={"startDestroyBlock"}, at={@At(value="HEAD")}, cancellable=true)
    private void simulatedcoasters$startDestroyHoveredAnchorPeerCurve(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (this.minecraft.player == null || this.minecraft.level == null) {
            return;
        }
        if (this.minecraft.player.blockActionRestricted((Level)this.minecraft.level, loc, ((MultiPlayerGameMode)this).getPlayerMode())) {
            return;
        }
        if (!this.minecraft.level.getWorldBorder().isWithinBounds(loc)) {
            return;
        }
        Optional<AnchorPeerCurveMineTarget> pending = MultiPlayerGameModeCoasterMixin.resolvePendingCurveDestroy(loc);
        if (pending.isPresent()) {
            BlockPos mineLoc = MultiPlayerGameModeCoasterMixin.preferredCurveMinePos(loc, pending.get());
            if (this.simulatedcoasters$miningConnectedAnchor) {
                this.simulatedcoasters$abortConnectedAnchorMiningClient();
            }
            if (this.simulatedcoasters$miningCurve && this.simulatedcoasters$sameAnchorPeerEdge(pending.get()) && !this.simulatedcoasters$curveMineItem.shouldCauseBlockBreakReset(this.minecraft.player.getMainHandItem())) {
                this.simulatedcoasters$curveMiningPos = mineLoc.immutable();
                cir.setReturnValue((Object)true);
                cir.cancel();
                return;
            }
            if (this.simulatedcoasters$miningCurve && !this.simulatedcoasters$sameAnchorPeerEdge(pending.get())) {
                this.simulatedcoasters$abortCurveMiningClient();
            }
            if (this.isDestroying) {
                ((MultiPlayerGameMode)this).stopDestroyBlock();
            }
            BlockState blockstate1 = this.minecraft.level.getBlockState(mineLoc);
            this.simulatedcoasters$beginCurveMiningSession(pending.get(), mineLoc);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, mineLoc, blockstate1, 0.0f);
            MultiPlayerGameModeCoasterMixin.setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), pending.get(), -1);
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        if (this.simulatedcoasters$miningCurve && !loc.equals((Object)this.simulatedcoasters$curveMiningPos)) {
            this.simulatedcoasters$abortCurveMiningClient();
        } else if (this.simulatedcoasters$miningCurve && loc.equals((Object)this.simulatedcoasters$curveMiningPos)) {
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        if (!CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak((Level)this.minecraft.level, loc)) {
            return;
        }
        if (this.simulatedcoasters$miningCurve) {
            this.simulatedcoasters$abortCurveMiningClient();
        }
        if (this.simulatedcoasters$miningConnectedAnchor && loc.equals((Object)this.simulatedcoasters$connectedAnchorMiningPos) && !this.simulatedcoasters$connectedAnchorItem.shouldCauseBlockBreakReset(this.minecraft.player.getMainHandItem())) {
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        if (this.simulatedcoasters$miningConnectedAnchor && !loc.equals((Object)this.simulatedcoasters$connectedAnchorMiningPos)) {
            this.simulatedcoasters$abortConnectedAnchorMiningClient();
        }
        if (this.isDestroying) {
            ((MultiPlayerGameMode)this).stopDestroyBlock();
        }
        BlockState anchorState = this.minecraft.level.getBlockState(loc);
        this.simulatedcoasters$beginConnectedAnchorMiningSession(loc);
        this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, loc, anchorState, 0.0f);
        cir.setReturnValue((Object)true);
        cir.cancel();
    }

    @Inject(method={"continueDestroyBlock"}, at={@At(value="HEAD")}, cancellable=true)
    private void simulatedcoasters$continueCurveMine(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        if (this.simulatedcoasters$miningCurve) {
            this.simulatedcoasters$continueCurveMineInner(posBlock, directionFacing, cir);
            return;
        }
        if (this.simulatedcoasters$miningConnectedAnchor) {
            this.simulatedcoasters$continueConnectedAnchorMine(posBlock, directionFacing, cir);
            return;
        }
        Optional<AnchorPeerCurveMineTarget> pending = MultiPlayerGameModeCoasterMixin.resolvePendingCurveDestroy(posBlock);
        if (pending.isPresent() && this.minecraft.player != null && this.minecraft.level != null) {
            BlockPos mineLoc = MultiPlayerGameModeCoasterMixin.preferredCurveMinePos(posBlock, pending.get());
            if (this.isDestroying) {
                ((MultiPlayerGameMode)this).stopDestroyBlock();
            }
            BlockState blockstate1 = this.minecraft.level.getBlockState(mineLoc);
            this.simulatedcoasters$beginCurveMiningSession(pending.get(), mineLoc);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, mineLoc, blockstate1, 0.0f);
            MultiPlayerGameModeCoasterMixin.setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), pending.get(), -1);
            this.simulatedcoasters$continueCurveMineInner(posBlock, directionFacing, cir);
            return;
        }
        if (CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak((Level)this.minecraft.level, posBlock) && this.minecraft.player != null) {
            if (this.simulatedcoasters$miningCurve) {
                this.simulatedcoasters$abortCurveMiningClient();
            }
            if (this.isDestroying) {
                ((MultiPlayerGameMode)this).stopDestroyBlock();
            }
            BlockState anchorState = this.minecraft.level.getBlockState(posBlock);
            this.simulatedcoasters$beginConnectedAnchorMiningSession(posBlock);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, anchorState, 0.0f);
            this.simulatedcoasters$continueConnectedAnchorMine(posBlock, directionFacing, cir);
        }
    }

    private void simulatedcoasters$beginCurveMiningSession(AnchorPeerCurveMineTarget curveTarget, BlockPos loc) {
        this.simulatedcoasters$miningCurve = true;
        this.simulatedcoasters$curveMiningPos = loc.immutable();
        this.simulatedcoasters$pendingCurveDestroy = curveTarget;
        AnchorCurveMiningFilter.beginMining(curveTarget.mined(), curveTarget.peer());
        this.simulatedcoasters$curveMineProgress = 0.0f;
        this.simulatedcoasters$curveMineTicks = 0.0f;
        this.simulatedcoasters$curveMineItem = this.minecraft.player.getMainHandItem();
    }

    private void simulatedcoasters$beginConnectedAnchorMiningSession(BlockPos loc) {
        this.simulatedcoasters$miningConnectedAnchor = true;
        this.simulatedcoasters$connectedAnchorMiningPos = loc.immutable();
        this.simulatedcoasters$connectedAnchorProgress = 0.0f;
        this.simulatedcoasters$connectedAnchorTicks = 0.0f;
        this.simulatedcoasters$connectedAnchorItem = this.minecraft.player.getMainHandItem();
        this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), loc, -1);
    }

    private void simulatedcoasters$continueCurveMineInner(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        boolean sameCurveSession;
        this.simulatedcoasters$ensureHasSentCarriedItem();
        if (this.destroyDelay > 0) {
            --this.destroyDelay;
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        Optional<AnchorPeerCurveMineTarget> still = MultiPlayerGameModeCoasterMixin.resolvePendingCurveDestroy(posBlock);
        boolean bl = sameCurveSession = still.isPresent() && this.simulatedcoasters$pendingCurveDestroy != null && MultiPlayerGameModeCoasterMixin.sameAnchorPeerEdge(still.get(), this.simulatedcoasters$pendingCurveDestroy);
        if (!sameCurveSession) {
            this.simulatedcoasters$abortCurveMiningClient();
            Optional<AnchorPeerCurveMineTarget> newCurve = MultiPlayerGameModeCoasterMixin.resolvePendingCurveDestroy(posBlock);
            if (newCurve.isPresent()) {
                this.simulatedcoasters$beginCurveMiningSession(newCurve.get(), posBlock);
                BlockState st = this.minecraft.level.getBlockState(posBlock);
                this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, st, 0.0f);
                MultiPlayerGameModeCoasterMixin.setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), newCurve.get(), -1);
                this.simulatedcoasters$continueCurveMineInner(posBlock, directionFacing, cir);
                return;
            }
            if (CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak((Level)this.minecraft.level, posBlock)) {
                this.simulatedcoasters$beginConnectedAnchorMiningSession(posBlock);
                BlockState anchorState = this.minecraft.level.getBlockState(posBlock);
                this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, anchorState, 0.0f);
                this.simulatedcoasters$continueConnectedAnchorMine(posBlock, directionFacing, cir);
                return;
            }
            return;
        }
        this.simulatedcoasters$curveMiningPos = posBlock.immutable();
        if (this.simulatedcoasters$curveMineItem.shouldCauseBlockBreakReset(this.minecraft.player.getMainHandItem())) {
            this.simulatedcoasters$abortCurveMiningClient();
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        BlockState blockstate = this.minecraft.level.getBlockState(posBlock);
        if (blockstate.isAir()) {
            this.simulatedcoasters$abortCurveMiningClient();
            cir.setReturnValue((Object)false);
            cir.cancel();
            return;
        }
        BlockState digState = this.simulatedcoasters$curveDigReferenceState(this.simulatedcoasters$pendingCurveDestroy);
        BlockPos digPos = this.simulatedcoasters$pendingCurveDestroy != null ? this.simulatedcoasters$pendingCurveDestroy.mined() : posBlock;
        float digIncrement = this.simulatedcoasters$survivalCurveDigIncrement(digState, digPos);
        this.simulatedcoasters$curveMineProgress += digIncrement;
        if (this.simulatedcoasters$curveMineTicks % 4.0f == 0.0f) {
            SoundType soundtype = digState.getSoundType((LevelReader)this.minecraft.level, digPos, (Entity)this.minecraft.player);
            this.minecraft.getSoundManager().play((SoundInstance)new SimpleSoundInstance(soundtype.getHitSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0f) / 8.0f, soundtype.getPitch() * 0.5f, SoundInstance.createUnseededRandom(), posBlock));
        }
        this.simulatedcoasters$curveMineTicks += 1.0f;
        this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, blockstate, Mth.clamp((float)this.simulatedcoasters$curveMineProgress, (float)0.0f, (float)1.0f));
        if (CommonHooks.onClientMineHold((Player)this.minecraft.player, (BlockPos)posBlock, (Direction)directionFacing).getUseItem().isFalse()) {
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        if (this.simulatedcoasters$curveMineProgress >= 1.0f) {
            AnchorPeerCurveMineTarget done = this.simulatedcoasters$pendingCurveDestroy;
            this.simulatedcoasters$abortCurveMiningClient();
            if (done != null) {
                MultiPlayerGameModeCoasterMixin.sendCurveDestroyPacket(done);
            }
            this.destroyDelay = 5;
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        int stage = this.simulatedcoasters$curveMineProgress > 0.0f ? (int)(this.simulatedcoasters$curveMineProgress * 10.0f) : -1;
        MultiPlayerGameModeCoasterMixin.setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), this.simulatedcoasters$pendingCurveDestroy, stage);
        cir.setReturnValue((Object)true);
        cir.cancel();
    }

    private void simulatedcoasters$continueConnectedAnchorMine(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
        this.simulatedcoasters$ensureHasSentCarriedItem();
        if (this.destroyDelay > 0) {
            --this.destroyDelay;
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        if (!posBlock.equals((Object)this.simulatedcoasters$connectedAnchorMiningPos) || !CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak((Level)this.minecraft.level, posBlock)) {
            this.simulatedcoasters$abortConnectedAnchorMiningClient();
            Optional<AnchorPeerCurveMineTarget> newCurve = MultiPlayerGameModeCoasterMixin.resolvePendingCurveDestroy(posBlock);
            if (newCurve.isPresent()) {
                this.simulatedcoasters$beginCurveMiningSession(newCurve.get(), posBlock);
                BlockState st = this.minecraft.level.getBlockState(posBlock);
                this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, st, 0.0f);
                MultiPlayerGameModeCoasterMixin.setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), newCurve.get(), -1);
                this.simulatedcoasters$continueCurveMineInner(posBlock, directionFacing, cir);
                return;
            }
            return;
        }
        if (this.simulatedcoasters$connectedAnchorItem.shouldCauseBlockBreakReset(this.minecraft.player.getMainHandItem())) {
            this.simulatedcoasters$abortConnectedAnchorMiningClient();
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        BlockState blockstate = this.minecraft.level.getBlockState(posBlock);
        if (blockstate.isAir()) {
            this.simulatedcoasters$abortConnectedAnchorMiningClient();
            cir.setReturnValue((Object)false);
            cir.cancel();
            return;
        }
        float digIncrement = this.simulatedcoasters$survivalCurveDigIncrement(blockstate, posBlock);
        this.simulatedcoasters$connectedAnchorProgress += digIncrement;
        if (this.simulatedcoasters$connectedAnchorTicks % 4.0f == 0.0f) {
            SoundType soundtype = blockstate.getSoundType((LevelReader)this.minecraft.level, posBlock, (Entity)this.minecraft.player);
            this.minecraft.getSoundManager().play((SoundInstance)new SimpleSoundInstance(soundtype.getHitSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0f) / 8.0f, soundtype.getPitch() * 0.5f, SoundInstance.createUnseededRandom(), posBlock));
        }
        this.simulatedcoasters$connectedAnchorTicks += 1.0f;
        this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, blockstate, Mth.clamp((float)this.simulatedcoasters$connectedAnchorProgress, (float)0.0f, (float)1.0f));
        if (CommonHooks.onClientMineHold((Player)this.minecraft.player, (BlockPos)posBlock, (Direction)directionFacing).getUseItem().isFalse()) {
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        if (this.simulatedcoasters$connectedAnchorProgress >= 1.0f) {
            BlockPos donePos = posBlock.immutable();
            Direction dir = directionFacing;
            this.simulatedcoasters$abortConnectedAnchorMiningClient();
            MultiPlayerGameMode self = (MultiPlayerGameMode)this;
            ClientLevel clientLevel = this.minecraft.level;
            boolean creative = self.getPlayerMode().isCreative();
            this.simulatedcoasters$invokeStartPrediction(clientLevel, sequence -> {
                self.destroyBlock(donePos);
                ServerboundPlayerActionPacket.Action packetAction = creative ? ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK : ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK;
                return new ServerboundPlayerActionPacket(packetAction, donePos, dir, sequence);
            });
            this.destroyDelay = 5;
            cir.setReturnValue((Object)true);
            cir.cancel();
            return;
        }
        int stage = this.simulatedcoasters$connectedAnchorProgress > 0.0f ? (int)(this.simulatedcoasters$connectedAnchorProgress * 10.0f) : -1;
        this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), posBlock, stage);
        cir.setReturnValue((Object)true);
        cir.cancel();
    }

    @Inject(method={"destroyBlock"}, at={@At(value="HEAD")}, cancellable=true)
    private void simulatedcoasters$destroyHoveredAnchorPeerCurve(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Optional<AnchorPeerCurveMineTarget> at = MultiPlayerGameModeCoasterMixin.resolvePendingCurveDestroy(pos);
        if (this.simulatedcoasters$miningCurve && this.simulatedcoasters$pendingCurveDestroy != null && at.isPresent() && MultiPlayerGameModeCoasterMixin.sameAnchorPeerEdge(at.get(), this.simulatedcoasters$pendingCurveDestroy)) {
            cir.setReturnValue((Object)false);
            cir.cancel();
            return;
        }
        if (this.simulatedcoasters$miningConnectedAnchor && pos.equals((Object)this.simulatedcoasters$connectedAnchorMiningPos)) {
            cir.setReturnValue((Object)false);
            cir.cancel();
            return;
        }
        if (this.tryInstantCurveDestroyOnBlock(pos)) {
            cir.setReturnValue((Object)true);
            cir.cancel();
        }
    }

    private void simulatedcoasters$abortCurveMiningClient() {
        if (!this.simulatedcoasters$miningCurve) {
            return;
        }
        AnchorPeerCurveMineTarget pending = this.simulatedcoasters$pendingCurveDestroy;
        BlockPos loneMiningPos = this.simulatedcoasters$curveMiningPos;
        this.simulatedcoasters$miningCurve = false;
        this.simulatedcoasters$curveMiningPos = null;
        this.simulatedcoasters$pendingCurveDestroy = null;
        this.simulatedcoasters$curveMineProgress = 0.0f;
        this.simulatedcoasters$curveMineTicks = 0.0f;
        this.simulatedcoasters$curveMineItem = ItemStack.EMPTY;
        if (this.minecraft.player != null && this.minecraft.level != null) {
            if (pending != null) {
                MultiPlayerGameModeCoasterMixin.setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), pending, -1);
            } else if (loneMiningPos != null) {
                this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), loneMiningPos, -1);
            }
        }
        AnchorCurveMiningFilter.clear();
    }

    private void simulatedcoasters$abortConnectedAnchorMiningClient() {
        if (!this.simulatedcoasters$miningConnectedAnchor) {
            return;
        }
        BlockPos p = this.simulatedcoasters$connectedAnchorMiningPos;
        this.simulatedcoasters$miningConnectedAnchor = false;
        this.simulatedcoasters$connectedAnchorMiningPos = null;
        this.simulatedcoasters$connectedAnchorProgress = 0.0f;
        this.simulatedcoasters$connectedAnchorTicks = 0.0f;
        this.simulatedcoasters$connectedAnchorItem = ItemStack.EMPTY;
        if (this.minecraft.player != null && this.minecraft.level != null && p != null) {
            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), p, -1);
        }
    }

    private boolean simulatedcoasters$sameAnchorPeerEdge(AnchorPeerCurveMineTarget pendingNow) {
        return this.simulatedcoasters$pendingCurveDestroy != null && MultiPlayerGameModeCoasterMixin.sameAnchorPeerEdge(pendingNow, this.simulatedcoasters$pendingCurveDestroy);
    }

    private static boolean sameAnchorPeerEdge(AnchorPeerCurveMineTarget a, AnchorPeerCurveMineTarget b) {
        if (a.wrench() != b.wrench()) {
            return false;
        }
        long a0 = Math.min(a.mined().asLong(), a.peer().asLong());
        long a1 = Math.max(a.mined().asLong(), a.peer().asLong());
        long b0 = Math.min(b.mined().asLong(), b.peer().asLong());
        long b1 = Math.max(b.mined().asLong(), b.peer().asLong());
        return a0 == b0 && a1 == b1;
    }

    private static Optional<AnchorPeerCurveMineTarget> resolvePendingCurveDestroy(BlockPos pos) {
        boolean miningHoveredCurve;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return Optional.empty();
        }
        if (!AnchorPeerCurveHit.isActive()) {
            return Optional.empty();
        }
        BezierConnection bc = AnchorPeerCurveHit.curve();
        if (bc == null) {
            return Optional.empty();
        }
        BlockPos first = (BlockPos)bc.bePositions.getFirst();
        BlockPos second = (BlockPos)bc.bePositions.getSecond();
        BlockPos focus = AnchorPeerCurveHit.focusAnchor();
        if (focus == null) {
            return Optional.empty();
        }
        BlockPos raster = AnchorPeerCurveHit.rasterBlockPos();
        boolean hitRasterFake = AnchorPeerFakeTracks.rasterContains(bc, pos) || raster != null && pos.equals((Object)raster);
        boolean hitFocusAnchor = pos.equals((Object)focus);
        boolean bl = miningHoveredCurve = PickInteractionGates.isDestroyInputDown(mc) && raster != null;
        if (!(hitRasterFake || hitFocusAnchor || miningHoveredCurve)) {
            return Optional.empty();
        }
        BlockPos mined = focus;
        BlockPos peer = mined.equals((Object)first) ? second : first;
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        boolean wrench = AllItems.WRENCH.isIn(main) || AllItems.WRENCH.isIn(off);
        return Optional.of(new AnchorPeerCurveMineTarget(mined, peer, wrench));
    }

    private static BlockPos preferredCurveMinePos(BlockPos loc, AnchorPeerCurveMineTarget pending) {
        BlockPos raster = AnchorPeerCurveHit.rasterBlockPos();
        if (raster != null) {
            return raster;
        }
        if (AnchorPeerCurveHit.curve() != null && AnchorPeerFakeTracks.rasterContains(AnchorPeerCurveHit.curve(), loc)) {
            return loc;
        }
        return pending.mined();
    }

    private static void setAnchorCurveDestroyProgress(Minecraft mc, int breakerId, AnchorPeerCurveMineTarget pending, int stage) {
        BlockPos flywheelPos = AnchorPeerCurveFlywheelPos.crumbleAnchor((Level)mc.level, pending);
        mc.level.destroyBlockProgress(breakerId, flywheelPos, stage);
    }

    private static void sendCurveDestroyPacket(AnchorPeerCurveMineTarget data) {
        AnchorPeerCurvePendingRemoval.mark(data.mined(), data.peer());
        PacketDistributor.sendToServer((CustomPacketPayload)new AnchorPeerCurveDestroyPayload(data.mined(), data.peer(), data.wrench()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        AnchorPeerCurveHit.clear();
        TrackBlockOutline.result = null;
    }

    private boolean tryInstantCurveDestroyOnBlock(BlockPos pos) {
        Optional<AnchorPeerCurveMineTarget> pending = MultiPlayerGameModeCoasterMixin.resolvePendingCurveDestroy(pos);
        if (pending.isEmpty()) {
            return false;
        }
        BlockState st = this.minecraft.level.getBlockState(pos);
        if (st.getBlock() instanceof CoasterAnchorpointBlock) {
            return false;
        }
        GameType mode = ((MultiPlayerGameMode)this).getPlayerMode();
        if (mode.isCreative()) {
            return false;
        }
        AnchorPeerCurveMineTarget target = pending.get();
        BlockState digState = this.simulatedcoasters$curveDigReferenceState(target);
        BlockPos digPos = target.mined();
        if (st.isAir() || digState.getDestroyProgress((Player)this.minecraft.player, (BlockGetter)this.minecraft.player.level(), digPos) < 1.0f) {
            return false;
        }
        MultiPlayerGameModeCoasterMixin.sendCurveDestroyPacket(target);
        return true;
    }

    private BlockState simulatedcoasters$curveDigReferenceState(@Nullable AnchorPeerCurveMineTarget pending) {
        if (pending == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return ((CoasterTrackMaterialBlock)((Object)SimulatedCoastersBlocks.COASTER_TRACK_MATERIAL.get())).defaultBlockState();
    }

    private float simulatedcoasters$survivalCurveDigIncrement(BlockState digState, BlockPos digPos) {
        GameType playerMode = ((MultiPlayerGameMode)this).getPlayerMode();
        if (playerMode.isCreative()) {
            return 0.125f;
        }
        return digState.getDestroyProgress((Player)this.minecraft.player, (BlockGetter)this.minecraft.player.level(), digPos) * CoasterTrackGauge.survivalCurveDigScale((Player)this.minecraft.player);
    }
}

