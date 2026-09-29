package dev.silvergold.simulatedcoasters.client.rivet;

import dev.silvergold.simulatedcoasters.rivet.RivetPlaceAnimPayload;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class RivetPlacementAnimationClient {
   public static final long DURATION_NANOS = 250000000L;
   public static final float PIVOT_X = 0.5F;
   public static final float PIVOT_Y = 0.9375F;
   public static final float PIVOT_Z = 0.5F;
   private static final Map<UUID, RivetPlacementAnimationClient.PendingAnim> PENDING_BY_RIVET_SUB = new HashMap<>();
   private static volatile boolean active;

   private RivetPlacementAnimationClient() {
   }

   public static boolean hasActive() {
      return active;
   }

   public static void begin(RivetPlaceAnimPayload payload) {
      PENDING_BY_RIVET_SUB.put(
         payload.rivetSubLevelId(),
         new RivetPlacementAnimationClient.PendingAnim(System.nanoTime(), RivetPlacementAnimationClient.SeatFxSnapshot.from(payload))
      );
      active = true;
   }

   public static float progress(UUID subLevelId) {
      RivetPlacementAnimationClient.PendingAnim pending = PENDING_BY_RIVET_SUB.get(subLevelId);
      if (pending == null) {
         return -1.0F;
      } else {
         float t = (float)(System.nanoTime() - pending.startNanos()) / 2.5E8F;
         return t >= 1.0F ? -1.0F : t;
      }
   }

   public static float easeIn(float t) {
      return t * t;
   }

   public static void clientTick(@Nullable ClientLevel level) {
      if (active) {
         long now = System.nanoTime();
         Iterator<Entry<UUID, RivetPlacementAnimationClient.PendingAnim>> it = PENDING_BY_RIVET_SUB.entrySet().iterator();

         while (it.hasNext()) {
            Entry<UUID, RivetPlacementAnimationClient.PendingAnim> entry = it.next();
            if (now - entry.getValue().startNanos() >= 250000000L) {
               RivetPlacementAnimationClient.PendingAnim pending = entry.getValue();
               it.remove();
               ClientLevel fxLevel = level;
               if (level == null) {
                  ClientLevel mcLevel = Minecraft.getInstance().level;
                  if (mcLevel instanceof ClientLevel) {
                     fxLevel = mcLevel;
                  }
               }

               if (fxLevel != null) {
                  RivetPlacementParticlesClient.onAnimationComplete(fxLevel, pending.seatFx());
               }
            }
         }

         active = !PENDING_BY_RIVET_SUB.isEmpty();
      }
   }

   public static void clearAll() {
      PENDING_BY_RIVET_SUB.clear();
      active = false;
   }

   private static record PendingAnim(long startNanos, RivetPlacementAnimationClient.SeatFxSnapshot seatFx) {
   }

   static record SeatFxSnapshot(Vec3 seatWorld, Vec3 faceOutwardWorld, BlockState hostBlockState) {
      static RivetPlacementAnimationClient.SeatFxSnapshot from(RivetPlaceAnimPayload payload) {
         return new RivetPlacementAnimationClient.SeatFxSnapshot(payload.seatWorld(), payload.faceOutwardWorld(), payload.hostBlockState());
      }
   }
}
