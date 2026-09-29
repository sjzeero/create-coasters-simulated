/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package dev.silvergold.simulatedcoasters;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SimulatedCoastersSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create((ResourceKey)Registries.SOUND_EVENT, (String)"simulatedcoasters");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIVET_ROTATE = SOUND_EVENTS.register("rivet.rotate", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"rivet.rotate")));
    public static final DeferredHolder<SoundEvent, SoundEvent> RIVET_PLACE = SOUND_EVENTS.register("rivet.place", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"rivet.place")));
    public static final DeferredHolder<SoundEvent, SoundEvent> BALLOON_POP = SOUND_EVENTS.register("balloon.pop", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"balloon.pop")));

    private SimulatedCoastersSoundEvents() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}

