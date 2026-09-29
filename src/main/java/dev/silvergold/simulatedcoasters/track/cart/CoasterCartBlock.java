package dev.silvergold.simulatedcoasters.track.cart;

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoasterCartBlock extends Block implements BlockSubLevelCollisionShape {
   public static final MapCodec<CoasterCartBlock> CODEC = simpleCodec(CoasterCartBlock::new);
   public static final BooleanProperty SNAP_ENGAGED = BooleanProperty.create("snap_engaged");
   private static final VoxelShape SABLE_SNAP_ENGAGED_SHAPE = Shapes.box(0.0, 0.5, 0.0, 1.0, 1.0, 1.0);
   private static final Map<ServerSubLevel, Boolean> LAST_SNAP_ENGAGED_SYNCED = Collections.synchronizedMap(new WeakHashMap<>());

   public CoasterCartBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(SNAP_ENGAGED, false));
   }

   public static Properties defaultProperties() {
      return Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion();
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{SNAP_ENGAGED});
   }

   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return new ItemStack((ItemLike)SimulatedCoasters.COASTER_CART.get());
   }

   public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
      return state.getValue(SNAP_ENGAGED) ? SABLE_SNAP_ENGAGED_SHAPE : state.getCollisionShape(blockGetter, BlockPos.ZERO, SableCollisionContext.get());
   }

   public static void syncPlotSnapEngaged(ServerSubLevel sub, boolean engaged) {
      Boolean prev = LAST_SNAP_ENGAGED_SYNCED.get(sub);
      if (prev == null || prev != engaged) {
         LAST_SNAP_ENGAGED_SYNCED.put(sub, engaged);
         LevelPlot plot = sub.getPlot();
         EmbeddedPlotLevelAccessor embedded = plot.getEmbeddedLevelAccessor();
         BlockPos plotOrigin = plot.getCenterBlock();
         CoasterCartBlock bearingBlock = (CoasterCartBlock)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
         Level level = sub.getLevel();
         CoasterCartPlotScan.forEachBearingWorldCell(plot, bearingBlock, mut -> {
            BlockState cur = level.getBlockState(mut);
            if (cur.is(bearingBlock) && (Boolean)cur.getValue(SNAP_ENGAGED) != engaged) {
               BlockPos local = mut.immutable().subtract(plotOrigin);
               embedded.setBlock(local, (BlockState)cur.setValue(SNAP_ENGAGED, engaged), 3);
            }
         });
      }
   }
}
