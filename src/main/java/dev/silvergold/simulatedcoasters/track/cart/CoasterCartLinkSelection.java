/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.CustomData
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 */
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
        CompoundTag tag = ((CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, (Object)CustomData.EMPTY)).copyTag();
        tag.putUUID(LINK_TARGET_CUSTOM_TAG, cartSubLevelId);
        stack.set(DataComponents.CUSTOM_DATA, (Object)CustomData.of((CompoundTag)tag));
    }

    public static void clearLinkTarget(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) {
            return;
        }
        CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
        if (!tag.contains(LINK_TARGET_CUSTOM_TAG)) {
            return;
        }
        tag.remove(LINK_TARGET_CUSTOM_TAG);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, (Object)CustomData.of((CompoundTag)tag));
        }
    }

    @Nullable
    public static UUID readLinkTarget(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) {
            return null;
        }
        CompoundTag tag = ((CustomData)stack.get(DataComponents.CUSTOM_DATA)).copyTag();
        if (!tag.hasUUID(LINK_TARGET_CUSTOM_TAG)) {
            return null;
        }
        return tag.getUUID(LINK_TARGET_CUSTOM_TAG);
    }

    public static boolean hasLinkTarget(ItemStack stack) {
        return CoasterCartLinkSelection.readLinkTarget(stack) != null;
    }

    public static void applyLinkTarget(Level level, ItemStack stack, SubLevel cart) {
        CoasterCartLinkSelection.setLinkTarget(stack, cart.getUniqueId());
        Vector3d pos = cart.logicalPose().position();
        level.playSound(null, BlockPos.containing((double)pos.x(), (double)pos.y(), (double)pos.z()), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75f, 1.0f);
    }
}

