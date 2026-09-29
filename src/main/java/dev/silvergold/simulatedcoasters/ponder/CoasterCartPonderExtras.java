/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.foundation.ponder.CreateSceneBuilder
 *  net.createmod.ponder.api.element.AnimatedSceneElement
 *  net.createmod.ponder.api.element.ElementLink
 *  net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction
 *  net.createmod.ponder.foundation.instruction.FadeOutOfSceneInstruction
 *  net.createmod.ponder.foundation.instruction.PonderInstruction
 *  net.minecraft.core.Direction
 */
package dev.silvergold.simulatedcoasters.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.silvergold.simulatedcoasters.ponder.elements.CoasterCartExtrasElement;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction;
import net.createmod.ponder.foundation.instruction.FadeOutOfSceneInstruction;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.Direction;

public final class CoasterCartPonderExtras {
    private static final int FADE_TICKS = 15;

    private CoasterCartPonderExtras() {
    }

    public static ElementLink<CoasterCartExtrasElement> show(CreateSceneBuilder scene, CoasterCartExtrasElement element, Direction fadeInFrom) {
        ShowInstruction instruction = new ShowInstruction(15, fadeInFrom, element);
        scene.addInstruction((PonderInstruction)instruction);
        return instruction.createLink(scene.getScene());
    }

    public static ElementLink<CoasterCartExtrasElement> showImmediately(CreateSceneBuilder scene, CoasterCartExtrasElement element) {
        ShowInstruction instruction = new ShowInstruction(0, Direction.DOWN, element);
        scene.addInstruction((PonderInstruction)instruction);
        return instruction.createLink(scene.getScene());
    }

    public static void hide(CreateSceneBuilder scene, ElementLink<CoasterCartExtrasElement> link, Direction fadeOutTo) {
        scene.addInstruction((PonderInstruction)new FadeOutOfSceneInstruction(15, fadeOutTo, link));
    }

    public static void hideImmediately(CreateSceneBuilder scene, ElementLink<CoasterCartExtrasElement> link) {
        scene.addInstruction((PonderInstruction)new FadeOutOfSceneInstruction(0, Direction.DOWN, link));
    }

    public static void setCouplerVisible(CreateSceneBuilder scene, ElementLink<CoasterCartExtrasElement> link, boolean visible) {
        scene.addInstruction(ponderScene -> ponderScene.runWith(link, extras -> extras.setCouplerVisible(visible)));
    }

    private static final class ShowInstruction
    extends FadeIntoSceneInstruction<CoasterCartExtrasElement> {
        ShowInstruction(int fadeInTicks, Direction fadeInFrom, CoasterCartExtrasElement element) {
            super(fadeInTicks, fadeInFrom, (AnimatedSceneElement)element);
        }

        protected Class<CoasterCartExtrasElement> getElementClass() {
            return CoasterCartExtrasElement.class;
        }
    }
}

