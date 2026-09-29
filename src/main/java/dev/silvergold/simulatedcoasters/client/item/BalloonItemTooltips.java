package dev.silvergold.simulatedcoasters.client.item;

import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.ItemDescription.Modifier;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BalloonItemTooltips {
   private BalloonItemTooltips() {
   }

   public static void register() {
      TooltipModifier.REGISTRY
         .register((Item)SimulatedCoasters.RED_BALLOON.get(), new Modifier((Item)SimulatedCoasters.RED_BALLOON.get(), Palette.STANDARD_CREATE));
   }
}
