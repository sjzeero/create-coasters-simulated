/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.world.level.Level
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3dc;

@OnlyIn(value=Dist.CLIENT)
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isAnyPhysicsStaffGrabbing(Level level, SubLevel sub) {
        if (unavailable) {
            return false;
        }
        Object object = INIT_LOCK;
        synchronized (object) {
            if (!PhysicsStaffClientDragProbe.ensureInitialized()) {
                return false;
            }
        }
        UUID id = sub.getUniqueId();
        try {
            SubLevel sl;
            Object dragged;
            Object session = getDragSession.invoke(cachedHandler, new Object[0]);
            if (session != null && (dragged = dragSubLevelGetter.invoke(session, new Object[0])) instanceof SubLevel && (sl = (SubLevel)dragged).getUniqueId().equals(id)) {
                return true;
            }
            Map byDimension = (Map)serverDragSessionsField.get(cachedHandler);
            if (byDimension == null) {
                return false;
            }
            Object sessionsObj = byDimension.get(level.dimension());
            if (!(sessionsObj instanceof Map)) {
                return false;
            }
            Map sessions = (Map)sessionsObj;
            for (Map.Entry o : sessions.entrySet()) {
                Vector3dc anchor;
                SubLevel sl2;
                Map.Entry e = o;
                Object v = e.getValue();
                if (!(v instanceof Vector3dc) || (sl2 = Sable.HELPER.getContaining(level, anchor = (Vector3dc)v)) == null || !sl2.getUniqueId().equals(id)) continue;
                return true;
            }
        }
        catch (ReflectiveOperationException ignored) {
            return false;
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isPhysicsStaffLocked(Level level, SubLevel sub) {
        if (unavailable) {
            return false;
        }
        Object object = INIT_LOCK;
        synchronized (object) {
            if (!PhysicsStaffClientDragProbe.ensureInitialized()) {
                return false;
            }
        }
        try {
            return (Boolean)isLockedMethod.invoke(cachedHandler, sub);
        }
        catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean ensureInitialized() {
        if (cachedHandler != null) {
            return true;
        }
        if (unavailable) {
            return false;
        }
        try {
            Class<?> simClient = Class.forName(SIMULATED_CLIENT);
            cachedHandler = simClient.getField("PHYSICS_STAFF_CLIENT_HANDLER").get(null);
            Class<?> handlerCl = cachedHandler.getClass();
            getDragSession = handlerCl.getMethod("getDragSession", new Class[0]);
            isLockedMethod = handlerCl.getDeclaredMethod("isLocked", SubLevel.class);
            isLockedMethod.setAccessible(true);
            Class<?> sessionCl = Class.forName("dev.simulated_team.simulated.content.physics_staff.PhysicsStaffClientHandler$ClientDragSession");
            dragSubLevelGetter = sessionCl.getMethod("dragSubLevel", new Class[0]);
            serverDragSessionsField = handlerCl.getDeclaredField("serverDragSessions");
            serverDragSessionsField.setAccessible(true);
            return true;
        }
        catch (ClassNotFoundException e) {
            unavailable = true;
            return false;
        }
        catch (ReflectiveOperationException e) {
            unavailable = true;
            return false;
        }
    }
}

