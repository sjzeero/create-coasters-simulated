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
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
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

@Mixin({MultiPlayerGameMode.class})
public abstract class MultiPlayerGameModeCoasterMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   private boolean isDestroying;
   @Shadow
   private int destroyDelay;
   private boolean simulatedcoasters$miningCurve;
   private static final float CREATIVE_CURVED_TRACK_DIG_PER_TICK = 0.125F;
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

   @Invoker("ensureHasSentCarriedItem")
   protected abstract void simulatedcoasters$ensureHasSentCarriedItem();

   @Invoker("startPrediction")
   protected abstract void simulatedcoasters$invokeStartPrediction(ClientLevel var1, PredictiveAction var2);

   @Inject(
      method = {"stopDestroyBlock"},
      at = {@At("HEAD")}
   )
   private void simulatedcoasters$releaseCurveMine(CallbackInfo ci) {
      this.simulatedcoasters$abortCurveMiningClient();
      this.simulatedcoasters$abortConnectedAnchorMiningClient();
   }

   @Inject(
      method = {"startDestroyBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$startDestroyHoveredAnchorPeerCurve(BlockPos loc, Direction face, CallbackInfoReturnable<Boolean> cir) {
      if (this.minecraft.player != null && this.minecraft.level != null) {
         if (!this.minecraft.player.blockActionRestricted(this.minecraft.level, loc, ((MultiPlayerGameMode)(Object)this).getPlayerMode())) {
            if (this.minecraft.level.getWorldBorder().isWithinBounds(loc)) {
               Optional<AnchorPeerCurveMineTarget> pending = resolvePendingCurveDestroy(loc);
               if (pending.isPresent()) {
                  BlockPos mineLoc = preferredCurveMinePos(loc, pending.get());
                  if (this.simulatedcoasters$miningConnectedAnchor) {
                     this.simulatedcoasters$abortConnectedAnchorMiningClient();
                  }

                  if (this.simulatedcoasters$miningCurve
                     && this.simulatedcoasters$sameAnchorPeerEdge(pending.get())
                     && !this.simulatedcoasters$curveMineItem.shouldCauseBlockBreakReset(this.minecraft.player.getMainHandItem())) {
                     this.simulatedcoasters$curveMiningPos = mineLoc.immutable();
                     cir.setReturnValue(true);
                     cir.cancel();
                  } else {
                     if (this.simulatedcoasters$miningCurve && !this.simulatedcoasters$sameAnchorPeerEdge(pending.get())) {
                        this.simulatedcoasters$abortCurveMiningClient();
                     }

                     if (this.isDestroying) {
                        ((MultiPlayerGameMode)(Object)this).stopDestroyBlock();
                     }

                     BlockState blockstate1 = this.minecraft.level.getBlockState(mineLoc);
                     this.simulatedcoasters$beginCurveMiningSession(pending.get(), mineLoc);
                     this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, mineLoc, blockstate1, 0.0F);
                     setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), pending.get(), -1);
                     cir.setReturnValue(true);
                     cir.cancel();
                  }
               } else {
                  if (this.simulatedcoasters$miningCurve && !loc.equals(this.simulatedcoasters$curveMiningPos)) {
                     this.simulatedcoasters$abortCurveMiningClient();
                  } else if (this.simulatedcoasters$miningCurve && loc.equals(this.simulatedcoasters$curveMiningPos)) {
                     cir.setReturnValue(true);
                     cir.cancel();
                     return;
                  }

                  if (CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak(this.minecraft.level, loc)) {
                     if (this.simulatedcoasters$miningCurve) {
                        this.simulatedcoasters$abortCurveMiningClient();
                     }

                     if (this.simulatedcoasters$miningConnectedAnchor
                        && loc.equals(this.simulatedcoasters$connectedAnchorMiningPos)
                        && !this.simulatedcoasters$connectedAnchorItem.shouldCauseBlockBreakReset(this.minecraft.player.getMainHandItem())) {
                        cir.setReturnValue(true);
                        cir.cancel();
                     } else {
                        if (this.simulatedcoasters$miningConnectedAnchor && !loc.equals(this.simulatedcoasters$connectedAnchorMiningPos)) {
                           this.simulatedcoasters$abortConnectedAnchorMiningClient();
                        }

                        if (this.isDestroying) {
                           ((MultiPlayerGameMode)(Object)this).stopDestroyBlock();
                        }

                        BlockState anchorState = this.minecraft.level.getBlockState(loc);
                        this.simulatedcoasters$beginConnectedAnchorMiningSession(loc);
                        this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, loc, anchorState, 0.0F);
                        cir.setReturnValue(true);
                        cir.cancel();
                     }
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {"continueDestroyBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$continueCurveMine(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
      if (this.simulatedcoasters$miningCurve) {
         this.simulatedcoasters$continueCurveMineInner(posBlock, directionFacing, cir);
      } else if (this.simulatedcoasters$miningConnectedAnchor) {
         this.simulatedcoasters$continueConnectedAnchorMine(posBlock, directionFacing, cir);
      } else {
         Optional<AnchorPeerCurveMineTarget> pending = resolvePendingCurveDestroy(posBlock);
         if (pending.isPresent() && this.minecraft.player != null && this.minecraft.level != null) {
            BlockPos mineLoc = preferredCurveMinePos(posBlock, pending.get());
            if (this.isDestroying) {
               ((MultiPlayerGameMode)(Object)this).stopDestroyBlock();
            }

            BlockState blockstate1 = this.minecraft.level.getBlockState(mineLoc);
            this.simulatedcoasters$beginCurveMiningSession(pending.get(), mineLoc);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, mineLoc, blockstate1, 0.0F);
            setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), pending.get(), -1);
            this.simulatedcoasters$continueCurveMineInner(posBlock, directionFacing, cir);
         } else {
            if (CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak(this.minecraft.level, posBlock) && this.minecraft.player != null) {
               if (this.simulatedcoasters$miningCurve) {
                  this.simulatedcoasters$abortCurveMiningClient();
               }

               if (this.isDestroying) {
                  ((MultiPlayerGameMode)(Object)this).stopDestroyBlock();
               }

               BlockState anchorState = this.minecraft.level.getBlockState(posBlock);
               this.simulatedcoasters$beginConnectedAnchorMiningSession(posBlock);
               this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, anchorState, 0.0F);
               this.simulatedcoasters$continueConnectedAnchorMine(posBlock, directionFacing, cir);
            }
         }
      }
   }

   private void simulatedcoasters$beginCurveMiningSession(AnchorPeerCurveMineTarget curveTarget, BlockPos loc) {
      this.simulatedcoasters$miningCurve = true;
      this.simulatedcoasters$curveMiningPos = loc.immutable();
      this.simulatedcoasters$pendingCurveDestroy = curveTarget;
      AnchorCurveMiningFilter.beginMining(curveTarget.mined(), curveTarget.peer());
      this.simulatedcoasters$curveMineProgress = 0.0F;
      this.simulatedcoasters$curveMineTicks = 0.0F;
      this.simulatedcoasters$curveMineItem = this.minecraft.player.getMainHandItem();
   }

   private void simulatedcoasters$beginConnectedAnchorMiningSession(BlockPos loc) {
      this.simulatedcoasters$miningConnectedAnchor = true;
      this.simulatedcoasters$connectedAnchorMiningPos = loc.immutable();
      this.simulatedcoasters$connectedAnchorProgress = 0.0F;
      this.simulatedcoasters$connectedAnchorTicks = 0.0F;
      this.simulatedcoasters$connectedAnchorItem = this.minecraft.player.getMainHandItem();
      this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), loc, -1);
   }

   private void simulatedcoasters$continueCurveMineInner(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
      this.simulatedcoasters$ensureHasSentCarriedItem();
      if (this.destroyDelay > 0) {
         this.destroyDelay--;
         cir.setReturnValue(true);
         cir.cancel();
      } else {
         Optional<AnchorPeerCurveMineTarget> still = resolvePendingCurveDestroy(posBlock);
         boolean sameCurveSession = still.isPresent()
            && this.simulatedcoasters$pendingCurveDestroy != null
            && sameAnchorPeerEdge(still.get(), this.simulatedcoasters$pendingCurveDestroy);
         if (!sameCurveSession) {
            this.simulatedcoasters$abortCurveMiningClient();
            Optional<AnchorPeerCurveMineTarget> newCurve = resolvePendingCurveDestroy(posBlock);
            if (newCurve.isPresent()) {
               this.simulatedcoasters$beginCurveMiningSession(newCurve.get(), posBlock);
               BlockState st = this.minecraft.level.getBlockState(posBlock);
               this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, st, 0.0F);
               setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), newCurve.get(), -1);
               this.simulatedcoasters$continueCurveMineInner(posBlock, directionFacing, cir);
            } else if (CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak(this.minecraft.level, posBlock)) {
               this.simulatedcoasters$beginConnectedAnchorMiningSession(posBlock);
               BlockState anchorState = this.minecraft.level.getBlockState(posBlock);
               this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, anchorState, 0.0F);
               this.simulatedcoasters$continueConnectedAnchorMine(posBlock, directionFacing, cir);
            }
         } else {
            this.simulatedcoasters$curveMiningPos = posBlock.immutable();
            if (this.simulatedcoasters$curveMineItem.shouldCauseBlockBreakReset(this.minecraft.player.getMainHandItem())) {
               this.simulatedcoasters$abortCurveMiningClient();
               cir.setReturnValue(true);
               cir.cancel();
            } else {
               BlockState blockstate = this.minecraft.level.getBlockState(posBlock);
               if (blockstate.isAir()) {
                  this.simulatedcoasters$abortCurveMiningClient();
                  cir.setReturnValue(false);
                  cir.cancel();
               } else {
                  BlockState digState = this.simulatedcoasters$curveDigReferenceState(this.simulatedcoasters$pendingCurveDestroy);
                  BlockPos digPos = this.simulatedcoasters$pendingCurveDestroy != null ? this.simulatedcoasters$pendingCurveDestroy.mined() : posBlock;
                  float digIncrement = this.simulatedcoasters$survivalCurveDigIncrement(digState, digPos);
                  this.simulatedcoasters$curveMineProgress += digIncrement;
                  if (this.simulatedcoasters$curveMineTicks % 4.0F == 0.0F) {
                     SoundType soundtype = digState.getSoundType(this.minecraft.level, digPos, this.minecraft.player);
                     this.minecraft
                        .getSoundManager()
                        .play(
                           new SimpleSoundInstance(
                              soundtype.getHitSound(),
                              SoundSource.BLOCKS,
                              (soundtype.getVolume() + 1.0F) / 8.0F,
                              soundtype.getPitch() * 0.5F,
                              SoundInstance.createUnseededRandom(),
                              posBlock
                           )
                        );
                  }

                  this.simulatedcoasters$curveMineTicks++;
                  this.minecraft
                     .getTutorial()
                     .onDestroyBlock(this.minecraft.level, posBlock, blockstate, Mth.clamp(this.simulatedcoasters$curveMineProgress, 0.0F, 1.0F));
                  if (CommonHooks.onClientMineHold(this.minecraft.player, posBlock, directionFacing).getUseItem().isFalse()) {
                     cir.setReturnValue(true);
                     cir.cancel();
                  } else if (this.simulatedcoasters$curveMineProgress >= 1.0F) {
                     AnchorPeerCurveMineTarget done = this.simulatedcoasters$pendingCurveDestroy;
                     this.simulatedcoasters$abortCurveMiningClient();
                     if (done != null) {
                        sendCurveDestroyPacket(done);
                     }

                     this.destroyDelay = 5;
                     cir.setReturnValue(true);
                     cir.cancel();
                  } else {
                     int stage = this.simulatedcoasters$curveMineProgress > 0.0F ? (int)(this.simulatedcoasters$curveMineProgress * 10.0F) : -1;
                     setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), this.simulatedcoasters$pendingCurveDestroy, stage);
                     cir.setReturnValue(true);
                     cir.cancel();
                  }
               }
            }
         }
      }
   }

   private void simulatedcoasters$continueConnectedAnchorMine(BlockPos posBlock, Direction directionFacing, CallbackInfoReturnable<Boolean> cir) {
      this.simulatedcoasters$ensureHasSentCarriedItem();
      if (this.destroyDelay > 0) {
         this.destroyDelay--;
         cir.setReturnValue(true);
         cir.cancel();
      } else if (posBlock.equals(this.simulatedcoasters$connectedAnchorMiningPos)
         && CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak(this.minecraft.level, posBlock)) {
         if (this.simulatedcoasters$connectedAnchorItem.shouldCauseBlockBreakReset(this.minecraft.player.getMainHandItem())) {
            this.simulatedcoasters$abortConnectedAnchorMiningClient();
            cir.setReturnValue(true);
            cir.cancel();
         } else {
            BlockState blockstate = this.minecraft.level.getBlockState(posBlock);
            if (blockstate.isAir()) {
               this.simulatedcoasters$abortConnectedAnchorMiningClient();
               cir.setReturnValue(false);
               cir.cancel();
            } else {
               float digIncrement = this.simulatedcoasters$survivalCurveDigIncrement(blockstate, posBlock);
               this.simulatedcoasters$connectedAnchorProgress += digIncrement;
               if (this.simulatedcoasters$connectedAnchorTicks % 4.0F == 0.0F) {
                  SoundType soundtype = blockstate.getSoundType(this.minecraft.level, posBlock, this.minecraft.player);
                  this.minecraft
                     .getSoundManager()
                     .play(
                        new SimpleSoundInstance(
                           soundtype.getHitSound(),
                           SoundSource.BLOCKS,
                           (soundtype.getVolume() + 1.0F) / 8.0F,
                           soundtype.getPitch() * 0.5F,
                           SoundInstance.createUnseededRandom(),
                           posBlock
                        )
                     );
               }

               this.simulatedcoasters$connectedAnchorTicks++;
               this.minecraft
                  .getTutorial()
                  .onDestroyBlock(this.minecraft.level, posBlock, blockstate, Mth.clamp(this.simulatedcoasters$connectedAnchorProgress, 0.0F, 1.0F));
               if (CommonHooks.onClientMineHold(this.minecraft.player, posBlock, directionFacing).getUseItem().isFalse()) {
                  cir.setReturnValue(true);
                  cir.cancel();
               } else if (this.simulatedcoasters$connectedAnchorProgress >= 1.0F) {
                  BlockPos donePos = posBlock.immutable();
                  this.simulatedcoasters$abortConnectedAnchorMiningClient();
                  MultiPlayerGameMode self = (MultiPlayerGameMode)(Object)this;
                  ClientLevel clientLevel = this.minecraft.level;
                  boolean creative = self.getPlayerMode().isCreative();
                  this.simulatedcoasters$invokeStartPrediction(clientLevel, sequence -> {
                     self.destroyBlock(donePos);
                     Action packetAction = creative ? Action.START_DESTROY_BLOCK : Action.STOP_DESTROY_BLOCK;
                     return new ServerboundPlayerActionPacket(packetAction, donePos, directionFacing, sequence);
                  });
                  this.destroyDelay = 5;
                  cir.setReturnValue(true);
                  cir.cancel();
               } else {
                  int stage = this.simulatedcoasters$connectedAnchorProgress > 0.0F ? (int)(this.simulatedcoasters$connectedAnchorProgress * 10.0F) : -1;
                  this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), posBlock, stage);
                  cir.setReturnValue(true);
                  cir.cancel();
               }
            }
         }
      } else {
         this.simulatedcoasters$abortConnectedAnchorMiningClient();
         Optional<AnchorPeerCurveMineTarget> newCurve = resolvePendingCurveDestroy(posBlock);
         if (newCurve.isPresent()) {
            this.simulatedcoasters$beginCurveMiningSession(newCurve.get(), posBlock);
            BlockState st = this.minecraft.level.getBlockState(posBlock);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, st, 0.0F);
            setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), newCurve.get(), -1);
            this.simulatedcoasters$continueCurveMineInner(posBlock, directionFacing, cir);
         }
      }
   }

   @Inject(
      method = {"destroyBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$destroyHoveredAnchorPeerCurve(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      Optional<AnchorPeerCurveMineTarget> at = resolvePendingCurveDestroy(pos);
      if (this.simulatedcoasters$miningCurve
         && this.simulatedcoasters$pendingCurveDestroy != null
         && at.isPresent()
         && sameAnchorPeerEdge(at.get(), this.simulatedcoasters$pendingCurveDestroy)) {
         cir.setReturnValue(false);
         cir.cancel();
      } else if (this.simulatedcoasters$miningConnectedAnchor && pos.equals(this.simulatedcoasters$connectedAnchorMiningPos)) {
         cir.setReturnValue(false);
         cir.cancel();
      } else {
         if (this.tryInstantCurveDestroyOnBlock(pos)) {
            cir.setReturnValue(true);
            cir.cancel();
         }
      }
   }

   private void simulatedcoasters$abortCurveMiningClient() {
      if (this.simulatedcoasters$miningCurve) {
         AnchorPeerCurveMineTarget pending = this.simulatedcoasters$pendingCurveDestroy;
         BlockPos loneMiningPos = this.simulatedcoasters$curveMiningPos;
         this.simulatedcoasters$miningCurve = false;
         this.simulatedcoasters$curveMiningPos = null;
         this.simulatedcoasters$pendingCurveDestroy = null;
         this.simulatedcoasters$curveMineProgress = 0.0F;
         this.simulatedcoasters$curveMineTicks = 0.0F;
         this.simulatedcoasters$curveMineItem = ItemStack.EMPTY;
         if (this.minecraft.player != null && this.minecraft.level != null) {
            if (pending != null) {
               setAnchorCurveDestroyProgress(this.minecraft, this.minecraft.player.getId(), pending, -1);
            } else if (loneMiningPos != null) {
               this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), loneMiningPos, -1);
            }
         }

         AnchorCurveMiningFilter.clear();
      }
   }

   private void simulatedcoasters$abortConnectedAnchorMiningClient() {
      if (this.simulatedcoasters$miningConnectedAnchor) {
         BlockPos p = this.simulatedcoasters$connectedAnchorMiningPos;
         this.simulatedcoasters$miningConnectedAnchor = false;
         this.simulatedcoasters$connectedAnchorMiningPos = null;
         this.simulatedcoasters$connectedAnchorProgress = 0.0F;
         this.simulatedcoasters$connectedAnchorTicks = 0.0F;
         this.simulatedcoasters$connectedAnchorItem = ItemStack.EMPTY;
         if (this.minecraft.player != null && this.minecraft.level != null && p != null) {
            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), p, -1);
         }
      }
   }

   private boolean simulatedcoasters$sameAnchorPeerEdge(AnchorPeerCurveMineTarget pendingNow) {
      return this.simulatedcoasters$pendingCurveDestroy != null && sameAnchorPeerEdge(pendingNow, this.simulatedcoasters$pendingCurveDestroy);
   }

   private static boolean sameAnchorPeerEdge(AnchorPeerCurveMineTarget a, AnchorPeerCurveMineTarget b) {
      if (a.wrench() != b.wrench()) {
         return false;
      } else {
         long a0 = Math.min(a.mined().asLong(), a.peer().asLong());
         long a1 = Math.max(a.mined().asLong(), a.peer().asLong());
         long b0 = Math.min(b.mined().asLong(), b.peer().asLong());
         long b1 = Math.max(b.mined().asLong(), b.peer().asLong());
         return a0 == b0 && a1 == b1;
      }
   }

   private static Optional<AnchorPeerCurveMineTarget> resolvePendingCurveDestroy(BlockPos pos) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) {
         return Optional.empty();
      } else if (!AnchorPeerCurveHit.isActive()) {
         return Optional.empty();
      } else {
         BezierConnection bc = AnchorPeerCurveHit.curve();
         if (bc == null) {
            return Optional.empty();
         } else {
            BlockPos first = (BlockPos)bc.bePositions.getFirst();
            BlockPos second = (BlockPos)bc.bePositions.getSecond();
            BlockPos focus = AnchorPeerCurveHit.focusAnchor();
            if (focus == null) {
               return Optional.empty();
            } else {
               BlockPos raster = AnchorPeerCurveHit.rasterBlockPos();
               boolean hitRasterFake = AnchorPeerFakeTracks.rasterContains(bc, pos) || raster != null && pos.equals(raster);
               boolean hitFocusAnchor = pos.equals(focus);
               boolean miningHoveredCurve = PickInteractionGates.isDestroyInputDown(mc) && raster != null;
               if (!hitRasterFake && !hitFocusAnchor && !miningHoveredCurve) {
                  return Optional.empty();
               } else {
                  BlockPos peer = focus.equals(first) ? second : first;
                  ItemStack main = mc.player.getMainHandItem();
                  ItemStack off = mc.player.getOffhandItem();
                  boolean wrench = AllItems.WRENCH.isIn(main) || AllItems.WRENCH.isIn(off);
                  return Optional.of(new AnchorPeerCurveMineTarget(focus, peer, wrench));
               }
            }
         }
      }
   }

   private static BlockPos preferredCurveMinePos(BlockPos loc, AnchorPeerCurveMineTarget pending) {
      BlockPos raster = AnchorPeerCurveHit.rasterBlockPos();
      if (raster != null) {
         return raster;
      } else {
         return AnchorPeerCurveHit.curve() != null && AnchorPeerFakeTracks.rasterContains(AnchorPeerCurveHit.curve(), loc) ? loc : pending.mined();
      }
   }

   private static void setAnchorCurveDestroyProgress(Minecraft mc, int breakerId, AnchorPeerCurveMineTarget pending, int stage) {
      BlockPos flywheelPos = AnchorPeerCurveFlywheelPos.crumbleAnchor(mc.level, pending);
      mc.level.destroyBlockProgress(breakerId, flywheelPos, stage);
   }

   private static void sendCurveDestroyPacket(AnchorPeerCurveMineTarget data) {
      AnchorPeerCurvePendingRemoval.mark(data.mined(), data.peer());
      PacketDistributor.sendToServer(new AnchorPeerCurveDestroyPayload(data.mined(), data.peer(), data.wrench()), new CustomPacketPayload[0]);
      AnchorPeerCurveHit.clear();
      TrackBlockOutline.result = null;
   }

   private boolean tryInstantCurveDestroyOnBlock(BlockPos pos) {
      Optional<AnchorPeerCurveMineTarget> pending = resolvePendingCurveDestroy(pos);
      if (pending.isEmpty()) {
         return false;
      } else {
         BlockState st = this.minecraft.level.getBlockState(pos);
         if (st.getBlock() instanceof CoasterAnchorpointBlock) {
            return false;
         } else {
            GameType mode = ((MultiPlayerGameMode)(Object)this).getPlayerMode();
            if (mode.isCreative()) {
               return false;
            } else {
               AnchorPeerCurveMineTarget target = pending.get();
               BlockState digState = this.simulatedcoasters$curveDigReferenceState(target);
               BlockPos digPos = target.mined();
               if (!st.isAir() && !(digState.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), digPos) < 1.0F)) {
                  sendCurveDestroyPacket(target);
                  return true;
               } else {
                  return false;
               }
            }
         }
      }
   }

   private BlockState simulatedcoasters$curveDigReferenceState(@Nullable AnchorPeerCurveMineTarget pending) {
      return pending == null
         ? Blocks.AIR.defaultBlockState()
         : ((CoasterTrackMaterialBlock)SimulatedCoastersBlocks.COASTER_TRACK_MATERIAL.get()).defaultBlockState();
   }

   private float simulatedcoasters$survivalCurveDigIncrement(BlockState digState, BlockPos digPos) {
      GameType playerMode = ((MultiPlayerGameMode)(Object)this).getPlayerMode();
      return playerMode.isCreative()
         ? 0.125F
         : digState.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), digPos)
            * CoasterTrackGauge.survivalCurveDigScale(this.minecraft.player);
   }
}
