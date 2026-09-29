/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllKeys
 *  com.simibubi.create.foundation.item.ItemDescription$Builder
 *  com.simibubi.create.foundation.item.TooltipModifier
 *  net.createmod.catnip.lang.FontHelper$Palette
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.world.item.Item
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.event.entity.player.ItemTooltipEvent
 */
package dev.silvergold.simulatedcoasters.client.item;

import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@OnlyIn(value=Dist.CLIENT)
public final class RivetItemTooltips {
    private RivetItemTooltips() {
    }

    public static void register() {
        TooltipModifier.REGISTRY.register((Object)((Item)SimulatedCoasters.RIVET.get()), RivetItemTooltips::modify);
    }

    private static void modify(ItemTooltipEvent event) {
        String alt = AllKeys.ALT_MODIFIER.getBoundKey();
        String ctrl = AllKeys.CTRL_MODIFIER.getBoundKey();
        ItemDescription.Builder builder = new ItemDescription.Builder(FontHelper.Palette.STANDARD_CREATE);
        builder.addSummary(I18n.get((String)"item.simulatedcoasters.rivet.tooltip.summary", (Object[])new Object[0]));
        builder.addBehaviour(I18n.get((String)"item.simulatedcoasters.rivet.tooltip.condition1", (Object[])new Object[]{alt}), I18n.get((String)"item.simulatedcoasters.rivet.tooltip.behaviour1", (Object[])new Object[0]));
        builder.addBehaviour(I18n.get((String)"item.simulatedcoasters.rivet.tooltip.condition2", (Object[])new Object[]{alt}), I18n.get((String)"item.simulatedcoasters.rivet.tooltip.behaviour2", (Object[])new Object[0]));
        builder.addBehaviour(I18n.get((String)"item.simulatedcoasters.rivet.tooltip.condition3", (Object[])new Object[]{ctrl}), I18n.get((String)"item.simulatedcoasters.rivet.tooltip.behaviour3", (Object[])new Object[0]));
        event.getToolTip().addAll(1, builder.build().getCurrentLines());
    }
}

