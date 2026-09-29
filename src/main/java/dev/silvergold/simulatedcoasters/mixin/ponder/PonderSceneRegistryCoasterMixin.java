/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.createmod.ponder.api.level.PonderLevel
 *  net.createmod.ponder.foundation.PonderScene
 *  net.createmod.ponder.foundation.registration.PonderSceneRegistry
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin.ponder;

import com.llamalad7.mixinextras.sugar.Local;
import dev.silvergold.simulatedcoasters.ponder.CoasterPonderRestore;
import java.util.List;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PonderSceneRegistry.class})
public class PonderSceneRegistryCoasterMixin {
    @Inject(method={"compile(Ljava/util/Collection;)Ljava/util/List;"}, at={@At(value="INVOKE", target="Lnet/createmod/ponder/api/level/PonderLevel;createBackup()V", shift=At.Shift.BEFORE)})
    private static void simulatedcoasters$seedCoasterAnchorEntitiesBeforeBackup(CallbackInfoReturnable<List<PonderScene>> cir, @Local StructureTemplate template, @Local PonderLevel ponderLevel) {
        CoasterPonderRestore.seedFromStructureTemplate(ponderLevel, template);
    }
}

