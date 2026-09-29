/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  com.simibubi.create.content.equipment.wrench.IWrenchable
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.DirectionProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.EntityCollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.rivet;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetRemoval;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RivetBlock
extends BaseEntityBlock
implements BlockSubLevelCollisionShape,
IWrenchable {
    public static final MapCodec<RivetBlock> CODEC = RivetBlock.simpleCodec(RivetBlock::new);
    public static final DirectionProperty FACING = DirectionProperty.create((String)"facing", (Direction[])Direction.values());
    public static final double HALF_PLANE_BLOCKS = 0.3125;
    public static final double DEPTH_BLOCKS = 0.125;
    private static final double HALF_DEPTH_BLOCKS = 0.0625;
    private static final double CENTER = 0.5;
    private static final VoxelShape SHAPE_UP = RivetBlock.plate(Direction.UP);
    private static final VoxelShape SHAPE_DOWN = RivetBlock.plate(Direction.DOWN);
    private static final VoxelShape SHAPE_NORTH = RivetBlock.plate(Direction.NORTH);
    private static final VoxelShape SHAPE_SOUTH = RivetBlock.plate(Direction.SOUTH);
    private static final VoxelShape SHAPE_WEST = RivetBlock.plate(Direction.WEST);
    private static final VoxelShape SHAPE_EAST = RivetBlock.plate(Direction.EAST);

    public RivetBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)FACING, (Comparable)Direction.UP));
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion();
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
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
        if (!state.is(newState.getBlock()) && !level.isClientSide() && level instanceof ServerLevel) {
            ServerSubLevel ssl;
            ServerLevel serverLevel = (ServerLevel)level;
            SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)pos);
            if (sub instanceof ServerSubLevel && !(ssl = (ServerSubLevel)sub).isRemoved() && ssl.getPlot().getCenterBlock().equals((Object)pos) && !RivetRemoval.isRemoving(ssl.getUniqueId())) {
                RivetRemoval.onRivetBlockRemoved(serverLevel, ssl);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return RivetBlock.shapeForFacing((Direction)state.getValue((Property)FACING));
    }

    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        EntityCollisionContext entityContext;
        if (context instanceof EntityCollisionContext && (entityContext = (EntityCollisionContext)context).getEntity() instanceof Player) {
            return Shapes.empty();
        }
        return RivetBlock.shapeForFacing((Direction)state.getValue((Property)FACING));
    }

    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
        return Shapes.empty();
    }

    protected BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rotation.rotate((Direction)state.getValue((Property)FACING)));
    }

    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation((Direction)state.getValue((Property)FACING)));
    }

    public static VoxelShape shapeForFacing(Direction face) {
        return switch (face) {
            default -> throw new MatchException(null, null);
            case Direction.UP -> SHAPE_UP;
            case Direction.DOWN -> SHAPE_DOWN;
            case Direction.NORTH -> SHAPE_NORTH;
            case Direction.SOUTH -> SHAPE_SOUTH;
            case Direction.WEST -> SHAPE_WEST;
            case Direction.EAST -> SHAPE_EAST;
        };
    }

    private static VoxelShape plate(Direction face) {
        double h = 0.3125;
        double hd = 0.0625;
        double c = 0.5;
        return switch (face) {
            default -> throw new MatchException(null, null);
            case Direction.UP -> Shapes.box((double)(c - h), (double)(1.0 - hd), (double)(c - h), (double)(c + h), (double)(1.0 + hd), (double)(c + h));
            case Direction.DOWN -> Shapes.box((double)(c - h), (double)(-hd), (double)(c - h), (double)(c + h), (double)hd, (double)(c + h));
            case Direction.NORTH -> Shapes.box((double)(c - h), (double)(c - h), (double)(-hd), (double)(c + h), (double)(c + h), (double)hd);
            case Direction.SOUTH -> Shapes.box((double)(c - h), (double)(c - h), (double)(1.0 - hd), (double)(c + h), (double)(c + h), (double)(1.0 + hd));
            case Direction.WEST -> Shapes.box((double)(-hd), (double)(c - h), (double)(c - h), (double)hd, (double)(c + h), (double)(c + h));
            case Direction.EAST -> Shapes.box((double)(1.0 - hd), (double)(c - h), (double)(c - h), (double)(1.0 + hd), (double)(c + h), (double)(c + h));
        };
    }
}

