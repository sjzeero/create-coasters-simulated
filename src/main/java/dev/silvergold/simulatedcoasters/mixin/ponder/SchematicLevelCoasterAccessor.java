package dev.silvergold.simulatedcoasters.mixin.ponder;

import java.util.List;
import java.util.Map;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({SchematicLevel.class})
public interface SchematicLevelCoasterAccessor {
   @Accessor("blockEntities")
   Map<BlockPos, BlockEntity> simulatedcoasters$blockEntities();

   @Accessor("renderedBlockEntities")
   List<BlockEntity> simulatedcoasters$renderedBlockEntities();

   @Invoker("onBEadded")
   void simulatedcoasters$onBEadded(BlockEntity var1, BlockPos var2);
}
