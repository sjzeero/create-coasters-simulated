package dev.silvergold.simulatedcoasters.mixin.ponder;

import java.util.Map;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PonderLevel.class})
public interface PonderLevelCoasterAccessor {
   @Accessor("originalBlockEntities")
   Map<BlockPos, CompoundTag> simulatedcoasters$originalBlockEntities();
}
