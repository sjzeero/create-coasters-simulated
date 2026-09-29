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
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathAssemblyGraphDeferral;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoasterAnchorpointBlock
   extends BaseEntityBlock
   implements BlockSubLevelCollisionShape,
   BlockSubLevelAssemblyListener,
   IHaveBigOutline,
   IRotate,
   IWrenchable {
   public static final MapCodec<CoasterAnchorpointBlock> CODEC = simpleCodec(CoasterAnchorpointBlock::new);
   public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());
   public static final BooleanProperty CHAIN_LIFT_SHAFT = BooleanProperty.create("chain_lift_shaft");
   public static final BooleanProperty TRACK_CONNECTED = BooleanProperty.create("track_connected");
   private static final VoxelShape SHAPE_UP = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.5, 1.0);
   private static final VoxelShape SHAPE_DOWN = Shapes.box(0.0, 0.5, 0.0, 1.0, 1.0, 1.0);
   private static final VoxelShape SHAPE_NORTH = Shapes.box(0.0, 0.0, 0.5, 1.0, 1.0, 1.0);
   private static final VoxelShape SHAPE_SOUTH = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.5);
   private static final VoxelShape SHAPE_WEST = Shapes.box(0.5, 0.0, 0.0, 1.0, 1.0, 1.0);
   private static final VoxelShape SHAPE_EAST = Shapes.box(0.0, 0.0, 0.0, 0.5, 1.0, 1.0);
   private static final double INTERACTION_MARGIN = 0.08;
   private static final VoxelShape CURVE_INTERACTION_SHAPE = Shapes.box(-0.08, -0.08, -0.08, 1.08, 1.08, 1.08);

   public CoasterAnchorpointBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.UP)).setValue(CHAIN_LIFT_SHAFT, false))
            .setValue(TRACK_CONNECTED, false)
      );
   }

   public static Properties defaultProperties() {
      return Properties.of().mapColor(MapColor.STONE).strength(3.0F, 6.0F).sound(SoundType.NETHERITE_BLOCK).noOcclusion();
   }

   public static boolean haveOppositeFacing(BlockState anchorA, BlockState anchorB) {
      if (anchorA.getBlock() instanceof CoasterAnchorpointBlock && anchorB.getBlock() instanceof CoasterAnchorpointBlock) {
         Direction fa = (Direction)anchorA.getValue(FACING);
         Direction fb = (Direction)anchorB.getValue(FACING);
         return fa == fb.getOpposite();
      } else {
         return false;
      }
   }

   public static boolean haveSameFacingCollinearAlongFacing(BlockState anchorA, BlockState anchorB, BlockPos posA, BlockPos posB) {
      if (anchorA.getBlock() instanceof CoasterAnchorpointBlock && anchorB.getBlock() instanceof CoasterAnchorpointBlock) {
         Direction facing = (Direction)anchorA.getValue(FACING);
         if (facing != anchorB.getValue(FACING)) {
            return false;
         } else {
            BlockPos delta = posB.subtract(posA);
            return delta.equals(BlockPos.ZERO) ? false : isDeltaOnAxisOnly(delta, facing.getAxis());
         }
      } else {
         return false;
      }
   }

   private static boolean isDeltaOnAxisOnly(BlockPos delta, Axis axis) {
      return switch (axis) {
         case X -> delta.getY() == 0 && delta.getZ() == 0;
         case Y -> delta.getX() == 0 && delta.getZ() == 0;
         case Z -> delta.getX() == 0 && delta.getY() == 0;
         default -> throw new MatchException(null, null);
      };
   }

   private static boolean peerInReferencePickyHalfSpace(BlockPos referencePos, Direction referenceFacing, BlockPos peerPos) {
      BlockPos delta = peerPos.subtract(referencePos);
      Vec3i n = referenceFacing.getNormal();
      return delta.getX() * n.getX() + delta.getY() * n.getY() + delta.getZ() * n.getZ() <= 0;
   }

   private static boolean peerAlignedOnOrthogonalPlaneThroughFacingAxis(BlockPos referencePos, Direction referenceFacing, BlockPos peerPos) {
      BlockPos delta = peerPos.subtract(referencePos);
      if (delta.equals(BlockPos.ZERO)) {
         return false;
      } else {
         return switch (referenceFacing.getAxis()) {
            case X -> delta.getY() == 0 || delta.getZ() == 0;
            case Y -> delta.getX() == 0 || delta.getZ() == 0;
            case Z -> delta.getX() == 0 || delta.getY() == 0;
            default -> throw new MatchException(null, null);
         };
      }
   }

   private static boolean facesToward(BlockPos fromPos, Direction fromFacing, BlockPos towardPos) {
      BlockPos delta = towardPos.subtract(fromPos);
      if (delta.equals(BlockPos.ZERO)) {
         return false;
      } else {
         Vec3i n = fromFacing.getNormal();
         return delta.getX() * n.getX() + delta.getY() * n.getY() + delta.getZ() * n.getZ() > 0;
      }
   }

   public static boolean connectionHasImpossibleBend(BlockState anchorA, BlockState anchorB, BlockPos posA, BlockPos posB) {
      if (anchorA.getBlock() instanceof CoasterAnchorpointBlock && anchorB.getBlock() instanceof CoasterAnchorpointBlock) {
         Direction fa = (Direction)anchorA.getValue(FACING);
         Direction fb = (Direction)anchorB.getValue(FACING);
         return fa == fb ? false : impossibleBendFromReference(fa, posA, fb, posB) || impossibleBendFromReference(fb, posB, fa, posA);
      } else {
         return false;
      }
   }

   private static boolean impossibleBendFromReference(Direction referenceFacing, BlockPos referencePos, Direction peerFacing, BlockPos peerPos) {
      if (!peerInReferencePickyHalfSpace(referencePos, referenceFacing, peerPos)) {
         return false;
      } else {
         return !peerAlignedOnOrthogonalPlaneThroughFacingAxis(referencePos, referenceFacing, peerPos) ? false : facesToward(peerPos, peerFacing, referencePos);
      }
   }

   public static boolean connectionBelowMinLegAngle(Level level, BlockPos anchorA, BlockPos anchorB, BezierConnection primary) {
      return primary == null ? false : legAngleTooShallowAtAnchor(level, anchorA, anchorB) || legAngleTooShallowAtAnchor(level, anchorB, anchorA);
   }

   private static boolean legAngleTooShallowAtAnchor(Level level, BlockPos hub, BlockPos newPeer) {
      if (level.getBlockEntity(hub) instanceof CoasterAnchorpointBlockEntity be) {
         Vec3 var11 = CoasterAnchorpointBlockEntity.worldCenter(level, hub);
         Vec3 toNew = CoasterAnchorpointBlockEntity.worldCenter(level, newPeer).subtract(var11);
         if (toNew.lengthSqr() < 1.0E-12) {
            return false;
         } else {
            toNew = toNew.normalize();
            double maxDot = CoasterTrackGauge.anchorPlacementMinLegAngleMaxDot();

            for (BlockPos existingPeer : be.getAnchorPeerCurvesView().keySet()) {
               Vec3 toExisting = CoasterAnchorpointBlockEntity.worldCenter(level, existingPeer).subtract(var11);
               if (!(toExisting.lengthSqr() < 1.0E-12)) {
                  toExisting = toExisting.normalize();
                  if (toExisting.dot(toNew) > maxDot) {
                     return true;
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean connectionReversesOnSameFacingCoplanarPlane(Level level, BlockPos anchorA, BlockPos anchorB) {
      return reversesAtHubIfAddingSecondLeg(level, anchorA, anchorB) || reversesAtHubIfAddingSecondLeg(level, anchorB, anchorA);
   }

   private static boolean reversesAtHubIfAddingSecondLeg(Level level, BlockPos hub, BlockPos newPeer) {
      if (level.getBlockEntity(hub) instanceof CoasterAnchorpointBlockEntity be) {
         Map<BlockPos, BezierConnection> existingPeers = be.getAnchorPeerCurvesView();
         if (existingPeers.size() != 1) {
            return false;
         } else {
            BlockPos existingPeer = existingPeers.keySet().iterator().next();
            return hubReversesOnCoplanarSameFacingPlane(level, hub, existingPeer, newPeer);
         }
      } else {
         return false;
      }
   }

   private static boolean hubReversesOnCoplanarSameFacingPlane(Level level, BlockPos hub, BlockPos existingPeer, BlockPos newPeer) {
      BlockState hubState = level.getBlockState(hub);
      BlockState stateExisting = level.getBlockState(existingPeer);
      BlockState stateNew = level.getBlockState(newPeer);
      if (hubState.getBlock() instanceof CoasterAnchorpointBlock
         && stateExisting.getBlock() instanceof CoasterAnchorpointBlock
         && stateNew.getBlock() instanceof CoasterAnchorpointBlock) {
         Direction facing = (Direction)hubState.getValue(FACING);
         if (facing == stateExisting.getValue(FACING) && facing == stateNew.getValue(FACING)) {
            Vec3 centerHub = CoasterAnchorpointBlockEntity.worldCenter(level, hub);
            Vec3 centerExisting = CoasterAnchorpointBlockEntity.worldCenter(level, existingPeer);
            Vec3 centerNew = CoasterAnchorpointBlockEntity.worldCenter(level, newPeer);
            Vec3 toExisting = centerExisting.subtract(centerHub);
            Vec3 toNew = centerNew.subtract(centerHub);
            if (!(toExisting.lengthSqr() < 1.0E-12) && !(toNew.lengthSqr() < 1.0E-12)) {
               Vec3 inDir = centerHub.subtract(centerExisting);
               Vec3 outDir = centerNew.subtract(centerHub);
               if (!(inDir.lengthSqr() < 1.0E-12) && !(outDir.lengthSqr() < 1.0E-12)) {
                  Vec3 facingUnit = Vec3.atLowerCornerOf(facing.getNormal());
                  Vec3 planeNormal = toExisting.cross(toNew);
                  double fnLenSqr = planeNormal.lengthSqr();
                  if (fnLenSqr < 1.0E-12) {
                     planeNormal = toExisting.cross(facingUnit);
                     fnLenSqr = planeNormal.lengthSqr();
                     if (fnLenSqr < 1.0E-12) {
                        return false;
                     }
                  } else {
                     planeNormal = planeNormal.normalize();
                     if (Math.abs(planeNormal.dot(facingUnit)) > 0.001) {
                        return false;
                     }
                  }

                  planeNormal = planeNormal.normalize();
                  inDir = projectOntoPlane(inDir, planeNormal);
                  outDir = projectOntoPlane(outDir, planeNormal);
                  return !(inDir.lengthSqr() < 1.0E-12) && !(outDir.lengthSqr() < 1.0E-12)
                     ? legReversesOnCoplanarPlane(inDir, outDir, planeNormal, facingUnit)
                     : false;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Vec3 projectOntoPlane(Vec3 v, Vec3 planeUnitNormal) {
      return v.subtract(planeUnitNormal.scale(v.dot(planeUnitNormal)));
   }

   private static boolean legReversesOnCoplanarPlane(Vec3 inDir, Vec3 outDir, Vec3 planeUnitNormal, Vec3 facingUnit) {
      double eps = 1.0E-4;
      Vec3 inUnit = inDir.normalize();
      Vec3 outUnit = outDir.normalize();
      if (inUnit.dot(outUnit) < -0.99) {
         return true;
      } else {
         Vec3 steerAxis = planeUnitNormal.cross(facingUnit);
         if (steerAxis.lengthSqr() < 1.0E-8) {
            return false;
         } else {
            steerAxis = steerAxis.normalize();
            double inSteer = inUnit.dot(steerAxis);
            double outSteer = outUnit.dot(steerAxis);
            return Math.abs(inSteer) > eps && Math.abs(outSteer) > eps && inSteer * outSteer < 0.0;
         }
      }
   }

   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      if (world.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity anchor && anchor.hasChainLiftShaft()) {
         return face.getAxis() == anchor.getChainLiftShaftAxis();
      }

      return false;
   }

   public Axis getRotationAxis(BlockState state) {
      return Axis.Y;
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      return InteractionResult.SUCCESS;
   }

   public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
      return InteractionResult.PASS;
   }

   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return shapeForFacing((Direction)state.getValue(FACING));
   }

   protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
      VoxelShape base = shapeForFacing((Direction)state.getValue(FACING));
      if (level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be) {
         if (be.getAnchorPeerCurvesView().isEmpty()) {
            return base;
         } else {
            if (level instanceof Level wl && wl.isClientSide() && PickInteractionGates.destroyKeyDown) {
               return base;
            }

            return Shapes.or(base, CURVE_INTERACTION_SHAPE);
         }
      } else {
         return base;
      }
   }

   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return shapeForFacing((Direction)state.getValue(FACING));
   }

   public VoxelShape getSubLevelCollisionShape(BlockGetter blockGetter, BlockState state) {
      return Shapes.empty();
   }

   protected RenderShape getRenderShape(BlockState state) {
      return state.getValue(CHAIN_LIFT_SHAFT) ? RenderShape.INVISIBLE : RenderShape.MODEL;
   }

   protected BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getClickedFace());
   }

   public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
      if (!level.isClientSide()) {
         CoasterAnchorGirderConnection.scheduleAdjacentGirderUpdates(level, pos, level.getBlockTicks());
      }
   }

   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      super.neighborChanged(state, level, pos, block, fromPos, isMoving);
      if (!level.isClientSide() && level.getBlockState(fromPos).getBlock() instanceof GirderBlock) {
         CoasterAnchorGirderConnection.scheduleAdjacentGirderUpdates(level, pos, level.getBlockTicks());
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, CHAIN_LIFT_SHAFT, TRACK_CONNECTED});
   }

   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new CoasterAnchorpointBlockEntity(pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, (BlockEntityType)SimulatedCoastersBlocks.COASTER_ANCHORPOINT_BE.get(), CoasterAnchorpointBlockEntity::serverTick);
   }

   public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      CoasterPathAssemblyGraphDeferral.scheduleSubLevelBlockMove(originLevel, oldPos, newPos);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!state.is(newState.getBlock())) {
         if (!level.isClientSide()) {
            CoasterAnchorGirderConnection.scheduleAdjacentGirderUpdates(level, pos, level.getBlockTicks());
         }

         if (level instanceof ServerLevel sl && level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity anchor) {
            anchor.detachAllConnections(sl);
         }
      }

      super.onRemove(state, level, pos, newState, isMoving);
   }

   private static VoxelShape shapeForFacing(Direction facing) {
      return switch (facing) {
         case DOWN -> SHAPE_DOWN;
         case NORTH -> SHAPE_NORTH;
         case SOUTH -> SHAPE_SOUTH;
         case WEST -> SHAPE_WEST;
         case EAST -> SHAPE_EAST;
         case UP -> SHAPE_UP;
         default -> throw new MatchException(null, null);
      };
   }
}
