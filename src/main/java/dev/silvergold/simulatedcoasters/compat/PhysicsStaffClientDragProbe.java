package dev.silvergold.simulatedcoasters.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3dc;

@OnlyIn(Dist.CLIENT)
public final class PhysicsStaffClientDragProbe {
   private static final String SIMULATED_CLIENT = "dev.simulated_team.simulated.SimulatedClient";
   private static final Object INIT_LOCK = new Object();
   private static Object cachedHandler;
   private static Method getDragSession;
   private static Method dragSubLevelGetter;
   private static Method isLockedMethod;
   private static Field serverDragSessionsField;
   private static boolean unavailable;

   private PhysicsStaffClientDragProbe() {
   }

   public static boolean isAnyPhysicsStaffGrabbing(Level level, SubLevel sub) {
      if (unavailable) {
         return false;
      } else {
         synchronized (INIT_LOCK) {
            if (!ensureInitialized()) {
               return false;
            }
         }

         UUID id = sub.getUniqueId();

         try {
            Object session = getDragSession.invoke(cachedHandler);
            if (session != null && dragSubLevelGetter.invoke(session) instanceof SubLevel sl && sl.getUniqueId().equals(id)) {
               return true;
            } else {
               Map<?, ?> byDimension = (Map<?, ?>)serverDragSessionsField.get(cachedHandler);
               if (byDimension == null) {
                  return false;
               } else if (byDimension.get(level.dimension()) instanceof Map<?, ?> sessions) {
                  for (Object o : sessions.entrySet()) {
                     Entry<?, ?> e = (Entry<?, ?>)o;
                     if (e.getValue() instanceof Vector3dc anchor) {
                        SubLevel sl = Sable.HELPER.getContaining(level, anchor);
                        if (sl != null && sl.getUniqueId().equals(id)) {
                           return true;
                        }
                     }
                  }

                  return false;
               } else {
                  return false;
               }
            }
         } catch (ReflectiveOperationException var12) {
            return false;
         }
      }
   }

   public static boolean isPhysicsStaffLocked(Level level, SubLevel sub) {
      if (unavailable) {
         return false;
      } else {
         synchronized (INIT_LOCK) {
            if (!ensureInitialized()) {
               return false;
            }
         }

         try {
            return (Boolean)isLockedMethod.invoke(cachedHandler, sub);
         } catch (ReflectiveOperationException var4) {
            return false;
         }
      }
   }

   private static boolean ensureInitialized() {
      if (cachedHandler != null) {
         return true;
      } else if (unavailable) {
         return false;
      } else {
         try {
            Class<?> simClient = Class.forName("dev.simulated_team.simulated.SimulatedClient");
            cachedHandler = simClient.getField("PHYSICS_STAFF_CLIENT_HANDLER").get(null);
            Class<?> handlerCl = cachedHandler.getClass();
            getDragSession = handlerCl.getMethod("getDragSession");
            isLockedMethod = handlerCl.getDeclaredMethod("isLocked", SubLevel.class);
            isLockedMethod.setAccessible(true);
            Class<?> sessionCl = Class.forName("dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler$ClientDragSession");
            dragSubLevelGetter = sessionCl.getMethod("dragSubLevel");
            serverDragSessionsField = handlerCl.getDeclaredField("serverDragSessions");
            serverDragSessionsField.setAccessible(true);
            return true;
         } catch (ClassNotFoundException var3) {
            unavailable = true;
            return false;
         } catch (ReflectiveOperationException var4) {
            unavailable = true;
            return false;
         }
      }
   }
}
