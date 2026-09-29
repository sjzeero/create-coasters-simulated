package dev.silvergold.simulatedcoasters.rivet;

public final class RivetAssemblyMoveContext {
   private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

   private RivetAssemblyMoveContext() {
   }

   public static boolean isActive() {
      return ACTIVE.get();
   }

   public static void begin() {
      ACTIVE.set(true);
   }

   public static void end() {
      ACTIVE.remove();
   }
}
