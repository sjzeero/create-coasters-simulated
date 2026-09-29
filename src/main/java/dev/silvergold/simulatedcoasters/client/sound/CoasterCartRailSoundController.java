package dev.silvergold.simulatedcoasters.client.sound;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartRailSoundController {
   private static final Map<UUID, CoasterCartRailSoundInstance> ACTIVE = new HashMap<>();

   private CoasterCartRailSoundController() {
   }

   public static void clientTick(ClientLevel level) {
      CoasterCartRailSoundCache.beginClientTick(level);
      if (SubLevelContainer.getContainer(level) == null) {
         stopAll();
      } else {
         SoundManager sm = Minecraft.getInstance().getSoundManager();
         Set<UUID> cartIds = CoasterCartRailSoundCache.bearingPlotByCart().keySet();

         for (UUID leaderId : CoasterCartRailSoundCache.soundLeaderIds()) {
            BlockPos bp = CoasterCartRailSoundCache.bearingPlotByCart().get(leaderId);
            if (bp != null) {
               SubLevel sub = CoasterCartRailSoundCache.cartSubLevel(leaderId);
               if (sub != null && !sub.isRemoved() && !ACTIVE.containsKey(leaderId)) {
                  CoasterCartRailSoundInstance snd = new CoasterCartRailSoundInstance(sub, bp);
                  sm.play(snd);
                  ACTIVE.put(leaderId, snd);
               }
            }
         }

         for (UUID id : new HashSet<>(ACTIVE.keySet())) {
            if (!cartIds.contains(id) || !CoasterCartRailSoundCache.isSoundLeader(id)) {
               CoasterCartRailSoundInstance rm = ACTIVE.remove(id);
               if (rm != null) {
                  sm.stop(rm);
               }
            }
         }
      }
   }

   private static void stopAll() {
      SoundManager sm = Minecraft.getInstance().getSoundManager();

      for (CoasterCartRailSoundInstance snd : ACTIVE.values()) {
         sm.stop(snd);
      }

      ACTIVE.clear();
   }
}
