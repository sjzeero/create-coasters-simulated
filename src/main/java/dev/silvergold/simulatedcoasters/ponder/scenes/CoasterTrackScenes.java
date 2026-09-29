package dev.silvergold.simulatedcoasters.ponder.scenes;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.ponder.ChainLiftPonderExtras;
import dev.silvergold.simulatedcoasters.ponder.CoasterPonderRestore;
import dev.silvergold.simulatedcoasters.ponder.CoasterPonderUndyeEffects;
import dev.silvergold.simulatedcoasters.ponder.elements.ChainLiftExtrasElement;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.Vec3;

public final class CoasterTrackScenes {
   private static final int DISPLAY_Y = 1;
   private static final BlockPos ANCHOR_LEFT = new BlockPos(3, 0, 7);
   private static final BlockPos ANCHOR_RIGHT = new BlockPos(11, 0, 7);
   private static final BlockPos[] MULTI_ANCHOR_LAYOUT = new BlockPos[]{
      new BlockPos(3, 0, 6),
      new BlockPos(5, 0, 10),
      new BlockPos(7, 0, 2),
      new BlockPos(7, 0, 6),
      new BlockPos(9, 0, 4),
      new BlockPos(10, 0, 8),
      new BlockPos(12, 0, 11)
   };
   private static final int MULTI_LAYER_MIN_X = 3;
   private static final int MULTI_LAYER_MAX_X = 12;
   private static final int MULTI_LAYER_MIN_Z = 2;
   private static final int MULTI_LAYER_MAX_Z = 11;
   private static final BlockPos UNDYE_AXE_ANCHOR = new BlockPos(10, 0, 8);
   private static final CoasterTrackScenes.PonderDyeHint[] LAYER_5_DYE_HINTS = new CoasterTrackScenes.PonderDyeHint[]{
      new CoasterTrackScenes.PonderDyeHint(new BlockPos(3, 0, 6), Items.BLUE_DYE),
      new CoasterTrackScenes.PonderDyeHint(new BlockPos(7, 0, 2), Items.YELLOW_DYE),
      new CoasterTrackScenes.PonderDyeHint(new BlockPos(10, 0, 8), Items.RED_DYE)
   };
   private static final int CHAIN_HINT_STAGGER = 30;
   private static final int CHAIN_HINT_HOLD_AFTER_LAST = 25;
   private static final int GAP_BEFORE_DYE_HINTS = 30;
   private static final int DYE_HINT_STAGGER = 15;
   private static final int DYE_HINT_HOLD_AFTER_LAST = 15;
   private static final Vec3 OFFSCREEN = new Vec3(0.0, -100.0, 0.0);

   private CoasterTrackScenes() {
   }

