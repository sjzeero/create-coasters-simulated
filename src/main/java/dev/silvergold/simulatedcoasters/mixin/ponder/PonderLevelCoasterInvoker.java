package dev.silvergold.simulatedcoasters.mixin.ponder;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({PonderLevel.class})
public interface PonderLevelCoasterInvoker {
   @Invoker("onBEAdded")
   void simulatedcoasters$onBEAdded(BlockEntity var1, BlockPos var2);
}
