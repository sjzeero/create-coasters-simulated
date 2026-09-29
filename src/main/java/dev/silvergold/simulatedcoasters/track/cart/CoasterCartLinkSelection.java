package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class CoasterCartLinkSelection {
   private static final String LINK_TARGET_CUSTOM_TAG = "SimulatedCoastersCartLinkTarget";

   private CoasterCartLinkSelection() {
   }

   public static void setLinkTarget(ItemStack stack, UUID cartSubLevelId) {
      CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
      tag.putUUID("SimulatedCoastersCartLinkTarget", cartSubLevelId);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
   }

   public static void clearLinkTarget(ItemStack stack) {
      if (stack.has(DataComponents.CUSTOM_DATA)) {
         CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
         if (tag.contains("SimulatedCoastersCartLinkTarget")) {
            tag.remove("SimulatedCoastersCartLinkTarget");
            if (tag.isEmpty()) {
               stack.remove(DataComponents.CUSTOM_DATA);
            } else {
               stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            }
         }
      }
   }

   @Nullable
   public static UUID readLinkTarget(ItemStack stack) {
      if (!stack.has(DataComponents.CUSTOM_DATA)) {
         return null;
      } else {
         CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
         return !tag.hasUUID("SimulatedCoastersCartLinkTarget") ? null : tag.getUUID("SimulatedCoastersCartLinkTarget");
      }
   }

   public static boolean hasLinkTarget(ItemStack stack) {
      return readLinkTarget(stack) != null;
   }

   public static void applyLinkTarget(Level level, ItemStack stack, SubLevel cart) {
      setLinkTarget(stack, cart.getUniqueId());
      Vector3d pos = cart.logicalPose().position();
      level.playSound(null, BlockPos.containing(pos.x(), pos.y(), pos.z()), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75F, 1.0F);
   }
}
