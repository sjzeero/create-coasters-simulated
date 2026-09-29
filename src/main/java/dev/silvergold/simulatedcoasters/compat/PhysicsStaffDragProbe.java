/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.server.level.ServerLevel
 */
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isPhysicsStaffGrabbing(ServerLevel level, ServerSubLevel sub) {
        if (unavailable) {
            return false;
        }
        Object object = INIT_LOCK;
        synchronized (object) {
            if (!PhysicsStaffDragProbe.ensureInitialized()) {
                return false;
            }
        }
        try {
            Object handler = getHandler.invoke(null, level);
            Map sessions = (Map)draggingSessionsField.get(handler);
            UUID id = sub.getUniqueId();
            for (Object session : sessions.values()) {
                ServerSubLevel other;
                Object sl = sessionSubLevelField.get(session);
                if (!(sl instanceof ServerSubLevel) || !(other = (ServerSubLevel)sl).getUniqueId().equals(id)) continue;
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
    public static boolean isPhysicsStaffLocked(ServerLevel level, ServerSubLevel sub) {
        if (unavailable) {
            return false;
        }
        Object object = INIT_LOCK;
        synchronized (object) {
            if (!PhysicsStaffDragProbe.ensureInitialized()) {
                return false;
            }
        }
        try {
            Object handler = getHandler.invoke(null, level);
            return (Boolean)isLockedMethod.invoke(handler, sub);
        }
        catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean ensureInitialized() {
        if (getHandler != null) {
            return true;
        }
        if (unavailable) {
            return false;
        }
        try {
            Class<?> handlerCl = Class.forName(HANDLER_CLASS);
            getHandler = handlerCl.getMethod("get", ServerLevel.class);
            isLockedMethod = handlerCl.getMethod("isLocked", SubLevel.class);
            draggingSessionsField = handlerCl.getDeclaredField("draggingSessions");
            draggingSessionsField.setAccessible(true);
            Class<?> sessionCl = Class.forName(SESSION_CLASS);
            sessionSubLevelField = sessionCl.getDeclaredField("subLevel");
            sessionSubLevelField.setAccessible(true);
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