   public static void placement(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      scene.title("ponder_track_placement", "Placing Coaster Track");
      scene.configureBasePlate(0, 0, 15);
      scene.scaleSceneView(0.7F);
      scene.setSceneOffsetY(-1.0F);
      scene.showBasePlate();
      scene.idle(10);
      Selection twoAnchorsAtDisplay = util.select().fromTo(3, 1, 7, 11, 1, 7);
      Vec3 twoAnchorTextFocus = util.vector().topOf(7, 1, 7);
      Vec3 multiAnchorFocus = util.vector().topOf(8, 0, 6);
      ItemStack trackStack = new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
      Vec3 leftAnchorTop = util.vector().topOf(ANCHOR_LEFT.atY(1));
      Vec3 rightAnchorTop = util.vector().topOf(ANCHOR_RIGHT.atY(1));
      ElementLink<WorldSectionElement> trackLayer = scene.world().showIndependentSection(twoAnchorsAtDisplay, Direction.DOWN);
      scene.idle(20);
      scene.overlay().showText(90).attachKeyFrame().independent(20).text("When connecting two Track Anchorpoints with a Coaster Track...");
      scene.overlay().showControls(leftAnchorTop, Pointing.DOWN, 70).withItem(trackStack);
      scene.idle(30);
      scene.overlay().showControls(rightAnchorTop, Pointing.DOWN, 40).withItem(trackStack);
      scene.idle(50);
      trackLayer = swapAnchorLayer(scene, util, trackLayer, 2, false);
      scene.idle(20);
      scene.overlay().showText(60).attachKeyFrame().text("A track spline is created").placeNearTarget().pointAt(twoAnchorTextFocus);
      scene.idle(70);
      scene.overlay().showControls(leftAnchorTop, Pointing.DOWN, 60).withItem(AllItems.WRENCH.asStack());
      scene.overlay().showText(60).attachKeyFrame().text("A wrench can be used to adjust the path of the spline").placeNearTarget().pointAt(leftAnchorTop);
      scene.idle(40);
      trackLayer = swapAnchorLayer(scene, util, trackLayer, 3, false);
      scene.idle(60);
      trackLayer = swapAnchorLayer(scene, util, trackLayer, 4, true, true);
      scene.idle(20);
      scene.overlay()
         .showText(80)
         .attachKeyFrame()
         .text("Tracks will automatically smooth out when connecting multiple anchorpoints")
         .placeNearTarget()
         .pointAt(multiAnchorFocus);
      scene.idle(90);
      scene.overlay()
         .showText(75)
         .attachKeyFrame()
         .text("Track smoothing can be disabled by holding left alt during placement")
         .placeNearTarget()
         .pointAt(multiAnchorFocus);
      scene.idle(75);
      scene.idle(30);
      showDyedTrackUseHints(scene, util);
      trackLayer = swapAnchorLayer(scene, util, trackLayer, 5, true);
      scene.overlay().showText(80).attachKeyFrame().text("Dye can be used to decorate tracks").placeNearTarget().pointAt(multiAnchorFocus);
      scene.idle(90);
      Vec3 undyeAnchorTop = util.vector().topOf(UNDYE_AXE_ANCHOR.atY(1));
      scene.idle(30);
      scene.overlay().showControls(undyeAnchorTop, Pointing.DOWN, 60).withItem(new ItemStack(Items.STONE_AXE)).rightClick();
      trackLayer = swapAnchorLayer(scene, util, trackLayer, 4, true);
      CoasterPonderUndyeEffects.spawnUndyeParticlesForDyedLayer(scene, 5, 1, MULTI_ANCHOR_LAYOUT);
      scene.overlay().showText(60).attachKeyFrame().text("Right clicking with an axe removes dye").placeNearTarget().pointAt(undyeAnchorTop);
      scene.idle(80);
   }

   public static void chainLifts(SceneBuilder builder, SceneBuildingUtil util) {
      CreateSceneBuilder scene = new CreateSceneBuilder(builder);
      scene.title("ponder_chainlift", "Adding and Removing Chain Lifts");
      scene.configureBasePlate(0, 0, 15);
      scene.scaleSceneView(0.7F);
      scene.setSceneOffsetY(-1.0F);
      scene.showBasePlate();
      scene.idle(10);
      Selection plainTrack = util.select().fromTo(3, 1, 7, 11, 1, 7);
      ElementLink<WorldSectionElement> trackLayer = scene.world().showIndependentSection(plainTrack, Direction.DOWN);
      CoasterPonderRestore.applyDisplayedAnchorLayer(scene, 1, 1, ANCHOR_LEFT, ANCHOR_RIGHT);
      scene.idle(20);
      ItemStack chain = new ItemStack(Items.CHAIN);
      Vec3 leftAnchorTop = util.vector().topOf(ANCHOR_LEFT.atY(1));
      Vec3 rightAnchorTop = util.vector().topOf(ANCHOR_RIGHT.atY(1));
      Vec3 trackMiddle = util.vector().topOf(7, 1, 7);
      scene.overlay().showText(100).attachKeyFrame().independent(35).text("With a Chain, select the start and end of a track span to add a chain lift");
      scene.idle(20);
      scene.overlay().showControls(leftAnchorTop, Pointing.DOWN, 55).withItem(chain).rightClick();
      scene.idle(30);
      scene.overlay().showControls(rightAnchorTop, Pointing.DOWN, 25).withItem(chain).rightClick();
      scene.idle(25);
      trackLayer = swapAnchorLayer(scene, util, trackLayer, 2, false);
      ElementLink<ChainLiftExtrasElement> chainRibbon = ChainLiftPonderExtras.show(
         scene, ChainLiftExtrasElement.span(ANCHOR_LEFT.atY(1), ANCHOR_RIGHT.atY(1)), Direction.DOWN
      );
      scene.idle(35);
      scene.overlay()
         .showText(90)
         .attachKeyFrame()
         .text("The shaft at the starting anchor accepts rotational power and drives the chain")
         .placeNearTarget()
         .pointAt(leftAnchorTop);
      scene.idle(100);
      scene.overlay()
         .showText(90)
         .attachKeyFrame()
         .text("Sneak-right-click anywhere along the chain-lifted span with a Wrench to remove it")
         .placeNearTarget()
         .pointAt(trackMiddle);
      scene.idle(20);
      scene.overlay().showControls(trackMiddle, Pointing.DOWN, 70).withItem(AllItems.WRENCH.asStack()).whileSneaking().rightClick();
      scene.idle(70);
      ChainLiftPonderExtras.hide(scene, chainRibbon, Direction.UP);
      trackLayer = swapAnchorLayer(scene, util, trackLayer, 1, false);
      scene.idle(50);
   }

