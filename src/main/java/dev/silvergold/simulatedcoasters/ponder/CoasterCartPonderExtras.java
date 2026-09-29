package dev.silvergold.simulatedcoasters.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.silvergold.simulatedcoasters.ponder.elements.CoasterCartExtrasElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction;
import net.createmod.ponder.foundation.instruction.FadeOutOfSceneInstruction;
import net.minecraft.core.Direction;

public final class CoasterCartPonderExtras {
   private static final int FADE_TICKS = 15;

   private CoasterCartPonderExtras() {
   }

   public static ElementLink<CoasterCartExtrasElement> show(CreateSceneBuilder scene, CoasterCartExtrasElement element, Direction fadeInFrom) {
      CoasterCartPonderExtras.ShowInstruction instruction = new CoasterCartPonderExtras.ShowInstruction(15, fadeInFrom, element);
      scene.addInstruction(instruction);
      return instruction.createLink(scene.getScene());
   }

   public static ElementLink<CoasterCartExtrasElement> showImmediately(CreateSceneBuilder scene, CoasterCartExtrasElement element) {
      CoasterCartPonderExtras.ShowInstruction instruction = new CoasterCartPonderExtras.ShowInstruction(0, Direction.DOWN, element);
      scene.addInstruction(instruction);
      return instruction.createLink(scene.getScene());
   }

   public static void hide(CreateSceneBuilder scene, ElementLink<CoasterCartExtrasElement> link, Direction fadeOutTo) {
      scene.addInstruction(new FadeOutOfSceneInstruction(15, fadeOutTo, link));
   }

   public static void hideImmediately(CreateSceneBuilder scene, ElementLink<CoasterCartExtrasElement> link) {
      scene.addInstruction(new FadeOutOfSceneInstruction(0, Direction.DOWN, link));
   }

   public static void setCouplerVisible(CreateSceneBuilder scene, ElementLink<CoasterCartExtrasElement> link, boolean visible) {
      scene.addInstruction(ponderScene -> ponderScene.runWith(link, extras -> extras.setCouplerVisible(visible)));
   }

   private static final class ShowInstruction extends FadeIntoSceneInstruction<CoasterCartExtrasElement> {
      ShowInstruction(int fadeInTicks, Direction fadeInFrom, CoasterCartExtrasElement element) {
         super(fadeInTicks, fadeInFrom, element);
      }

      protected Class<CoasterCartExtrasElement> getElementClass() {
         return CoasterCartExtrasElement.class;
      }
   }
}
