package dev.silvergold.simulatedcoasters;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SimulatedCoastersSoundEvents {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, "simulatedcoasters");
   public static final DeferredHolder<SoundEvent, SoundEvent> RIVET_ROTATE = SOUND_EVENTS.register(
      "rivet.rotate", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "rivet.rotate"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> RIVET_PLACE = SOUND_EVENTS.register(
      "rivet.place", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "rivet.place"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> BALLOON_POP = SOUND_EVENTS.register(
      "balloon.pop", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "balloon.pop"))
   );

   private SimulatedCoastersSoundEvents() {
   }

   public static void register(IEventBus modEventBus) {
      SOUND_EVENTS.register(modEventBus);
   }
}
