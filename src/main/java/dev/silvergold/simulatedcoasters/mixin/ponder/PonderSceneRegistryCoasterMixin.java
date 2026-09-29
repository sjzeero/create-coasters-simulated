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
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PonderSceneRegistry.class})
public class PonderSceneRegistryCoasterMixin {
   @Inject(
      method = {"compile(Ljava/util/Collection;)Ljava/util/List;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/createmod/ponder/api/level/PonderLevel;createBackup()V",
         shift = Shift.BEFORE
      )}
   )
   private static void simulatedcoasters$seedCoasterAnchorEntitiesBeforeBackup(
      CallbackInfoReturnable<List<PonderScene>> cir, @Local StructureTemplate template, @Local PonderLevel ponderLevel
   ) {
      CoasterPonderRestore.seedFromStructureTemplate(ponderLevel, template);
   }
}
