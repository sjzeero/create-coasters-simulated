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
import dev.silvergold.simulatedcoasters.ponder.elements.ChainLiftExtrasElement;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.foundation.instruction.FadeIntoSceneInstruction;
import net.createmod.ponder.foundation.instruction.FadeOutOfSceneInstruction;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.Direction;

public final class ChainLiftPonderExtras {
    private static final int FADE_TICKS = 15;

    private ChainLiftPonderExtras() {
    }

    public static ElementLink<ChainLiftExtrasElement> show(CreateSceneBuilder scene, ChainLiftExtrasElement element, Direction fadeInFrom) {
        ShowInstruction instruction = new ShowInstruction(15, fadeInFrom, element);
        scene.addInstruction((PonderInstruction)instruction);
        return instruction.createLink(scene.getScene());
    }

    public static void hide(CreateSceneBuilder scene, ElementLink<ChainLiftExtrasElement> link, Direction fadeOutTo) {
        scene.addInstruction((PonderInstruction)new FadeOutOfSceneInstruction(15, fadeOutTo, link));
    }

    private static final class ShowInstruction
    extends FadeIntoSceneInstruction<ChainLiftExtrasElement> {
        ShowInstruction(int fadeInTicks, Direction fadeInFrom, ChainLiftExtrasElement element) {
            super(fadeInTicks, fadeInFrom, (AnimatedSceneElement)element);
        }

        protected Class<ChainLiftExtrasElement> getElementClass() {
            return ChainLiftExtrasElement.class;
        }
    }
}

