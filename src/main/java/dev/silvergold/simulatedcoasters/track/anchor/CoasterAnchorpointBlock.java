/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  com.simibubi.create.content.decoration.girder.GirderBlock
 *  com.simibubi.create.content.equipment.wrench.IWrenchable
 *  com.simibubi.create.content.kinetics.base.IRotate
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.foundation.block.IHaveBigOutline
 *  dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener
 *  dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.DirectionProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.minecraft.world.ticks.LevelTickAccess
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.decoration.girder.GirderBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.foundation.block.IHaveBigOutline;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.silvergold.simulatedcoasters.PickInteractionGates;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorGirderConnection;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathAssemblyGraphDeferral;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTickAccess;

public class CoasterAnchorpointBlock
extends BaseEntityBlock
implements BlockSubLevelCollisionShape,
BlockSubLevelAssemblyListener,
IHaveBigOutline,
IRotate,
IWrenchable {
    public static final MapCodec<CoasterAnchorpointBlock> CODEC = CoasterAnchorpointBlock.simpleCodec(CoasterAnchorpointBlock::new);
    public static final DirectionProperty FACING = DirectionProperty.create((String)"facing", (Direction[])Direction.values());
    public static final BooleanProperty CHAIN_LIFT_SHAFT = BooleanProperty.create((String)"chain_lift_shaft");
    public static final BooleanProperty TRACK_CONNECTED = BooleanProperty.create((String)"track_connected");
    private static final VoxelShape SHAPE_UP = Shapes.box((double)0.0, (double)0.0, (double)0.0, (double)1.0, (double)0.5, (double)1.0);
    private static final VoxelShape SHAPE_DOWN = Shapes.box((double)0.0, (double)0.5, (double)0.0, (double)1.0, (double)1.0, (double)1.0);
    private static final VoxelShape SHAPE_NORTH = Shapes.box((double)0.0, (double)0.0, (double)0.5, (double)1.0, (double)1.0, (double)1.0);
    private static final VoxelShape SHAPE_SOUTH = Shapes.box((double)0.0, (double)0.0, (double)0.0, (double)1.0, (double)1.0, (double)0.5);
    private static final VoxelShape SHAPE_WEST = Shapes.box((double)0.5, (double)0.0, (double)0.0, (double)1.0, (double)1.0, (double)1.0);
    private static final VoxelShape SHAPE_EAST = Shapes.box((double)0.0, (double)0.0, (double)0.0, (double)0.5, (double)1.0, (double)1.0);
    private static final double INTERACTION_MARGIN = 0.08;
    private static final VoxelShape CURVE_INTERACTION_SHAPE = Shapes.box((double)-0.08, (double)-0.08, (double)-0.08, (double)1.08, (double)1.08, (double)1.08);

    public CoasterAnchorpointBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)FACING, (Comparable)Direction.UP)).setValue((Property)CHAIN_LIFT_SHAFT, (Comparable)Boolean.valueOf(false))).setValue((Property)TRACK_CONNECTED, (Comparable)Boolean.valueOf(false)));
    }

    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion();
    }

    public static boolean haveOppositeFacing(BlockState anchorA, BlockState anchorB) {
        Direction fb;
        if (!(anchorA.getBlock() instanceof CoasterAnchorpointBlock) || !(anchorB.getBlock() instanceof CoasterAnchorpointBlock)) {
            return false;
        }
        Direction fa = (Direction)anchorA.getValue((Property)FACING);
        return fa == (fb = (Direction)anchorB.getValue((Property)FACING)).getOpposite();
    }

    public static boolean haveSameFacingCollinearAlongFacing(BlockState anchorA, BlockState anchorB, BlockPos posA, BlockPos posB) {
        if (!(anchorA.getBlock() instanceof CoasterAnchorpointBlock) || !(anchorB.getBlock() instanceof CoasterAnchorpointBlock)) {
            return false;
        }
        Direction facing = (Direction)anchorA.getValue((Property)FACING);
        if (facing != anchorB.getValue((Property)FACING)) {
            return false;
        }
        BlockPos delta = posB.subtract((Vec3i)posA);
        if (delta.equals((Object)BlockPos.ZERO)) {
            return false;
        }
        return CoasterAnchorpointBlock.isDeltaOnAxisOnly(delta, facing.getAxis());
    }

    private static boolean isDeltaOnAxisOnly(BlockPos delta, Direction.Axis axis) {
        return switch (axis) {
            default -> throw new MatchException(null, null);
            case Direction.Axis.X -> {
                if (delta.getY() == 0 && delta.getZ() == 0) {
                    yield true;
                }
                yield false;
            }
            case Direction.Axis.Y -> {
                if (delta.getX() == 0 && delta.getZ() == 0) {
                    yield true;
                }
                yield false;
            }
            case Direction.Axis.Z -> delta.getX() == 0 && delta.getY() == 0;
        };
    }

    private static boolean peerInReferencePickyHalfSpace(BlockPos referencePos, Direction referenceFacing, BlockPos peerPos) {
        BlockPos delta = peerPos.subtract((Vec3i)referencePos);
        Vec3i n = referenceFacing.getNormal();
        return delta.getX() * n.getX() + delta.getY() * n.getY() + delta.getZ() * n.getZ() <= 0;
    }

    private static boolean peerAlignedOnOrthogonalPlaneThroughFacingAxis(BlockPos referencePos, Direction referenceFacing, BlockPos peerPos) {
        BlockPos delta = peerPos.subtract((Vec3i)referencePos);
        if (delta.equals((Object)BlockPos.ZERO)) {
            return false;
        }
        return switch (referenceFacing.getAxis()) {
            default -> throw new MatchException(null, null);
            case Direction.Axis.X -> {
                if (delta.getY() == 0 || delta.getZ() == 0) {
                    yield true;
                }
                yield false;
            }
            case Direction.Axis.Y -> {
                if (delta.getX() == 0 || delta.getZ() == 0) {
                    yield true;
                }
                yield false;
            }
            case Direction.Axis.Z -> delta.getX() == 0 || delta.getY() == 0;
        };
    }

    private static boolean facesToward(BlockPos fromPos, Direction fromFacing, BlockPos towardPos) {
        BlockPos delta = towardPos.subtract((Vec3i)fromPos);
        if (delta.equals((Object)BlockPos.ZERO)) {
            return false;
        }
        Vec3i n = fromFacing.getNormal();
        return delta.getX() * n.getX() + delta.getY() * n.getY() + delta.getZ() * n.getZ() > 0;
    }

    public static boolean connectionHasImpossibleBend(BlockState anchorA, BlockState anchorB, BlockPos posA, BlockPos posB) {
        Direction fb;
        if (!(anchorA.getBlock() instanceof CoasterAnchorpointBlock) || !(anchorB.getBlock() instanceof CoasterAnchorpointBlock)) {
            return false;
        }
        Direction fa = (Direction)anchorA.getValue((Property)FACING);
        if (fa == (fb = (Direction)anchorB.getValue((Property)FACING))) {
            return false;
        }
        return CoasterAnchorpointBlock.impossibleBendFromReference(fa, posA, fb, posB) || CoasterAnchorpointBlock.impossibleBendFromReference(fb, posB, fa, posA);
    }

    private static boolean impossibleBendFromReference(Direction referenceFacing, BlockPos referencePos, Direction peerFacing, BlockPos peerPos) {
        if (!CoasterAnchorpointBlock.peerInReferencePickyHalfSpace(referencePos, referenceFacing, peerPos)) {
            return false;
        }
        if (!CoasterAnchorpointBlock.peerAlignedOnOrthogonalPlaneThroughFacingAxis(referencePos, referenceFacing, peerPos)) {
            return false;
        }
        return CoasterAnchorpointBlock.facesToward(peerPos, peerFacing, referencePos);
    }

    public static boolean connectionBelowMinLegAngle(Level level, BlockPos anchorA, BlockPos anchorB, BezierConnection primary) {
        if (primary == null) {
            return false;
        }
        return CoasterAnchorpointBlock.legAngleTooShallowAtAnchor(level, anchorA, anchorB) || CoasterAnchorpointBlock.legAngleTooShallowAtAnchor(level, anchorB, anchorA);
    }

    private static boolean legAngleTooShallowAtAnchor(Level level, BlockPos hub, BlockPos newPeer) {
        BlockEntity blockEntity = level.getBlockEntity(hub);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        Vec3 hubCenter = CoasterAnchorpointBlockEntity.worldCenter(level, hub);
        Vec3 toNew = CoasterAnchorpointBlockEntity.worldCenter(level, newPeer).subtract(hubCenter);
        if (toNew.lengthSqr() < 1.0E-12) {
            return false;
        }
        toNew = toNew.normalize();
        double maxDot = CoasterTrackGauge.anchorPlacementMinLegAngleMaxDot();
        for (BlockPos existingPeer : be.getAnchorPeerCurvesView().keySet()) {
            Vec3 toExisting = CoasterAnchorpointBlockEntity.worldCenter(level, existingPeer).subtract(hubCenter);
            if (toExisting.lengthSqr() < 1.0E-12 || !((toExisting = toExisting.normalize()).dot(toNew) > maxDot)) continue;
            return true;
        }
        return false;
    }

    public static boolean connectionReversesOnSameFacingCoplanarPlane(Level level, BlockPos anchorA, BlockPos anchorB) {
        return CoasterAnchorpointBlock.reversesAtHubIfAddingSecondLeg(level, anchorA, anchorB) || CoasterAnchorpointBlock.reversesAtHubIfAddingSecondLeg(level, anchorB, anchorA);
    }

    private static boolean reversesAtHubIfAddingSecondLeg(Level level, BlockPos hub, BlockPos newPeer) {
        BlockEntity blockEntity = level.getBlockEntity(hub);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        Map<BlockPos, BezierConnection> existingPeers = be.getAnchorPeerCurvesView();
        if (existingPeers.size() != 1) {
            return false;
        }
        BlockPos existingPeer = existingPeers.keySet().iterator().next();
        return CoasterAnchorpointBlock.hubReversesOnCoplanarSameFacingPlane(level, hub, existingPeer, newPeer);
    }

    private static boolean hubReversesOnCoplanarSameFacingPlane(Level level, BlockPos hub, BlockPos existingPeer, BlockPos newPeer) {
        BlockState hubState = level.getBlockState(hub);
        BlockState stateExisting = level.getBlockState(existingPeer);
        BlockState stateNew = level.getBlockState(newPeer);
        if (!(hubState.getBlock() instanceof CoasterAnchorpointBlock && stateExisting.getBlock() instanceof CoasterAnchorpointBlock && stateNew.getBlock() instanceof CoasterAnchorpointBlock)) {
            return false;
        }
        Direction facing = (Direction)hubState.getValue((Property)FACING);
        if (facing != stateExisting.getValue((Property)FACING) || facing != stateNew.getValue((Property)FACING)) {
            return false;
        }
        Vec3 centerHub = CoasterAnchorpointBlockEntity.worldCenter(level, hub);
        Vec3 centerExisting = CoasterAnchorpointBlockEntity.worldCenter(level, existingPeer);
        Vec3 centerNew = CoasterAnchorpointBlockEntity.worldCenter(level, newPeer);
        Vec3 toExisting = centerExisting.subtract(centerHub);
        Vec3 toNew = centerNew.subtract(centerHub);
        if (toExisting.lengthSqr() < 1.0E-12 || toNew.lengthSqr() < 1.0E-12) {
            return false;
        }
        Vec3 inDir = centerHub.subtract(centerExisting);
        Vec3 outDir = centerNew.subtract(centerHub);
        if (inDir.lengthSqr() < 1.0E-12 || outDir.lengthSqr() < 1.0E-12) {
            return false;
        }
        Vec3 facingUnit = Vec3.atLowerCornerOf((Vec3i)facing.getNormal());
        Vec3 planeNormal = toExisting.cross(toNew);
        double fnLenSqr = planeNormal.lengthSqr();
        if (fnLenSqr < 1.0E-12 ? (fnLenSqr = (planeNormal = toExisting.cross(facingUnit)).lengthSqr()) < 1.0E-12 : Math.abs((planeNormal = planeNormal.normalize()).dot(facingUnit)) > 0.001) {
            return false;
        }
        planeNormal = planeNormal.normalize();
        inDir = CoasterAnchorpointBlock.projectOntoPlane(inDir, planeNormal);
        outDir = CoasterAnchorpointBlock.projectOntoPlane(outDir, planeNormal);
        if (inDir.lengthSqr() < 1.0E-12 || outDir.lengthSqr() < 1.0E-12) {
            return false;
        }
        return CoasterAnchorpointBlock.legReversesOnCoplanarPlane(inDir, outDir, planeNormal, facingUnit);
    }

    private static Vec3 projectOntoPlane(Vec3 v, Vec3 planeUnitNormal) {
        return v.subtract(planeUnitNormal.scale(v.dot(planeUnitNormal)));
    }

    private static boolean legReversesOnCoplanarPlane(Vec3 inDir, Vec3 outDir, Vec3 planeUnitNormal, Vec3 facingUnit) {
        Vec3 outUnit;
        double eps = 1.0E-4;
        Vec3 inUnit = inDir.normalize();
        if (inUnit.dot(outUnit = outDir.normalize()) < -0.99) {
            return true;
        }
        Vec3 steerAxis = planeUnitNormal.cross(facingUnit);
        if (steerAxis.lengthSqr() < 1.0E-8) {
            return false;
        }
        steerAxis = steerAxis.normalize();
        double inSteer = inUnit.dot(steerAxis);
        double outSteer = outUnit.dot(steerAxis);
        return Math.abs(inSteer) > eps && Math.abs(outSteer) > eps && inSteer * outSteer < 0.0;
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        CoasterAnchorpointBlockEntity anchor;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity && (anchor = (CoasterAnchorpointBlockEntity)blockEntity).hasChainLiftShaft()) {
            return face.getAxis() == anchor.getChainLiftShaftAxis();
        }
        return false;
    }

    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.SUCCESS;
    }

    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CoasterAnchorpointBlock.shapeForFacing((Direction)state.getValue((Property)FACING));
    }

    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        Level wl;
        VoxelShape base = CoasterAnchorpointBlock.shapeForFacing((Direction)state.getValue((Property)FACING));
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return base;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        if (be.getAnchorPeerCurvesView().isEmpty()) {
            return base;
        }
        if (level instanceof Level && (wl = (Level)level).isClientSide() && PickInteractionGates.destroyKeyDown) {
            return base;
        }
        return Shapes.or((VoxelShape)base, (VoxelShape)CURVE_INTERACTION_SHAPE);
    }

    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CoasterAnchorpointBlock.shapeForFacing((Direction)state.getValue((Property)FACING));
    }

    public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
        return Shapes.empty();
    }

    protected RenderShape getRenderShape(BlockState state) {
        if (((Boolean)state.getValue((Property)CHAIN_LIFT_SHAFT)).booleanValue()) {
            return RenderShape.INVISIBLE;
        }
        return RenderShape.MODEL;
    }

    protected BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rotation.rotate((Direction)state.getValue((Property)FACING)));
    }

    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation((Direction)state.getValue((Property)FACING)));
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)context.getClickedFace());
    }

    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide()) {
            CoasterAnchorGirderConnection.scheduleAdjacentGirderUpdates(level, pos, (LevelTickAccess<Block>)level.getBlockTicks());
        }
    }

    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide() && level.getBlockState(fromPos).getBlock() instanceof GirderBlock) {
            CoasterAnchorGirderConnection.scheduleAdjacentGirderUpdates(level, pos, (LevelTickAccess<Block>)level.getBlockTicks());
        }
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, CHAIN_LIFT_SHAFT, TRACK_CONNECTED});
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoasterAnchorpointBlockEntity(pos, state);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return CoasterAnchorpointBlock.createTickerHelper(type, (BlockEntityType)((BlockEntityType)SimulatedCoastersBlocks.COASTER_ANCHORPOINT_BE.get()), CoasterAnchorpointBlockEntity::serverTick);
    }

    public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
        CoasterPathAssemblyGraphDeferral.scheduleSubLevelBlockMove(originLevel, oldPos, newPos);
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide()) {
                CoasterAnchorGirderConnection.scheduleAdjacentGirderUpdates(level, pos, (LevelTickAccess<Block>)level.getBlockTicks());
            }
            if (level instanceof ServerLevel) {
                ServerLevel sl = (ServerLevel)level;
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
                    CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
                    anchor.detachAllConnections(sl);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static VoxelShape shapeForFacing(Direction facing) {
        return switch (facing) {
            default -> throw new MatchException(null, null);
            case Direction.DOWN -> SHAPE_DOWN;
            case Direction.NORTH -> SHAPE_NORTH;
            case Direction.SOUTH -> SHAPE_SOUTH;
            case Direction.WEST -> SHAPE_WEST;
            case Direction.EAST -> SHAPE_EAST;
            case Direction.UP -> SHAPE_UP;
        };
    }
}

