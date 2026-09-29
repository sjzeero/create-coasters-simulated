/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.createmod.ponder.api.level.PonderLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package dev.silvergold.simulatedcoasters.mixin.ponder;

import java.util.Map;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={PonderLevel.class})
public interface PonderLevelCoasterAccessor {
    @Accessor(value="originalBlockEntities")
    public Map<BlockPos, CompoundTag> simulatedcoasters$originalBlockEntities();
}

