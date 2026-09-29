package dev.silvergold.simulatedcoasters;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SimulatedCoastersParticles {
   public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, "simulatedcoasters");
   public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TRACK_SNAP_SPARK = PARTICLE_TYPES.register(
      "track_snap_spark", () -> new SimpleParticleType(false)
   );

   private SimulatedCoastersParticles() {
   }

   public static void register(IEventBus modEventBus) {
      PARTICLE_TYPES.register(modEventBus);
   }
}
