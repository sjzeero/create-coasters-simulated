package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.silvergold.simulatedcoasters.track.CoasterCurveTintColors;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterCurveDyeOutlineClient {
   public static final float DYE_PREVIEW_OUTLINE_ALPHA = 0.85F;

   private CoasterCurveDyeOutlineClient() {
   }

   @Nullable
   public static BezierConnection resolvePreviewHostedPrimary(Minecraft mc) {
      if (mc.level == null) {
         return null;
      } else {
         BezierConnection raw;
         if (AnchorPeerCurveHit.isActive()) {
            raw = AnchorPeerCurveHit.curve();
         } else {
            BezierPointSelection sel = TrackBlockOutline.result;
            if (sel == null) {
               return null;
            }

            raw = TrackOutlineBezierAccess.primaryBezierForSelection(sel);
         }

         return AnchorPeerCurveOutlineSupport.resolveHostedPrimary(mc.level, raw);
      }
   }

   @Nullable
   public static Vec3 resolvePreviewWorldHit(Minecraft mc) {
      if (mc.level == null) {
         return null;
      } else if (AnchorPeerCurveHit.isActive()) {
         if (mc.hitResult != null) {
            return mc.hitResult.getLocation();
         } else {
            BlockPos focus = AnchorPeerCurveHit.focusAnchor();
            Vec3 plotLocal = AnchorPeerCurveHit.vec();
            if (focus != null && plotLocal != null) {
               float pt = AnimationTickHolder.getPartialTicks(mc.level);
               return plotLocalMidToWorld(mc, plotLocal, focus, pt);
            } else {
               return null;
            }
         }
      } else {
         BezierPointSelection sel = TrackBlockOutline.result;
         return sel == null ? null : sel.vec();
      }
   }

   private static Vec3 plotLocalMidToWorld(Minecraft mc, Vec3 plotLocalMid, BlockPos anchorPos, float pt) {
      ClientSubLevel sub = (ClientSubLevel)Sable.HELPER.getContaining(mc.level, anchorPos);
      return sub == null ? plotLocalMid : ((Pose3d)sub.renderPose(pt)).transformPosition(plotLocalMid);
   }

   @Nullable
   public static BlockPos resolvePreviewSublevelAnchor(Minecraft mc, BezierConnection bc) {
      return AnchorPeerCurveHit.isActive() ? AnchorPeerCurveHit.focusAnchor() : CoasterCurveSelectionOutlineRenderer.sublevelHostForCurve(mc.level, bc);
   }

   @Nullable
   public static Integer heldDyeDiffuseRgb(@Nullable Player player) {
      DyeItem dye = heldDyeItem(player);
      return dye == null ? null : CoasterCurveTintColors.diffuseRgbForDyePreviewOutline(dye.getDyeColor());
   }

   @Nullable
   public static Integer heldDyeDiffuseRgb(Minecraft mc) {
      return mc.player == null ? null : heldDyeDiffuseRgb(mc.player);
   }

   @Nullable
   public static InteractionHand handWithDye(Player player) {
      if (player.getMainHandItem().getItem() instanceof DyeItem) {
         return InteractionHand.MAIN_HAND;
      } else {
         return player.getOffhandItem().getItem() instanceof DyeItem ? InteractionHand.OFF_HAND : null;
      }
   }

   @Nullable
   public static DyeItem heldDyeItem(@Nullable Player player) {
      if (player == null) {
         return null;
      } else {
         ItemStack main = player.getMainHandItem();
         Item dye = main.getItem();
         if (dye instanceof DyeItem) {
            return (DyeItem)dye;
         } else {
            ItemStack off = player.getOffhandItem();
            Item var4 = off.getItem();
            return var4 instanceof DyeItem ? (DyeItem)var4 : null;
         }
      }
   }
}
