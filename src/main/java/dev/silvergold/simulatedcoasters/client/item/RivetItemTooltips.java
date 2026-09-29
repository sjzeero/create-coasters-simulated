package dev.silvergold.simulatedcoasters.client.item;

import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.ItemDescription.Builder;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@OnlyIn(Dist.CLIENT)
public final class RivetItemTooltips {
   private RivetItemTooltips() {
   }

   public static void register() {
      TooltipModifier.REGISTRY.register((Item)SimulatedCoasters.RIVET.get(), RivetItemTooltips::modify);
   }

   private static void modify(ItemTooltipEvent event) {
      String alt = AllKeys.ALT_MODIFIER.getBoundKey();
      String ctrl = AllKeys.CTRL_MODIFIER.getBoundKey();
      Builder builder = new Builder(Palette.STANDARD_CREATE);
      builder.addSummary(I18n.get("item.simulatedcoasters.rivet.tooltip.summary", new Object[0]));
      builder.addBehaviour(
         I18n.get("item.simulatedcoasters.rivet.tooltip.condition1", new Object[]{alt}),
         I18n.get("item.simulatedcoasters.rivet.tooltip.behaviour1", new Object[0])
      );
      builder.addBehaviour(
         I18n.get("item.simulatedcoasters.rivet.tooltip.condition2", new Object[]{alt}),
         I18n.get("item.simulatedcoasters.rivet.tooltip.behaviour2", new Object[0])
      );
      builder.addBehaviour(
         I18n.get("item.simulatedcoasters.rivet.tooltip.condition3", new Object[]{ctrl}),
         I18n.get("item.simulatedcoasters.rivet.tooltip.behaviour3", new Object[0])
      );
      event.getToolTip().addAll(1, builder.build().getCurrentLines());
   }
}
