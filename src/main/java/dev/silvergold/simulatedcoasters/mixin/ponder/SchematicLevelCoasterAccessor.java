/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.createmod.catnip.levelWrappers.SchematicLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package dev.silvergold.simulatedcoasters.mixin.ponder;

import java.util.List;
import java.util.Map;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={SchematicLevel.class})
public interface SchematicLevelCoasterAccessor {
    @Accessor(value="blockEntities")
    public Map<BlockPos, BlockEntity> simulatedcoasters$blockEntities();

    @Accessor(value="renderedBlockEntities")
    public List<BlockEntity> simulatedcoasters$renderedBlockEntities();

    @Invoker(value="onBEadded")
    public void simulatedcoasters$onBEadded(BlockEntity var1, BlockPos var2);
}

