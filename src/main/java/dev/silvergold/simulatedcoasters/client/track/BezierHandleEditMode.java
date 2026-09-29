package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartEditMode;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftRemovePayload;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftShaftCyclePayload;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanQuery;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class BezierHandleEditMode {
   @Nullable
   private static BlockPos activeAnchor;

   private BezierHandleEditMode() {
   }

   public static boolean isActive() {
      return activeAnchor != null;
   }

   @Nullable
   public static BlockPos getActiveAnchor() {
      return activeAnchor;
   }

   public static void switchActiveAnchorTo(BlockPos anchorPos, Level level) {
      if (isActive() && anchorPos != null) {
         if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity) {
            if (activeAnchor == null || !activeAnchor.equals(anchorPos)) {
               activeAnchor = anchorPos.immutable();
            }
         }
      }
   }

   public static void clear() {
      activeAnchor = null;
   }

   public static boolean tryActivateFromInteract(Level level, Player player, BlockPos anchorPos) {
      if (player.isShiftKeyDown()) {
         return tryRemoveChainLiftFromInteract(level, player, anchorPos);
      } else if (tryCycleChainLiftShaftFromInteract(level, player, anchorPos)) {
         return true;
      } else {
         tryActivate(level, player, anchorPos, true);
         return false;
      }
   }

   public static void tryActivateFromCurveInteract(Level level, Player player, BlockPos anchorPos) {
      tryActivate(level, player, anchorPos, false);
   }

   public static boolean tryRemoveChainLiftFromCurveInteract(Level level, Player player, CoasterPathTrackFrame.GraphHit hit) {
      if (level.isClientSide && hit != null) {
         if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
            return false;
         } else {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != player) {
               return false;
            } else {
               float pt = AnimationTickHolder.getPartialTicks(level);
               ChainLiftSpanQuery.ChainLiftSpanHit lift = ChainLiftSpanQuery.findAtGraphHit(level, hit, (double)pt);
               return lift == null ? false : sendChainLiftRemove(player, lift.anchor().getBlockPos(), hit.point());
            }
         }
      } else {
         return false;
      }
   }

   private static void tryActivate(Level level, Player player, BlockPos anchorPos, boolean enforceReach) {
      if (level.isClientSide && level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity) {
         if (AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == player) {
               if (enforceReach) {
                  float pt = AnimationTickHolder.getPartialTicks(level);
                  Vec3 eye = player.getEyePosition(pt);
                  double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 22.0;
                  Vec3 ac = CoasterAnchorClientSpace.anchorCenterRenderWorld(level, anchorPos);
                  if (eye.distanceToSqr(ac) > reach * reach) {
                     return;
                  }
               }

               if (activeAnchor == null || !activeAnchor.equals(anchorPos)) {
                  CoasterCartEditMode.clear();
                  activeAnchor = anchorPos.immutable();
                  BezierHandleDragManager.onEditModeActivated();
                  playWrenchActivateFeedback(player);
               }
            }
         }
      }
   }

   private static boolean tryCycleChainLiftShaftFromInteract(Level level, Player player, BlockPos anchorPos) {
      if (activeAnchor != null && activeAnchor.equals(anchorPos)) {
         if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
            if (!be.hasChainLiftShaft()) {
               return false;
            } else if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
               return false;
            } else {
               Minecraft mc = Minecraft.getInstance();
               if (mc.player != player) {
                  return false;
               } else {
                  float pt = AnimationTickHolder.getPartialTicks(level);
                  Vec3 eye = player.getEyePosition(pt);
                  double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 22.0;
                  Vec3 ac = CoasterAnchorClientSpace.anchorCenterRenderWorld(level, anchorPos, pt);
                  if (eye.distanceToSqr(ac) > reach * reach) {
                     return false;
                  } else {
                     PacketDistributor.sendToServer(new ChainLiftShaftCyclePayload(anchorPos), new CustomPacketPayload[0]);
                     BezierHandleDragManager.onEditModeActivated();
                     playWrenchActivateFeedback(player);
                     return true;
                  }
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean tryRemoveChainLiftFromInteract(Level level, Player player, BlockPos anchorPos) {
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         if (!AllItems.WRENCH.isIn(player.getMainHandItem()) && !AllItems.WRENCH.isIn(player.getOffhandItem())) {
            return false;
         } else {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != player) {
               return false;
            } else {
               float pt = AnimationTickHolder.getPartialTicks(level);
               Vec3 eye = player.getEyePosition(pt);
               double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 22.0;
               Vec3 ac = CoasterAnchorClientSpace.anchorCenterRenderWorld(level, anchorPos, pt);
               if (eye.distanceToSqr(ac) > reach * reach) {
                  return false;
               } else {
                  BlockPos hostPos = null;
                  if (be.hasChainLiftShaft()) {
                     hostPos = anchorPos;
                  } else {
                     CoasterAnchorpointBlockEntity host = findChainLiftHostCoveringAnchor(level, anchorPos, pt);
                     if (host != null) {
                        hostPos = host.getBlockPos();
                     }
                  }

                  return hostPos == null ? false : sendChainLiftRemove(player, hostPos, ac);
               }
            }
         }
      } else {
         return false;
      }
   }

   @Nullable
   private static CoasterAnchorpointBlockEntity findChainLiftHostCoveringAnchor(Level level, BlockPos anchorPos, float partialTick) {
      CoasterPathGraph graph = CoasterPathGraphManagerClient.get(level);

      for (CoasterPathTrackFrame.GraphHit junction : CoasterPathGraphSpans.anchorJunctionHits(level, graph, anchorPos, (double)partialTick)) {
         ChainLiftSpanQuery.ChainLiftSpanHit lift = ChainLiftSpanQuery.findAtGraphHit(level, junction, (double)partialTick);
         if (lift != null) {
            return lift.anchor();
         }
      }

      return null;
   }

   private static boolean sendChainLiftRemove(Player player, BlockPos hostPos, Vec3 soundWorldPos) {
      PacketDistributor.sendToServer(new ChainLiftRemovePayload(hostPos, soundWorldPos), new CustomPacketPayload[0]);
      if (isActive()) {
         BezierHandleDragManager.onEditModeActivated();
      }

      playWrenchSwing(player);
      return true;
   }

   private static void playWrenchSwing(Player player) {
      InteractionHand hand = AllItems.WRENCH.isIn(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
      player.swing(hand);
   }

   private static void playWrenchActivateFeedback(Player player) {
      playWrenchSwing(player);
      float pitch = 1.0F + player.level().random.nextFloat() * 0.08F;
      BezierHandleClientSounds.playCreateUi(AllSoundEvents.WRENCH_ROTATE.getMainEvent(), 0.9F, pitch);
   }

   public static void clientTick(Minecraft mc) {
      if (activeAnchor != null) {
         if (mc.player != null && mc.level != null && mc.screen == null) {
            if (!AllItems.WRENCH.isIn(mc.player.getMainHandItem()) && !AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
               clear();
            } else {
               if (!(mc.level.getBlockEntity(activeAnchor) instanceof CoasterAnchorpointBlockEntity)) {
                  clear();
               }
            }
         } else {
            clear();
         }
      }
   }
}
