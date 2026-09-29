package dev.silvergold.simulatedcoasters.rivet;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RivetBlock extends BaseEntityBlock implements BlockSubLevelCollisionShape, IWrenchable {
   public static final MapCodec<RivetBlock> CODEC = simpleCodec(RivetBlock::new);
   public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());
   public static final double HALF_PLANE_BLOCKS = 0.3125;
   public static final double DEPTH_BLOCKS = 0.125;
   private static final double HALF_DEPTH_BLOCKS = 0.0625;
   private static final double CENTER = 0.5;
   private static final VoxelShape SHAPE_UP = plate(Direction.UP);
   private static final VoxelShape SHAPE_DOWN = plate(Direction.DOWN);
   private static final VoxelShape SHAPE_NORTH = plate(Direction.NORTH);
   private static final VoxelShape SHAPE_SOUTH = plate(Direction.SOUTH);
   private static final VoxelShape SHAPE_WEST = plate(Direction.WEST);
   private static final VoxelShape SHAPE_EAST = plate(Direction.EAST);

   public RivetBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.UP));
   }

   public static Properties defaultProperties() {
      return Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion();
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return null;
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      return new ItemStack((ItemLike)SimulatedCoasters.RIVET.get());
   }

   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new RivetBlockEntity(pos, state);
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      return InteractionResult.PASS;
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.is(newState.getBlock())
         && !level.isClientSide()
         && level instanceof ServerLevel serverLevel
         && Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel ssl
         && !ssl.isRemoved()
         && ssl.getPlot().getCenterBlock().equals(pos)
         && !RivetRemoval.isRemoving(ssl.getUniqueId())) {
         RivetRemoval.onRivetBlockRemoved(serverLevel, ssl);
      }

      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return shapeForFacing((Direction)state.getValue(FACING));
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player) {
         return Shapes.empty();
      }

      return shapeForFacing((Direction)state.getValue(FACING));
   }

   protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
      return Shapes.block();
   }

   public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
      return Shapes.empty();
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   public static VoxelShape shapeForFacing(Direction face) {
      return switch (face) {
         case UP -> SHAPE_UP;
         case DOWN -> SHAPE_DOWN;
         case NORTH -> SHAPE_NORTH;
         case SOUTH -> SHAPE_SOUTH;
         case WEST -> SHAPE_WEST;
         case EAST -> SHAPE_EAST;
         default -> throw new MatchException(null, null);
      };
   }

   private static VoxelShape plate(Direction face) {
      double h = 0.3125;
      double hd = 0.0625;
      double c = 0.5;

      return switch (face) {
         case UP -> Shapes.box(c - h, 1.0 - hd, c - h, c + h, 1.0 + hd, c + h);
         case DOWN -> Shapes.box(c - h, -hd, c - h, c + h, hd, c + h);
         case NORTH -> Shapes.box(c - h, c - h, -hd, c + h, c + h, hd);
         case SOUTH -> Shapes.box(c - h, c - h, 1.0 - hd, c + h, c + h, 1.0 + hd);
         case WEST -> Shapes.box(-hd, c - h, c - h, hd, c + h, c + h);
         case EAST -> Shapes.box(1.0 - hd, c - h, c - h, 1.0 + hd, c + h, c + h);
         default -> throw new MatchException(null, null);
      };
   }
}
