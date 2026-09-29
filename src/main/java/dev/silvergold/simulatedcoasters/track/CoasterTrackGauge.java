package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.Config;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class CoasterTrackGauge {
   public static final float BAKE_RAIL_HALF_WIDTH_SCALE = 0.8F;
   public static final float BAKE_RAIL_HALF_GAUGE_BLOCKS = 0.965F;
   public static final float BAKE_TRACK_MODEL_Y_LIFT = 0.375F;
   public static final float BAKE_TRACK_MODEL_Y_LIFT_ASCENDING = 0.375F;
   public static final float BAKE_CENTER_BEAM_EXTRA_Y = -0.4375F;
   public static final float BAKE_ANCHOR_JUNCTION_PLATE_EXTRA_Y = -1.0F;
   public static final float BAKE_ANCHOR_JUNCTION_PLATE_TOP_CENTER_Y = 0.75F;
   public static final float BAKE_TIE_MESH_CENTER_X = 0.5F;
   public static final float BAKE_TIE_MESH_CENTER_Y = -0.1315F;
   public static final float BAKE_TIE_MESH_CENTER_Z = 0.25F;
   public static final float BAKE_OPEN_END_TIE_PLATE_LOCAL_Y = 0.8685F;
   public static final float BAKE_JUNCTION_TIE_LOCAL_Y = -0.1315F;
   public static final float BAKE_ANCHOR_GIRDER_MODEL_PIVOT_X = -0.5F;
   public static final float BAKE_ANCHOR_GIRDER_MODEL_PIVOT_Y = 0.0F;
   public static final float BAKE_ANCHOR_GIRDER_MODEL_PIVOT_Z = -0.5F;
   public static final float BAKE_ANCHOR_GIRDER_REFERENCE_HEIGHT_BLOCKS = 1.0F;
   public static final float BAKE_ANCHOR_GIRDER_MIN_SPAN_BLOCKS = 0.125F;
   public static final float BAKE_ANCHOR_GIRDER_HYBRID_SEGMENT_OFFSET_BLOCKS = 0.45F;
   public static final float CURVE_TIE_TARGET_SEPARATION_BLOCKS = 1.0F;
   public static final int BAKE_CURVE_TIE_STRIDE = 2;
   public static final float BEZIER_LIFT_MIN_BLOCKS = 0.25F;
   public static final float BEZIER_LIFT_MAX_BLOCKS = 2.5F;
   public static final float BEZIER_TILT_MIN_RADIANS = (float) (-Math.PI / 2);
   public static final float BEZIER_TILT_MAX_RADIANS = (float) (Math.PI / 2);
   public static final float BEZIER_TILT_SNAP_STEP_RADIANS = (float) (Math.PI / 12);
   public static final float BEZIER_TILT_NEIGHBOR_MAX_AWAY_RADIANS = (float) (Math.PI / 2);
   public static final double BEZIER_HANDLE_MAX_CHORD_FRACTION = 0.7;
   public static final float BEZIER_HANDLE_MAX_AXIS_AWAY_RADIANS = (float)Math.toRadians(60.0);
   public static final float BEZIER_HANDLE_HUB_MAX_AXIS_AWAY_RADIANS = (float)Math.toRadians(60.0);
   public static final float ANCHOR_DEFAULT_LIFT_BLOCKS = 0.5F;
   public static final float ANCHOR_CENTER_GLOBAL_Y_OFFSET_BLOCKS = 0.0F;
   public static final int ANCHOR_OPTIMIZER_MAX_AXIS_CANDIDATES = 120;
   public static final int ANCHOR_OPTIMIZER_MAX_PAIR_CANDIDATES = 40;
   public static final int ANCHOR_OPTIMIZER_SMOOTHNESS_SAMPLES = 14;
   public static final double ANCHOR_PLACEMENT_MAX_HANDLE_PERP_TO_CHORD_RATIO = 0.85;
   public static final double ANCHOR_PLACEMENT_MAX_HANDLE_PAST_ENDPOINT_RATIO = 0.6;
   public static final double ANCHOR_PLACEMENT_MAX_CHORD_PARAM_DROP_PER_STEP = 0.035;
   public static final double ANCHOR_PLACEMENT_ALT_SUPPRESS_MIN_CURVE_RADIUS_BLOCKS = 2.0;
   public static final double ANCHOR_PLACEMENT_ALT_SUPPRESS_MIN_RADIUS_END_REGION = 0.25;
   public static final int ANCHOR_PLACEMENT_ALT_SUPPRESS_MIN_RADIUS_END_SAMPLES = 7;
   public static final float ANCHOR_PLACEMENT_MIN_LEG_ANGLE_RADIANS = (float)Math.toRadians(80.0);
   public static final double ANCHOR_PLACEMENT_COPLANAR_FACING_DOT_EPS = 0.001;
   public static final double ANCHOR_PLACEMENT_COPLANAR_MIN_CROSS_LEN_SQ = 1.0E-6;
   public static final double ANCHOR_PLACEMENT_PLANE_AXIS_COMPONENT_EPS = 1.0E-4;
   public static final double ANCHOR_PLACEMENT_PLANE_COLINEAR_BACKTRACK_MAX_DOT = -0.99;
   public static final float CURVE_RUNTIME_WORLD_Y_OFFSET = -0.1875F;
   public static final float CURVE_RUNTIME_INTERACTION_WORLD_Y_OFFSET = -0.125F;
   public static final float PREVIEW_EXTRA_WORLD_Y_NON_UP_FACING = 0.0F;
   public static final float CREATE_PREVIEW_SPINE_TO_RAIL_Y = 0.25F;
   public static final float BAKE_BEZIER_ITERATOR_WORLD_Y_OFFSET = 0.1875F;
   public static final float GRAPH_PATH_CENTER_OFFSET_Y = 0.0F;
   public static final float GRAPH_PATH_CENTER_LOCAL_OFFSET_BLOCKS = 0.0625F;
   public static final float GRAPH_OPEN_END_STRAIGHT_EXTENSION_BLOCKS = 0.5F;
   public static final float OPEN_END_STRAIGHT_EXTENSION_MIN_BLOCKS = 0.15F;
   public static final float OPEN_END_STRAIGHT_EXTENSION_MAX_BLOCKS = 1.25F;
   public static final float OPEN_END_STRAIGHT_EXTENSION_SNAP_TOLERANCE_BLOCKS = 0.08F;
   public static final float CURVE_SURVIVAL_DIG_SCALE_DEFAULT = 0.125F;
   public static final float CURVE_SURVIVAL_DIG_SCALE_PICKAXE = 0.5F;
   public static final ThreadLocal<BezierConnection> BAKE_BEZIER = new ThreadLocal<>();
   public static final ThreadLocal<BezierConnection> RESOLVING_BEZIER = new ThreadLocal<>();

   public static int anchorGirderHybridSegmentCount(double spanBlocks) {
      double v = spanBlocks - 0.45F;
      return Math.max(1, 1 + (int)Math.floor(v));
   }

   public static double anchorPlacementMinLegAngleMaxDot() {
      return Math.cos((double)ANCHOR_PLACEMENT_MIN_LEG_ANGLE_RADIANS);
   }

   public static float curveInteractionLift() {
      return 0.375F;
   }

   public static float coasterPreviewRailHalfGauge() {
      return 0.772F;
   }

   public static float coasterPreviewSpineVerticalBumpBlocks() {
      return 0.625F;
   }

   public static float coasterPreviewCurveLiftAlongFaceUp() {
      return coasterPreviewSpineVerticalBumpBlocks() - 0.1875F;
   }

   public static float coasterPreviewRailWorldYOffsetBlocks() {
      return 0.0F;
   }

   public static int maxCoasterCurveEndpointSpanBlocks() {
      return Config.maxTrackConnectionSpanBlocks();
   }

   public static long maxCoasterCurveEndpointSpanBlocksSq() {
      int span = maxCoasterCurveEndpointSpanBlocks();
      return (long)span * (long)span;
   }

   public static int maxCoasterCurvePacketInteractionRangeBlocks() {
      return maxCoasterCurveEndpointSpanBlocks() * 4 + 16;
   }

   public static float survivalCurveDigScale(Player player) {
      return isHoldingPickaxe(player) ? 0.5F : 0.125F;
   }

   private static boolean isHoldingPickaxe(Player player) {
      return player.getMainHandItem().is(ItemTags.PICKAXES) || player.getOffhandItem().is(ItemTags.PICKAXES);
   }

   private CoasterTrackGauge() {
   }

   @Nullable
   public static BezierConnection bakingBezier() {
      return BAKE_BEZIER.get();
   }

   @Nullable
   public static BezierConnection resolvingBezier() {
      return RESOLVING_BEZIER.get();
   }
}
