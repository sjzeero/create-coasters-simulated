package dev.silvergold.simulatedcoasters.client.cart;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleEditMode;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkDistanceEditPayload;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.UUID;
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
public final class CoasterCartEditMode {
   @Nullable
   private static UUID activeCartId;

   private CoasterCartEditMode() {
   }

   public static boolean isActive() {
      return activeCartId != null;
   }

   @Nullable
   public static UUID getActiveCartId() {
      return activeCartId;
   }

   public static void clear() {
      activeCartId = null;
   }

   public static boolean tryActivateFromInteract(Level level, Player player, BlockPos clickedPos) {
      if (!level.isClientSide()) {
         return false;
      } else if (!isHoldingWrench(player)) {
         return false;
      } else {
         SubLevel cart = CoasterCartResolve.findCartSubLevelAt(level, clickedPos);
         if (cart != null && !cart.isRemoved()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != player) {
               return false;
            } else if (!isWithinReach(player, level, cart)) {
               return false;
            } else if (activeCartId != null && activeCartId.equals(cart.getUniqueId())) {
               return tryAdjustLinksFromInteract(player, cart);
            } else {
               BezierHandleEditMode.clear();
               activeCartId = cart.getUniqueId();
               playActivateFeedback(player);
               return true;
            }
         } else {
            return false;
         }
      }
   }

   private static boolean tryAdjustLinksFromInteract(Player player, SubLevel cart) {
      PacketDistributor.sendToServer(new CoasterCartLinkDistanceEditPayload(cart.getUniqueId(), player.isShiftKeyDown()), new CustomPacketPayload[0]);
      playActivateFeedback(player);
      return true;
   }

   public static void clientTick(Minecraft mc) {
      if (activeCartId != null) {
         if (mc.player == null || mc.level == null || mc.screen != null) {
            clear();
         } else if (!isHoldingWrench(mc.player)) {
            clear();
         } else {
            SubLevel cart = CoasterCartResolve.findCartById(mc.level, activeCartId);
            if (cart != null && !cart.isRemoved()) {
               if (!isWithinReach(mc.player, mc.level, cart)) {
                  clear();
               }
            } else {
               clear();
            }
         }
      }
   }

   private static boolean isHoldingWrench(Player player) {
      return AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem());
   }

   private static boolean isWithinReach(Player player, Level level, SubLevel cart) {
      float pt = AnimationTickHolder.getPartialTicks(level);
      Vec3 eye = player.getEyePosition(pt);
      double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      Vec3 center = CoasterCartLinkPlacement.partnerBearingCenterWorld(cart, (double)pt);
      return center == null ? false : eye.distanceToSqr(center) <= reach * reach;
   }

   private static void playActivateFeedback(Player player) {
      InteractionHand hand = AllItems.WRENCH.isIn(player.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
      player.swing(hand);
      float pitch = 1.0F + player.level().random.nextFloat() * 0.08F;
      BezierHandleClientSounds.playCreateUi(AllSoundEvents.WRENCH_ROTATE.getMainEvent(), 0.9F, pitch);
   }
}
