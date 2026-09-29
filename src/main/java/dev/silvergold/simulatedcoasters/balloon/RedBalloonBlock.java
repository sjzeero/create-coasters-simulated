package dev.silvergold.simulatedcoasters.balloon;

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RedBalloonBlock extends BaseEntityBlock implements BlockSubLevelCollisionShape {
   public static final MapCodec<RedBalloonBlock> CODEC = simpleCodec(RedBalloonBlock::new);
   private static final double INSET_BLOCKS = 0.0625;
   private static final double OUTSET_BLOCKS = 0.9375;
   private static final VoxelShape SHAPE = Shapes.box(0.0625, 0.0625, 0.0625, 0.9375, 0.9375, 0.9375);

   public RedBalloonBlock(Properties properties) {
      super(properties);
   }

   public static Properties defaultProperties() {
      return Properties.of().mapColor(MapColor.COLOR_RED).strength(0.2F).sound(SoundType.WOOL).noOcclusion().noLootTable();
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return null;
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return new ItemStack((ItemLike)SimulatedCoasters.RED_BALLOON.get());
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new BalloonBlockEntity(pos, state);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.is(newState.getBlock())
         && !level.isClientSide()
         && level instanceof ServerLevel serverLevel
         && Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel ssl
         && !ssl.isRemoved()
         && !BalloonRemoval.isRemoving(ssl.getUniqueId())) {
         BalloonRemoval.onBalloonBlockRemoved(serverLevel, ssl);
      }

      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
      return SHAPE;
   }
}
