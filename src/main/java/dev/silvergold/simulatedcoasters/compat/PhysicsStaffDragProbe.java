package dev.silvergold.simulatedcoasters.compat;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

public final class PhysicsStaffDragProbe {
   private static final String HANDLER_CLASS = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler";
   private static final String SESSION_CLASS = "dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler$DragSession";
   private static final Object INIT_LOCK = new Object();
   private static Method getHandler;
   private static Method isLockedMethod;
   private static Field draggingSessionsField;
   private static Field sessionSubLevelField;
   private static boolean unavailable;

   private PhysicsStaffDragProbe() {
   }

   public static boolean isPhysicsStaffGrabbing(ServerLevel level, ServerSubLevel sub) {
      if (unavailable) {
         return false;
      } else {
         synchronized (INIT_LOCK) {
            if (!ensureInitialized()) {
               return false;
            }
         }

         try {
            Object handler = getHandler.invoke(null, level);
            Map<?, ?> sessions = (Map<?, ?>)draggingSessionsField.get(handler);
            UUID id = sub.getUniqueId();

            for (Object session : sessions.values()) {
               if (sessionSubLevelField.get(session) instanceof ServerSubLevel other && other.getUniqueId().equals(id)) {
                  return true;
               }
            }

            return false;
         } catch (ReflectiveOperationException var9) {
            return false;
         }
      }
   }

   public static boolean isPhysicsStaffLocked(ServerLevel level, ServerSubLevel sub) {
      if (unavailable) {
         return false;
      } else {
         synchronized (INIT_LOCK) {
            if (!ensureInitialized()) {
               return false;
            }
         }

         try {
            Object handler = getHandler.invoke(null, level);
            return (Boolean)isLockedMethod.invoke(handler, sub);
         } catch (ReflectiveOperationException var4) {
            return false;
         }
      }
   }

   private static boolean ensureInitialized() {
      if (getHandler != null) {
         return true;
      } else if (unavailable) {
         return false;
      } else {
         try {
            Class<?> handlerCl = Class.forName("dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler");
            getHandler = handlerCl.getMethod("get", ServerLevel.class);
            isLockedMethod = handlerCl.getMethod("isLocked", SubLevel.class);
            draggingSessionsField = handlerCl.getDeclaredField("draggingSessions");
            draggingSessionsField.setAccessible(true);
            Class<?> sessionCl = Class.forName("dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler$DragSession");
            sessionSubLevelField = sessionCl.getDeclaredField("subLevel");
            sessionSubLevelField.setAccessible(true);
            return true;
         } catch (ClassNotFoundException var2) {
            unavailable = true;
            return false;
         } catch (ReflectiveOperationException var3) {
            unavailable = true;
            return false;
         }
      }
   }
}
