/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.foundation.item.ItemDescription$Modifier
 *  com.simibubi.create.foundation.item.TooltipModifier
 *  net.createmod.catnip.lang.FontHelper$Palette
 *  net.minecraft.world.item.Item
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.item;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class BalloonItemTooltips {
    private BalloonItemTooltips() {
    }

    public static void register() {
        TooltipModifier.REGISTRY.register((Object)((Item)SimulatedCoasters.RED_BALLOON.get()), (Object)new ItemDescription.Modifier((Item)SimulatedCoasters.RED_BALLOON.get(), FontHelper.Palette.STANDARD_CREATE));
    }
}