   private static void showDyedTrackUseHints(CreateSceneBuilder scene, SceneBuildingUtil util) {
      int count = LAYER_5_DYE_HINTS.length;
      int simultaneousEnd = 15 * (count - 1) + 15;

      for (int i = 0; i < count; i++) {
         if (i > 0) {
            scene.idle(15);
         }

         CoasterTrackScenes.PonderDyeHint hint = LAYER_5_DYE_HINTS[i];
         Vec3 at = util.vector().topOf(hint.anchor().atY(1));
         int duration = simultaneousEnd - i * 15;
         scene.overlay().showControls(at, Pointing.DOWN, duration).withItem(new ItemStack(hint.dye())).rightClick();
      }

      scene.idle(15);
   }

   private static Selection layerSelection(SceneBuildingUtil util, int sourceY, boolean multiAnchor) {
      return multiAnchor ? util.select().fromTo(3, sourceY, 2, 12, sourceY, 11) : util.select().fromTo(3, sourceY, 7, 11, sourceY, 7);
   }

   private static void applyDisplayedLayer(CreateSceneBuilder scene, int sourceY, int displayY, boolean multiAnchor) {
      if (multiAnchor) {
         CoasterPonderRestore.applyDisplayedAnchorLayer(scene, sourceY, displayY, MULTI_ANCHOR_LAYOUT);
      } else {
         CoasterPonderRestore.applyDisplayedAnchorLayer(scene, sourceY, displayY, ANCHOR_LEFT, ANCHOR_RIGHT);
      }
   }

   private static ElementLink<WorldSectionElement> swapAnchorLayer(
      CreateSceneBuilder scene, SceneBuildingUtil util, ElementLink<WorldSectionElement> previous, int sourceY, boolean multiAnchor
   ) {
      return swapAnchorLayer(scene, util, previous, sourceY, multiAnchor, false);
   }

   private static ElementLink<WorldSectionElement> swapAnchorLayer(
      CreateSceneBuilder scene, SceneBuildingUtil util, ElementLink<WorldSectionElement> previous, int sourceY, boolean multiAnchor, boolean fade
   ) {
      Selection layer = layerSelection(util, sourceY, multiAnchor);
      Vec3 offset = util.vector().of(0.0, (double)(1 - sourceY), 0.0);
      ElementLink<WorldSectionElement> section;
      if (fade) {
         scene.world().hideIndependentSection(previous, Direction.UP);
         scene.idle(10);
         section = scene.world().showIndependentSection(layer, Direction.DOWN);
      } else {
         scene.world().moveSection(previous, OFFSCREEN, 0);
         section = scene.world().showIndependentSectionImmediately(layer);
         scene.world().hideIndependentSection(previous, Direction.DOWN);
      }

      scene.world().moveSection(section, offset, 0);
      applyDisplayedLayer(scene, sourceY, 1, multiAnchor);
      return section;
   }

   private static record PonderDyeHint(BlockPos anchor, Item dye) {
   }
}
