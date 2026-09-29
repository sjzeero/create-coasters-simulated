package dev.silvergold.simulatedcoasters.client.item;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllKeys;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.SimulatedCoastersSoundEvents;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.client.track.OrientedBoxOutlineRenderer;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacePayload;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class RivetPlacementOutlineClient {
   private static final String OUTLINER_KEY = "rivet_placement_preview";
   private static final int OUTLINE_COLOR = 10411635;
   private static final int UNSUPPORTED_OUTLINE_COLOR = 16740721;
   private static final float RIVET_ROTATE_SOUND_VOLUME = 0.85F;
   private static final float RIVET_PLACE_SOUND_VOLUME = 0.5F;
   private static final float RIVET_SOUND_PITCH_SPREAD = 0.1F;
   private static final float FINE_TUNE_ENTER_PLACE_PITCH = 0.85F;
   private static final long ROTATE_HOLD_FINE_MS = 500L;
   private static final double FINE_ROTATION_YAW_TO_DEGREES = 0.15;
   private static long lastInsufficientSupportTick = -1L;
   private static int previewYRotationDegrees = 0;
   private static double previewYRotationDegreesExact = 0.0;
   private static long rotatePressStartedMs = -1L;
   private static boolean rotateHoldPromotedToFine;
   private static boolean rotateWasDown;
   private static boolean fineRotationActive;

   private RivetPlacementOutlineClient() {
   }

   static int tabPreviewYRotationDegrees() {
      return previewYRotationDegrees;
   }

   static int displayPreviewYRotationDegrees(Minecraft mc) {
      if (mc.player != null && mc.level != null) {
         BlockHitResult bhr = RivetPlacement.findPlacementHit(mc.level, mc.player);
         if (bhr == null) {
            return previewYRotationDegrees;
         } else {
            boolean freePosition = AllKeys.ctrlDown();
            Vec3 look = mc.player.getViewVector(1.0F);
            return RivetPlacement.effectiveYRotationDegrees(mc.level, bhr, freePosition, previewYRotationDegrees, look);
         }
      } else {
         return previewYRotationDegrees;
      }
   }

   static boolean isFineRotationActive() {
      return fineRotationActive;
   }

   public static boolean onFineRotationMouseMove(double yawDelta, double pitchDelta) {
      if (!fineRotationActive) {
         return false;
      } else {
         if (Math.abs(yawDelta) > 1.0E-12) {
            previewYRotationDegreesExact += yawDelta * 0.15;
            previewYRotationDegrees = RivetPlacement.wrapYRotationDegrees((int)Math.round(previewYRotationDegreesExact));
         }

         return true;
      }
   }

   public static void clientTick(Minecraft mc) {
      Outliner outliner = Outliner.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui && holdingRivet(mc)) {
         if (mc.screen != null) {
            resetRotateState(mc.player);
         } else {
            tickRotateHoldAndFineRotation(mc);
         }

         BlockHitResult bhr = RivetPlacement.findPlacementHit(mc.level, mc.player);
         if (bhr == null) {
            clearPreview(outliner);
         } else if (isItemHatch(mc.level, bhr.getBlockPos())) {
            clearPreview(outliner);
         } else if (RivetPlacement.looksAtExistingRivet(mc.level, mc.player)) {
            clearPreview(outliner);
         } else {
            boolean freePosition = AllKeys.ctrlDown();
            Vec3 look = mc.player.getViewVector(1.0F);
            int yRotation = RivetPlacement.effectiveYRotationDegrees(mc.level, bhr, freePosition, previewYRotationDegrees, look);
            if (RivetPlacement.crosshairTargetsExistingRivet(mc.level, bhr, freePosition, previewYRotationDegrees, look)) {
               clearPreview(outliner);
            } else {
               RivetPlacement.Resolved placement = RivetPlacement.resolve(bhr, freePosition).withYRotation(yRotation);
               boolean supported = RivetPlacement.isValidPlacement(mc.level, placement);
               int outlineColor = supported ? 10411635 : 16740721;
               RivetPlacement.PreviewOutline outline = RivetPlacement.previewOutline(placement);
               OrientedBoxOutlineRenderer.show(
                  outliner,
                  "rivet_placement_preview",
                  outline.center(),
                  outline.axisRight(),
                  outline.axisUp(),
                  outline.axisForward(),
                  outline.halfRight(),
                  outline.halfUp(),
                  outline.halfForward(),
                  outlineColor
               );
            }
         }
      } else {
         resetRotateState(mc.player);
         clearPreview(outliner);
         previewYRotationDegrees = 0;
         previewYRotationDegreesExact = 0.0;
      }
   }

   public static boolean onUseItemKey(InteractionKeyMappingTriggered event) {
      if (!event.isUseItem()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            if (!mc.player.getItemInHand(event.getHand()).is((Item)SimulatedCoasters.RIVET.get())) {
               return false;
            } else if (RivetPlacement.looksAtExistingRivet(mc.level, mc.player)) {
               return false;
            } else {
               BlockHitResult bhr = RivetPlacement.findPlacementHit(mc.level, mc.player);
               if (bhr != null && isItemHatch(mc.level, bhr.getBlockPos())) {
                  return false;
               } else {
                  RivetPlacePayload payload = buildPayload(mc, event.getHand());
                  if (payload == null) {
                     return false;
                  } else if (payload == RivetPlacePayload.INVALID) {
                     rejectInvalid(mc);
                     event.setCanceled(true);
                     event.setSwingHand(false);
                     return true;
                  } else {
                     PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
                     playPlaceSound(mc, payload);
                     event.setCanceled(true);
                     event.setSwingHand(true);
                     return true;
                  }
               }
            }
         } else {
            return false;
         }
      }
   }

   public static void onRightClickBlock(RightClickBlock event) {
      if (event.getLevel().isClientSide()) {
         if (event.getItemStack().is((Item)SimulatedCoasters.RIVET.get())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
               if (!RivetPlacement.looksAtExistingRivet(mc.level, mc.player)) {
                  if (!isItemHatch(event.getLevel(), event.getPos())) {
                     RivetPlacePayload payload = buildPayload(mc, event.getHand());
                     if (payload != null) {
                        event.setCanceled(true);
                        if (payload == RivetPlacePayload.INVALID) {
                           rejectInvalid(mc);
                           event.setCancellationResult(InteractionResult.FAIL);
                        } else {
                           PacketDistributor.sendToServer(payload, new CustomPacketPayload[0]);
                           playPlaceSound(mc, payload);
                           event.setCancellationResult(InteractionResult.SUCCESS);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Nullable
   private static RivetPlacePayload buildPayload(Minecraft mc, InteractionHand hand) {
      if (RivetPlacement.looksAtExistingRivet(mc.level, mc.player)) {
         return null;
      } else {
         BlockHitResult bhr = RivetPlacement.findPlacementHit(mc.level, mc.player);
         if (bhr == null) {
            return null;
         } else {
            boolean freePosition = AllKeys.ctrlDown();
            Vec3 look = mc.player.getViewVector(1.0F);
            int yRotation = RivetPlacement.effectiveYRotationDegrees(mc.level, bhr, freePosition, previewYRotationDegrees, look);
            RivetPlacement.Resolved placement = RivetPlacement.resolve(bhr, freePosition).withYRotation(yRotation);
            return !RivetPlacement.isValidPlacement(mc.level, placement)
               ? RivetPlacePayload.INVALID
               : new RivetPlacePayload(placement.host(), placement.face(), bhr.getLocation(), freePosition, placement.yRotationDegrees(), hand);
         }
      }
   }

   private static void tickRotateHoldAndFineRotation(Minecraft mc) {
      LocalPlayer player = mc.player;
      if (player != null) {
         boolean rotateDown = AllKeys.altDown();
         long now = System.currentTimeMillis();
         if (rotateDown && !rotateWasDown) {
            rotatePressStartedMs = now;
            rotateHoldPromotedToFine = false;
         }

         if (!rotateDown && rotateWasDown) {
            if (!rotateHoldPromotedToFine && rotatePressStartedMs >= 0L && now - rotatePressStartedMs < 500L) {
               previewYRotationDegrees = RivetPlacement.cycleYRotationDegrees(previewYRotationDegrees);
               previewYRotationDegreesExact = (double)previewYRotationDegrees;
               playRotateSound(mc);
            }

            resetRotateState(player);
         }

         rotateWasDown = rotateDown;
         if (rotateDown) {
            if (!fineRotationActive && rotatePressStartedMs >= 0L && now - rotatePressStartedMs >= 500L) {
               enterFineRotation(mc);
               rotateHoldPromotedToFine = true;
            }
         }
      }
   }

   private static void enterFineRotation(Minecraft mc) {
      if (mc.player != null) {
         fineRotationActive = true;
         previewYRotationDegreesExact = (double)previewYRotationDegrees;
         playFineTuneEnterSound(mc);
      }
   }

   private static void exitFineRotation() {
      if (fineRotationActive) {
         previewYRotationDegrees = RivetPlacement.wrapYRotationDegrees((int)Math.round(previewYRotationDegreesExact));
      }

      fineRotationActive = false;
   }

   private static void resetRotateState(@Nullable LocalPlayer player) {
      rotatePressStartedMs = -1L;
      rotateHoldPromotedToFine = false;
      rotateWasDown = false;
      exitFineRotation();
   }

   private static void clearPreview(Outliner outliner) {
      OrientedBoxOutlineRenderer.clear(outliner, "rivet_placement_preview");
   }

   private static void playRotateSound(Minecraft mc) {
      if (mc.level != null) {
         RandomSource random = mc.level.getRandom();
         float pitch = 1.0F + (random.nextFloat() - random.nextFloat()) * 0.1F;
         BezierHandleClientSounds.playCreateUi((SoundEvent)SimulatedCoastersSoundEvents.RIVET_ROTATE.get(), 0.85F, pitch);
      }
   }

   private static void playFineTuneEnterSound(Minecraft mc) {
      if (mc.level != null && mc.player != null) {
         RandomSource random = mc.level.getRandom();
         float pitch = 0.85F + (random.nextFloat() - random.nextFloat()) * 0.1F;
         Vec3 pos = mc.player.getEyePosition();
         mc.getSoundManager()
            .play(
               new SimpleSoundInstance((SoundEvent)SimulatedCoastersSoundEvents.RIVET_PLACE.get(), SoundSource.BLOCKS, 0.5F, pitch, random, pos.x, pos.y, pos.z)
            );
      }
   }

   private static void playPlaceSound(Minecraft mc, RivetPlacePayload payload) {
      if (mc.level != null) {
         BlockHitResult hit = new BlockHitResult(payload.hitLocation(), payload.face(), payload.host(), false);
         RivetPlacement.Resolved placement = RivetPlacement.resolve(hit, payload.freePosition()).withYRotation(payload.yRotationDegrees());
         Vec3 pos = RivetPlacement.faceAnchorWorld(mc.level, placement);
         RandomSource random = mc.level.getRandom();
         float pitch = 1.0F + (random.nextFloat() - random.nextFloat()) * 0.1F;
         mc.getSoundManager()
            .play(
               new SimpleSoundInstance((SoundEvent)SimulatedCoastersSoundEvents.RIVET_PLACE.get(), SoundSource.BLOCKS, 0.5F, pitch, random, pos.x, pos.y, pos.z)
            );
      }
   }

   private static void rejectInvalid(Minecraft mc) {
      long tick = mc.level.getGameTime();
      if (tick != lastInsufficientSupportTick) {
         lastInsufficientSupportTick = tick;
         BezierHandleClientSounds.playCreateDeny();
         mc.player.displayClientMessage(Component.translatable("simulatedcoasters.rivet.insufficient_support").withStyle(ChatFormatting.RED), true);
      }
   }

   private static boolean isItemHatch(Level level, BlockPos pos) {
      return AllBlocks.ITEM_HATCH.has(level.getBlockState(pos));
   }

   private static boolean holdingRivet(Minecraft mc) {
      Item rivet = (Item)SimulatedCoasters.RIVET.get();
      return mc.player.getMainHandItem().is(rivet) || mc.player.getOffhandItem().is(rivet);
   }
}
