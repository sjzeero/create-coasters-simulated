/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape
 *  dev.ryanhcode.sable.api.physics.collider.SableCollisionContext
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package dev.silvergold.simulatedcoasters.track.cart;

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoasterCartBlock
extends Block
implements BlockSubLevelCollisionShape {
    public static final MapCodec<CoasterCartBlock> CODEC = CoasterCartBlock.simpleCodec(CoasterCartBlock::new);
    public static final BooleanProperty SNAP_ENGAGED = BooleanProperty.create((String)"snap_engaged");
    private static final VoxelShape SABLE_SNAP_ENGAGED_SHAPE = Shapes.box((double)0.0, (double)0.5, (double)0.0, (double)1.0, (double)1.0, (double)1.0);
    private static final Map<ServerSubLevel, Boolean> LAST_SNAP_ENGAGED_SYNCED = Collections.synchronizedMap(new WeakHashMap());

    public CoasterCartBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)SNAP_ENGAGED, (Comparable)Boolean.valueOf(false)));
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion();
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{SNAP_ENGAGED});
    }

    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack((ItemLike)SimulatedCoasters.COASTER_CART.get());
    }

    public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
        if (((Boolean)state.getValue((Property)SNAP_ENGAGED)).booleanValue()) {
            return SABLE_SNAP_ENGAGED_SHAPE;
        }
        return state.getCollisionShape(blockGetter, BlockPos.ZERO, (CollisionContext)SableCollisionContext.get());
    }

    public static void syncPlotSnapEngaged(ServerSubLevel sub, boolean engaged) {
        Boolean prev = LAST_SNAP_ENGAGED_SYNCED.get(sub);
        if (prev != null && prev == engaged) {
            return;
        }
        LAST_SNAP_ENGAGED_SYNCED.put(sub, engaged);
        ServerLevelPlot plot = sub.getPlot();
        EmbeddedPlotLevelAccessor embedded = plot.getEmbeddedLevelAccessor();
        BlockPos plotOrigin = plot.getCenterBlock();
        CoasterCartBlock bearingBlock = (CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get());
        ServerLevel level = sub.getLevel();
        CoasterCartPlotScan.forEachBearingWorldCell((LevelPlot)plot, bearingBlock, arg_0 -> CoasterCartBlock.lambda$syncPlotSnapEngaged$0((Level)level, bearingBlock, engaged, plotOrigin, embedded, arg_0));
    }

    private static /* synthetic */ void lambda$syncPlotSnapEngaged$0(Level level, CoasterCartBlock bearingBlock, boolean engaged, BlockPos plotOrigin, EmbeddedPlotLevelAccessor embedded, BlockPos.MutableBlockPos mut) {
        BlockState cur = level.getBlockState((BlockPos)mut);
        if (!cur.is((Block)bearingBlock) || (Boolean)cur.getValue((Property)SNAP_ENGAGED) == engaged) {
            return;
        }
        BlockPos local = mut.immutable().subtract((Vec3i)plotOrigin);
        embedded.setBlock(local, (BlockState)cur.setValue((Property)SNAP_ENGAGED, (Comparable)Boolean.valueOf(engaged)), 3);
    }
}

